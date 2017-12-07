/*
 Copyright 2017 Red Hat, Inc.

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
	"encoding/json"
	"flag"
	"fmt"
	"io/ioutil"
	"log"
	"os"
	"strings"

	"github.com/Masterminds/glide/cfg"
	// nice tool to pprint data types for debugging: spew.Dump(var)
	// "github.com/davecgh/go-spew/spew"
)

// Generic string to any-type hashmap for exporting into json
type GenericMap map[string]interface{}

/*
Author (jpopelka) of the following filter*() functions is golang newbie,
but even he doesn't like them, because they are coupled with glide/cfg structures,
but he hasn't found any better way how to:
- read glide.[yaml|lock]
- merge into one structure
- serialize into json

He's already tried:
A)
  - type outputMap map[string]interface{}
	- yml := ioutil.ReadFile(args[0])
	- yaml.Unmarshal(yml, &outputMap)
	- json.Marshal(outputMap)
	json.Marshal(outputMap) fails because some outputMap values are of type map[interface {}]interface {}
B)
	- import "github.com/ghodss/yaml"
	- yml := ioutil.ReadFile(args[0])
	- j := yaml.YAMLToJSON(yml)
	- fmt.Println(string(j))
	Works, but I have no idea how to add another key (e.g. "dependency_lock_file")) into the j structure
C)
  - import "github.com/Masterminds/glide/cfg"
	- cfg.ConfigFromYaml() + cfg.LockfileFromYaml()
	- merge them into one structure
	- json.Marshal()
	Works, but the keys (in serialized json) have uppercase first letter.
	I'd need to add struct field tags (https://stackoverflow.com/a/11694255/2768738)
	to the glide/cfg structures definitions, but I can't modify them.

If you know more elegant way how to do that, let jpopelka know, he'll be glide (read: glad)
*/

// cfg.Owners to GenericMap converter
func filterOwners(owners cfg.Owners) []GenericMap {
	var filteredMap []GenericMap
	for _, o := range owners {
		owner := GenericMap{}
		if len(o.Name) > 0 {
			owner["name"] = o.Name
		}
		if len(o.Email) > 0 {
			owner["email"] = o.Email
		}
		if len(o.Home) > 0 {
			owner["homepage"] = o.Home
		}
		filteredMap = append(filteredMap, owner)
	}
	return filteredMap
}

// cfg.Dependencies to GenericMap converter
func filterDependencies(deps cfg.Dependencies) []GenericMap {
	var filteredMap []GenericMap
	for _, d := range deps {
		dep := GenericMap{}
		dep["package"] = d.Name
		if len(d.Reference) > 0 {
			dep["version"] = d.Reference
		}
		if len(d.Pin) > 0 {
			dep["-"] = d.Pin
		}
		if len(d.Repository) > 0 {
			dep["repo"] = d.Repository
		}
		if len(d.VcsType) > 0 {
			dep["vcs"] = d.VcsType
		}
		if len(d.Subpackages) > 0 {
			dep["subpackages"] = d.Subpackages
		}
		if len(d.Arch) > 0 {
			dep["arch"] = d.Arch
		}
		if len(d.Os) > 0 {
			dep["os"] = d.Os
		}
		filteredMap = append(filteredMap, dep)
	}
	return filteredMap
}

// cfg.Config to GenericMap converter, output arg is in/out
func filterGlideConfig(config cfg.Config, output GenericMap) {
	output["package"] = config.Name
	if len(config.License) > 0 {
		output["license"] = config.License
	}
	if len(config.Home) > 0 {
		output["homepage"] = config.Home
	}
	if len(config.Owners) > 0 {
		output["owners"] = filterOwners(config.Owners)
	}
	if len(config.Ignore) > 0 {
		output["ignore"] = config.Ignore
	}
	if len(config.Exclude) > 0 {
		output["excludeDirs"] = config.Exclude
	}
	if len(config.Imports) > 0 {
		output["import"] = filterDependencies(config.Imports)
	}
	if len(config.DevImports) > 0 {
		output["testImport"] = filterDependencies(config.DevImports)
	}
}

// cfg.Locks to GenericMap converter
func filterLocks(locks cfg.Locks) []GenericMap {
	var filteredMap []GenericMap
	for _, l := range locks {
		lock := GenericMap{}
		lock["name"] = l.Name
		if len(l.Version) > 0 {
			lock["version"] = l.Version
		}
		if len(l.Repository) > 0 {
			lock["repo"] = l.Repository
		}
		if len(l.VcsType) > 0 {
			lock["vcs"] = l.VcsType
		}
		if len(l.Subpackages) > 0 {
			lock["subpackages"] = l.Subpackages
		}
		if len(l.Arch) > 0 {
			lock["arch"] = l.Arch
		}
		if len(l.Os) > 0 {
			lock["os"] = l.Os
		}
		filteredMap = append(filteredMap, lock)
	}
	return filteredMap
}

// cfg.Lockfile to GenericMap converter
// adds the filtered lockfile into 'output' arg under separate key
func filterGlideLockfile(lockfile cfg.Lockfile, output GenericMap) {
	filteredMap := GenericMap{}
	filteredMap["hash"] = lockfile.Hash
	filteredMap["updated"] = lockfile.Updated
	if len(lockfile.Imports) > 0 {
		filteredMap["import"] = filterLocks(lockfile.Imports)
	}
	if len(lockfile.DevImports) > 0 {
		filteredMap["testImport"] = filterLocks(lockfile.DevImports)
	}
	output["_dependency_tree_lock_file"] = filteredMap
}

// read glide.yaml into 'output' map
func processGlideYaml(fpath string, output GenericMap) {
	if !strings.HasSuffix(fpath, "glide.yaml") {
		log.Fatalf("%s is not glide.yaml", fpath)
	}

	yml, err := ioutil.ReadFile(fpath)
	if err != nil {
		log.Fatalf("yamlFile.Get err: #%v ", err)
	}

	config, err := cfg.ConfigFromYaml(yml)
	filterGlideConfig(*config, output)
}

// read glide.lock into 'output' map
func processGlideLock(fpath string, output GenericMap) {
	if !strings.HasSuffix(fpath, "glide.lock") {
		log.Fatalf("%s is not glide.lock", fpath)
	}

	yml, err := ioutil.ReadFile(fpath)
	if err != nil {
		log.Fatalf("yamlFile.Get err: #%v ", err)
	}

	lockFile, err := cfg.LockfileFromYaml(yml)
	filterGlideLockfile(*lockFile, output)
}

func processFile(fpath string, output GenericMap) GenericMap {
	if strings.HasSuffix(fpath, "glide.yaml") {
		processGlideYaml(fpath, output)
	} else if strings.HasSuffix(fpath, "glide.lock") {
		processGlideLock(fpath, output)
	}
	return output
}

func jsonDump(output GenericMap) {
	j, err := json.Marshal(output)
	if err != nil {
		log.Fatalf("json.Marshal err: #%v", err)
	}
	fmt.Println(string(j))
}

func usage() {
	fmt.Fprintf(os.Stderr, "usage: mercator glide.yaml [glide.lock]\n")
	flag.PrintDefaults()
	os.Exit(2)
}

func main() {
	flag.Usage = usage
	flag.Parse()

	args := flag.Args()
	if len(args) < 1 {
		fmt.Println("Manifest file is missing.")
		os.Exit(1)
	}

	output := GenericMap{}
	for _, fpath := range args {
		processFile(fpath, output)
	}
	jsonDump(output)
}
