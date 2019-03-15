#!/bin/bash

set -ex

. cico_setup.sh

build_test_rpm

run_tests

