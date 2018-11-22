#!/usr/bin/env python3

# Copyright 2018 Red Hat, Inc.
#
# Mercator is free software: you can redistribute it and/or modify
# it under the terms of the GNU Lesser General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
# Mercator is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Lesser General Public License for more details.
# You should have received a copy of the GNU Lesser General Public License
# along with Mercator. If not, see <http://www.gnu.org/licenses/>.


# Mercator handler for Godeps.json manifest files.
# https://github.com/tools/godep

import sys
import json


if __name__ == '__main__':

    with open(sys.argv[1]) as f:
        # load and dump only
        print(json.dumps(json.load(f)))
