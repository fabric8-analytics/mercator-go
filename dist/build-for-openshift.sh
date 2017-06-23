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

    # build java handler now, because RPM build fails in copr for EPEL-6
    make build JAVA=YES DOTNET=YES RUST=NO
    cp mercator "${SOURCES_ROOT}/"
    cp handlers/java "${SOURCES_ROOT}/"
    cp handlers/dotnet "${SOURCES_ROOT}/"
    cp handlers.yml "${SOURCES_ROOT}/"

    git archive --prefix="mercator/" -o "mercator-go-${MERCATOR_VERSION}.tar.gz" HEAD
    cp "mercator-go-${MERCATOR_VERSION}.tar.gz" "${SOURCES_ROOT}/mercator-go.tar.gz"

    curl https://raw.githubusercontent.com/fabric8-analytics/fabric8-analytics-worker/master/f8a_worker/data_normalizer.py -o data_normalizer.py
    cp data_normalizer.py "${SOURCES_ROOT}"

    rpmbuild -bs dist/mercator.spec
)

