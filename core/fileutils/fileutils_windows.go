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
package fileutils

import "syscall"

func IsHidden(path string) (bool, error) {
	p, e := syscall.UTF16PtrFromString(path)
	if e != nil {
		return false, e
	}
	attrs, e := syscall.GetFileAttributes(p)
	if e != nil {
		return false, e
	}
	return attrs&syscall.FILE_ATTRIBUTE_HIDDEN != 0, nil
}
