/*
 Copyright 2016 Red Hat, Inc.

 Mercator is free software: you can redistribute it and/or modify
 it under the terms of the GNU Lesser General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 Mercator is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Lesser General Public License for more details.
 You should have received a copy of the GNU Lesser General Public License
 along with Mercator. If not, see <http://www.gnu.org/licenses/>.
*/
package main

import (
	"archive/zip"
	"crypto/sha1"
	"encoding/hex"
	"encoding/json"
	"flag"
	"fmt"
	"io"
	"io/ioutil"
	"log"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"sync"
	"time"

	"github.com/fabric8-analytics/mercator-go/core"
	"github.com/fabric8-analytics/mercator-go/core/fileutils"
)

// Sync our Goroutines
var wg sync.WaitGroup

// Location of configuration file
var configYaml string

// Do not run handlers, only report what was found
var noHandlers bool

// Store names of MIME archive types
var archiveTypes map[string]bool

// JSONObject Generic JSON object representation for result data
type JSONObject map[string]interface{}

// Resolved Information about resolved path and its handler
type Resolved struct {
	Path     string
	Lockfile string
	Handler  *core.Handler
}

// Store digests of lockfiles and manifests
type DigestsRecord struct {
	Manifest string `json:"manifest,omitempty"`
	Lockfile string `json:"lockfile,omitempty"`
}

// ItemRecord Contains final information to be printed out
type ItemRecord struct {
	RecTime   time.Time     `json:"time"`
	Path      string        `json:"path"`
	Ecosystem string        `json:"ecosystem"`
	Result    JSONObject    `json:"result,omitempty"`
	Digests   DigestsRecord `json:"digests"`
}

// Our special snowflake
type JavaHandlerInvocation struct {
	Config   *core.Configuration
	Resolved *Resolved
	Record   *ItemRecord
}

// Versioned output
type Output struct {
	Versions map[string]string `json:"versions"`
	Items    []*ItemRecord     `json:"items"`
}

// Define command line flags
func init() {
	flag.StringVar(&configYaml, "config", "/usr/share/mercator/handlers.yml", "location of config file")
	flag.BoolVar(&noHandlers, "no-handlers", false, "only print found files and their ecosystem")
	archiveTypes = map[string]bool{
		"application/zip": true,
	}
}

// Compute SHA1 hash of the file at `path`
func computeSHA1(path *string) string {
	hasher := sha1.New()
	f, err := os.Open(*path)
	if err != nil {
		return ""
	}
	defer f.Close()
	if _, err := io.Copy(hasher, f); err != nil {
		return ""
	}
	return hex.EncodeToString(hasher.Sum(nil))
}

// Executes ecosystem specific handler in a Goroutine and stores the resulting
// JSON document in the `out` object
func runHandler(res *Resolved, config *core.Configuration, out *ItemRecord, async bool) error {
	var cmd *exec.Cmd
	if async {
		// Decrease the WaitGroups counter after executing
		defer wg.Done()
	}

	handlerPath := filepath.Join(config.Directory, res.Handler.Handler)
	if res.Handler.Binary != "" {
		// Interpreted binary handler
		//
		// $ {Handler.Binary} {Handler.Args} {Handler} {Path}
		args := append(res.Handler.Args, handlerPath, res.Path)
		if res.Lockfile != "" {
			args = append(args, res.Lockfile)
		}
		cmd = exec.Command(res.Handler.Binary, args...)
	} else {
		// Native binary handler
		//
		// $ {Handler} {Handler.Args} {Path}
		args := make([]string, len(res.Handler.Args))
		args = append(args, res.Handler.Args...)
		args = append(args, res.Path)
		if res.Lockfile != "" {
			args = append(args, res.Lockfile)
		}
		cmd = exec.Command(handlerPath, args...)
	}

	// Get the output from Stdout as well as process status
	output, err := cmd.Output()
	if err != nil {
		//log.Printf("%s => %s: %q ->  %q", res.Path, err.Error(), cmd.Args, string(output))
		return err
	}

	// Result is always a JSON we unmarshal it into `out.Result` directly
	if err = json.Unmarshal(output, &out.Result); err != nil {
		//log.Printf("%s: %s", err.Error(), string(output))
		return err
	}

	return nil
}

// Handle archive files and file inspection within archives
func handleFileTypes(path string, handler *core.Handler, resolved chan *Resolved) error {
	// buffer for MIME type detection
	buf := make([]byte, 512)
	f, err := os.Open(path)
	if err != nil {
		return err
	}
	defer f.Close()

	/* Handle the case when the input file is shorter than 512 bytes */
	if cnt, err := f.Read(buf); cnt == 0 && err == io.EOF {
		return nil
	} else if err != nil {
		return err
	}

	detected := http.DetectContentType(buf)
	for k := range handler.Types {
		typ := handler.Types[k]

		// first check for MIME type
		if typ == detected {
			// now check if we want to search inside the archive
			if archiveTypes[typ] && len(handler.InArchive) > 0 {
				for j := range handler.InArchive {
					// we support only zip for the time being
					if typ == "application/zip" {
						r, err := zip.OpenReader(path)
						if err != nil {
							return err
						}
						defer r.Close()

						for _, f := range r.File {
							// check and emit if we got a match
							if f.Name == handler.InArchive[j] {
								resolved <- &Resolved{Path: path, Handler: handler}
								return nil
							}
						}
					}
				}
			} else {
				// MIME type match without archive contents search, so emit
				resolved <- &Resolved{Path: path, Handler: handler}
				return nil
			}
		}

	}

	return nil
}

// Walk all files recursively in a target path returning a channel of `Resolved`
// objects with path -> handler mapping.
func walkFiles(root string, config *core.Configuration) chan *Resolved {
	ch := make(chan *Resolved)

	wg.Add(1)
	go func() {
		// Decrease counter and close the channel after executing
		defer wg.Done()
		defer close(ch)

		// Walk down the filesystem path
		filepath.Walk(root, func(path string, f os.FileInfo, err error) error {
			// https://golang.org/pkg/os/#FileMode.IsRegular
			// this should cover sockets, symlinks, dirs, etc.
			if err == nil && f.Mode().IsRegular() {
				// Get filename and absolute path
				base := filepath.Base(path)
				absolute, err := filepath.Abs(path)
				if err != nil {
					return err
				}

				// Skip hidden files
				if r, err := fileutils.IsHidden(absolute); err == nil {
					if r {
						return nil
					}
				} else {
					return err
				}

				// Check all ecosystem handlers
				for i := range config.Ecosystems {
					handler := config.Ecosystems[i]
					var resolved *Resolved

					for k := range handler.FilePatterns {
						pattern := handler.FilePatterns[k]

						if pattern.MatchString(base) {
							// Send resolved item to the channel and return
							resolved = &Resolved{Path: absolute, Handler: handler}
							break
						}
					}

					if resolved == nil {
						for k := range handler.PathPatterns {
							pattern := handler.PathPatterns[k]

							if pattern.MatchString(absolute) {
								// Send resolved item to the channel and return
								resolved = &Resolved{Path: absolute, Handler: handler}
								break
							}
						}
					}

					if resolved != nil {
						// if we found a manifest file we now try to search for a lockfile
						// in the same directory
						for k := range handler.LockfilePatterns {
							pattern := handler.LockfilePatterns[k]

							dir := filepath.Dir(absolute)
							stop := false
							files, _ := filepath.Glob(dir + "/*")

							for f := range files {
								if pattern.MatchString(files[f]) {
									resolved.Lockfile = files[f]
									stop = true
									break
								}
							}

							if stop {
								break
							}
						}

						ch <- resolved
					}

					if err = handleFileTypes(absolute, handler, ch); err != nil {
						return err
					}
				}
			}

			return nil
		})
	}()

	return ch
}

// Invoke special handlers
func processJava(invocations []*JavaHandlerInvocation) {
	for _, inv := range invocations {
		runHandler(inv.Resolved, inv.Config, inv.Record, false)
	}
}

func main() {
	log.SetFlags(0)
	flag.Parse()

	if flag.NArg() == 0 {
		log.Fatalln("error: please provide a target path argument")
	}

	scan_path := flag.Args()[0]
	if bytes, err := ioutil.ReadFile(configYaml); err != nil {
		log.Fatalln(err)
	} else {
		// Load configuration from file
		if config, err := core.LoadConfig(string(bytes)); err != nil {
			log.Fatalln(err)
		} else {
			ch := walkFiles(scan_path, config)
			var resolved []*ItemRecord
			var java_invocations []*JavaHandlerInvocation
			versions := make(map[string]string)

			// Receive resolved items from the channel and fill out
			// the output ItemRecord objects with detected information
			// based on the command-line flags options
			for elem := range ch {
				digest_manifest := computeSHA1(&elem.Path)
				digest_lockfile := computeSHA1(&elem.Lockfile)
				digests := DigestsRecord{Manifest: digest_manifest, Lockfile: digest_lockfile}

				if noHandlers {
					// Fill in basic information
					resolved = append(resolved, &ItemRecord{Path: elem.Path, Ecosystem: elem.Handler.Name, Digests: digests})
				} else {
					r := ItemRecord{RecTime: time.Now(), Path: elem.Path, Ecosystem: elem.Handler.Name, Digests: digests}
					resolved = append(resolved, &r)
					if _, ok := versions[elem.Handler.Name]; !ok {
						handlerPath := filepath.Join(config.Directory, elem.Handler.Handler)
						versions[elem.Handler.Name] = computeSHA1(&handlerPath)
					}
					// Run the ecosystem specific handler in a Goroutine, the result will be stored in
					// the ItemRecord directly
					//
					// ... unless the ecosystem is Java, which is a special snowflake ...
					if !strings.HasPrefix(elem.Handler.Name, "Java") {
						// We're about to execute a new Goroutine, so bump up the WaitGroup's counter
						wg.Add(1)
						go runHandler(elem, config, &r, true)
					} else {
						invocation := &JavaHandlerInvocation{Config: config, Record: &r, Resolved: elem}
						java_invocations = append(java_invocations, invocation)
					}
				}
			}

			if noHandlers {
				if data, err := json.MarshalIndent(resolved, "", "   "); err != nil {
					log.Fatalln(err)
				} else {
					fmt.Println(string(data))
				}
			} else {
				// Wait for all Goroutines to finish up processing
				wg.Wait()
				processJava(java_invocations)

				// TODO: Post-process resolved JSONs and validate schemas
				if data, err := json.MarshalIndent(Output{Versions: versions, Items: resolved}, "", "   "); err != nil {
					log.Fatalln(err)
				} else {
					fmt.Println(string(data))
				}
			}
		}
	}
}
