# Copyright 2016 Red Hat, Inc.
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
NAME=mercator
DESTDIR=/usr
HANDLERSDIR=${DESTDIR}/share/${NAME}
HANDLERS_TEMPLATE=handler_templates/handlers_template.yml
DOTNET=NO
RUST=NO
JAVA=YES

all:
	$(MAKE) clean
	$(MAKE) build
	$(MAKE) install

.PHONY: handlers clean build install fmt

handlers:
	cp $(HANDLERS_TEMPLATE) handlers.yml
	@if [ "$(JAVA)" == "YES" ]; then \
		pushd handlers/java_handler && $(MAKE) all; \
		popd; \
		cat handler_templates/handler_java >> handlers.yml; \
	fi
	@if [ "$(DOTNET)" == "YES" ]; then \
		pushd ./handlers/dotnet_handler && $(MAKE) all; \
		popd; \
		cat handler_templates/handler_dotnetsolution >> handlers.yml; \
	fi
	@if [ "$(RUST)" == "YES" ]; then \
		pushd ./handlers/rust_handler && $(MAKE) all; \
		popd; \
		cat handler_templates/handler_rustcrate >> handlers.yml; \
	fi

build: handlers
	go get 'gopkg.in/yaml.v2'
	rm -f ${GOPATH}/src/github.com/shaded-enmity/mercator
	mkdir -p ${GOPATH}/src/github.com/shaded-enmity/
	ln -s `pwd` ${GOPATH}/src/github.com/shaded-enmity/mercator
	go build -o ${NAME}
	rm -f ${GOPATH}/src/github.com/shaded-enmity/mercator

install:
	mkdir -p ${DESTDIR}/bin ${HANDLERSDIR}
	cp ${NAME} ${DESTDIR}/bin/${NAME}
	cp -rf handlers/* ${HANDLERSDIR}
	cp handlers.yml ${HANDLERSDIR}

clean:
	rm -rf ${HANDLERSDIR}
	rm -f ${DESTDIR}/bin/${NAME}
	rm -f ${GOPATH}/src/github.com/shaded-enmity/mercator

check:
	docker build -t mercator-tests -f Dockerfile.tests .
	docker run mercator-tests

fmt:
	for FL in `find . -name '*.go'`; do \
		gofmt $${FL} > $${FL}.2 ; \
		mv $${FL}.2 $${FL} ; \
	done
