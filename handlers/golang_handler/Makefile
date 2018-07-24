# Copyright 2017 Red Hat, Inc.
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
#

GOPATH_SRC_MERCATOR=${GOPATH}/src/github.com/fabric8-analytics/mercator-go
GOPATH_SRC_HANDLERS=${GOPATH_SRC_MERCATOR}/handlers
GOPATH_SRC_HANDLERS_GOLANG=${GOPATH_SRC_HANDLERS}/golang_handler

.PHONY : clean

all:
	# for 'glide up' and 'go build' to work we need to be in $GOPATH_SRC_HANDLERS_GOLANG
	@if [ `pwd` != "${GOPATH_SRC_HANDLERS_GOLANG}" ]; then \
		mkdir -p ${GOPATH_SRC_HANDLERS}; \
		ln -f -s `pwd` ${GOPATH_SRC_HANDLERS_GOLANG}; \
	fi
	pushd ${GOPATH_SRC_HANDLERS_GOLANG}; \
		glide up; \
		go build -o ../goglide; \
	popd
	# if we did those hacks previously, we need to clean after ourselves,
	# becase the main makefile does similar hacks and it'd fail
	@if [ `pwd` != "${GOPATH_SRC_HANDLERS_GOLANG}" ]; then \
	        rm -rf ${GOPATH_SRC_MERCATOR}; \
	fi
