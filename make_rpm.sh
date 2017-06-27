#!/bin/sh

RPMBUILD_ROOT=${RPMBUILD_ROOT:-$HOME/rpmbuild}
SOURCES=${SOURCES:-$RPMBUILD_ROOT/SOURCES}
SPECS=${SPECS:-$RPMBUILD_ROOT/SPECS}
SRPMS=${SRPMS:-$RPMBUILD_ROOT/SRPMS}

NAME=mercator

usage() {
    echo "Usage: $(basename "$0") --source|--binary"
    exit 1
}

if [ $# -ne 1 ] ; then
    usage
fi

mkdir -p "${SOURCES}" "${SPECS}/"
git archive --prefix="${NAME}/" -o "${SOURCES}/${NAME}.tar.gz" HEAD
cp -f $NAME.spec "${SPECS}/"

if [ "$1" == "--source" ] ; then
    rpmbuild -bs "${SPECS}/$NAME.spec"
elif [ "$1" == "--binary" ] ; then
    rpmbuild -bb "${SPECS}/$NAME.spec"
else
    usage
fi
