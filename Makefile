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
NAME=mercator-go
BIN_NAME=mercator
ORG=github.com/fabric8-analytics
GOPATH_SRC=${GOPATH}/src/${ORG}/${NAME}
RPM_BUILDROOT=
DESTDIR_DEFAULT=/usr/local
DESTDIR=${DESTDIR_DEFAULT}
EFFECTIVE_DESTDIR=${RPM_BUILDROOT}${DESTDIR}
HANDLERSDIR_DEFAULT=${DESTDIR_DEFAULT}/share/${BIN_NAME}
HANDLERSDIR=${EFFECTIVE_DESTDIR}/share/${BIN_NAME}
HANDLERS_CONFIG_DEFAULT=${DESTDIR_DEFAULT}/share/mercator/handlers.yml
HANDLERS_CONFIG=${EFFECTIVE_DESTDIR}/share/mercator/handlers.yml
HANDLERS_TEMPLATE=handler_templates/handlers_template.yml
RUBY=YES
NPM=YES
PYTHON=YES
JAVA=YES
GRADLE=NO
DOTNET=NO
RUST=NO
HASKELL=NO
GOLANG=YES

all:
	$(MAKE) clean
	$(MAKE) build
	$(MAKE) install

.PHONY: handlers clean build install fmt

handlers:
	cp $(HANDLERS_TEMPLATE) handlers.yml
	sed -i -e "s~${HANDLERSDIR_DEFAULT}~${HANDLERSDIR}~" handlers.yml
	@if [ "$(PYTHON)" == "YES" ]; then \
		pushd ./handlers/python_handler && $(MAKE) all; \
		popd; \
		cat handler_templates/handler_python >> handlers.yml; \
	fi
	@if [ "$(RUBY)" == "YES" ]; then \
		pushd ./handlers/ruby_handler && $(MAKE) all; \
		popd; \
		cat handler_templates/handler_ruby >> handlers.yml; \
	fi
	@if [ "$(NPM)" == "YES" ]; then \
		pushd ./handlers/npm_handler && $(MAKE) all; \
		popd; \
		cat handler_templates/handler_npm >> handlers.yml; \
	fi
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
	@if [ "$(HASKELL)" == "YES" ]; then \
		pushd ./handlers/haskell_handler && $(MAKE) all; \
		popd; \
		cat handler_templates/handler_haskellcabal >> handlers.yml; \
	fi
	@if [ "$(GOLANG)" == "YES" ]; then \
		pushd ./handlers/golang_handler && $(MAKE) all; \
		popd; \
		cat handler_templates/handler_golang >> handlers.yml; \
	fi
	@if [ "$(GRADLE)" == "YES" ]; then \
		pushd ./handlers/gradle_handler && $(MAKE) all; \
		popd; \
		cat handler_templates/handler_gradle >> handlers.yml; \
	fi

build: handlers
	go get 'gopkg.in/yaml.v2'
	sed -i -e "s~${HANDLERS_CONFIG_DEFAULT}~${HANDLERS_CONFIG}~" main.go
	# for 'go build' to work properly, we need to be in GOPATH_SRC
	@if [ `pwd` != "${GOPATH_SRC}" ]; then \
		mkdir -p ${GOPATH}/src/${ORG}/; \
		ln -f -s `pwd` ${GOPATH_SRC}; \
	fi
	pushd ${GOPATH_SRC}; \
		go build -o ${BIN_NAME}; \
	popd
	@if [ `pwd` != "${GOPATH_SRC}" ]; then \
		rm -f ${GOPATH_SRC}; \
	fi


install:
	mkdir -p ${EFFECTIVE_DESTDIR}/bin ${HANDLERSDIR}
	cp ${BIN_NAME} ${EFFECTIVE_DESTDIR}/bin/${BIN_NAME}
	cp handlers.yml ${HANDLERSDIR}
	cp -rf handlers/* ${HANDLERSDIR} || :
	# bundled python pkginfo module
	cp -rf handlers/python_handler/pkginfo/ ${HANDLERSDIR}
	# bundle gradle dependencies
	@if [ -d handlers/gradle_handler/node_modules ]; then \
		cp -r handlers/gradle_handler/node_modules ${HANDLERSDIR}; \
	fi
clean:
	rm -rf ${HANDLERSDIR}
	rm -f ${EFFECTIVE_DESTDIR}/bin/${BIN_NAME}
	@if [ `pwd` != "${GOPATH_SRC}" ]; then \
		rm -f ${GOPATH_SRC}; \
	fi

check:
	docker build -t mercator-tests -f Dockerfile.tests .
	docker run mercator-tests

fmt:
	for FL in `find . -name '*.go'`; do \
		gofmt $${FL} > $${FL}.2 ; \
		mv $${FL}.2 $${FL} ; \
	done
