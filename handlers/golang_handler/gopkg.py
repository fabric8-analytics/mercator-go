#!/usr/bin/env python3

# Mercator handler for Godep manifest file and lock file.
# https://github.com/golang/dep/blob/master/docs/Gopkg.toml.md
# https://github.com/toml-lang/toml

import sys
import toml
import json


def process_toml(data):
    """Process Gopkg.toml files."""

    return {
        'constraint': data.get('constraint', []),
        'override': data.get('override', [])
    }


def process_lock(data):
    """Process Gopkg.toml files."""

    def unpack_project(project_dict):
        """Unpack project definitions."""
        project_packages = project_dict.get('packages', [])
        unpacked = []
        for package in project_packages:
            record = {}
            if package == '.':
                record['name'] = project_dict.get('name')
            else:
                record['name'] = '{base}/{pkg}'.format(
                    base=project_dict.get('name'),
                    pkg=package
                )
            record['revision'] = project_dict.get('revision')
            if 'version' in project_dict:
                record['version'] = project_dict.get('version')
            unpacked.append(record)

        return unpacked

    packages = []

    for project in data.get('projects', []):
        packages.extend(unpack_project(project))

    return {'packages': packages}


if __name__ == '__main__':

    with open(sys.argv[1]) as f:
        manifest = toml.load(f)
        result = process_toml(manifest)

    if len(sys.argv) > 2:
        with open(sys.argv[2]) as f:
            manifest = toml.load(f)
            result['_dependency_tree_lock_file'] = process_lock(manifest)

    print(json.dumps(result))
