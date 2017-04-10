#!/usr/bin/bash

set -e -x

RPMBUILD_ROOT=${RPMBUILD_ROOT:-$HOME/rpmbuild}
SOURCES_ROOT=${SOURCES_ROOT:-$RPMBUILD_ROOT/SOURCES}
MERCATOR_VERSION=${MERCATOR_VERSION:-1.0.0}
 
rm -rf "${RPMBUILD_ROOT}"/*
[ -d "${SOURCES_ROOT}" ] || mkdir -p "${SOURCES_ROOT}"

# Work in the root of GIT
(
	cd ..

	git archive --prefix="mercator/" -o "mercator-go-${MERCATOR_VERSION}.tar.gz" HEAD
	# TODO: Pinned specific version because github repo is private
	curl https://raw.githubusercontent.com/baytemp/worker/master/cucoslib/data_normalizer.py?token=APPXcH3SGOqOQje3nRuy8fHyNB0AaoNXks5Y9HVdwA%3D%3D -o data_normalizer.py
	# we need to build java handler as RPM build fails in copr for EPEL-6
        make -C handlers/java_handler
	

	cp "mercator-go-${MERCATOR_VERSION}.tar.gz" "${SOURCES_ROOT}/mercator-go.tar.gz"
	cp mercator "${SOURCES_ROOT}/"
	cp handlers/java "${SOURCES_ROOT}/"
	cp dist/handlers_template.yml "${SOURCES_ROOT}/"

	cp data_normalizer.py "${SOURCES_ROOT}"
	rpmbuild -bs dist/mercator-go-for-openshift.spec
)

