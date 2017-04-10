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

package core

import "gopkg.in/yaml.v2"
import "regexp"

// DefinedRuleSet Rules as read from the YAML
type DefinedRuleSet []string

// RuleSet Rules represented by Regexp
type RuleSet []*regexp.Regexp

// FileTypes Trigger on filetypes and if archive contains certain file
type FileTypes []string

// ArchiveContains Defines which files should be found in the archive
type ArchiveContains []string

// Args Defines additional arguments for the executed binary
type Args []string

// DefinedHandler Handler description as defined by the YAML
type DefinedHandler struct {
	Name             string
	Description      string
	Binary           string
	Args             Args
	Handler          string
	Types            FileTypes
	InArchive        ArchiveContains
	FilePatterns     DefinedRuleSet
	PathPatterns     DefinedRuleSet
	Lockfile         DefinedRuleSet
}

// DefinedConfiguration Configuration description as defined by the YAML
type DefinedConfiguration struct {
	Version    string
	Directory  string
	Ecosystems []DefinedHandler
}

// Handler with file/path rules loaded
type Handler struct {
	Name             string
	Description      string
	Binary           string
	Args             Args
	Handler          string
	Types            FileTypes
	InArchive        ArchiveContains
	FilePatterns     RuleSet
	PathPatterns     RuleSet
	LockfilePatterns RuleSet
}

// Configuration Contains runtime configuration
type Configuration struct {
	Version    string
	Directory  string
	Ecosystems []*Handler
}

// Convert a DefinedHandler to Handler by compiling all the necessary Regexps
func (df *DefinedHandler) toHandler() (*Handler, error) {
	var err error

	// Create file name patterns
	filePatterns := make(RuleSet, len(df.FilePatterns))
	for i := range df.FilePatterns {
		if filePatterns[i], err = regexp.Compile(df.FilePatterns[i]); err != nil {
			return nil, err
		}
	}

	// Create absolute path patterns
	pathPatterns := make(RuleSet, len(df.PathPatterns))
	for i := range df.PathPatterns {
		if pathPatterns[i], err = regexp.Compile(df.PathPatterns[i]); err != nil {
			return nil, err
		}
	}

	// Create lock file patterns
	lockPatterns := make(RuleSet, len(df.Lockfile))
	for i := range df.Lockfile {
		if lockPatterns[i], err = regexp.Compile(df.Lockfile[i]); err != nil {
			return nil, err
		}
	}

	return &Handler{
		Name:             df.Name,
		Description:      df.Description,
		Binary:           df.Binary,
		Args:             df.Args,
		Handler:          df.Handler,
		Types:            df.Types,
		InArchive:        df.InArchive,
		FilePatterns:     filePatterns,
		PathPatterns:     pathPatterns,
		LockfilePatterns: lockPatterns,
	}, nil
}

// LoadConfig Load runtime configuration from contents of input (YAML) data file
func LoadConfig(data string) (*Configuration, error) {
	config := DefinedConfiguration{}

	err := yaml.Unmarshal([]byte(data), &config)
	if err != nil {
		return nil, err
	}

	// Create Handlers from DefinedHandlers
	ecosystems := make([]*Handler, len(config.Ecosystems))
	for i := range config.Ecosystems {
		if ecosystems[i], err = config.Ecosystems[i].toHandler(); err != nil {
			return nil, err
		}
	}

	return &Configuration{
		Version:    config.Version,
		Directory:  config.Directory,
		Ecosystems: ecosystems,
	}, nil
}
