# -*- coding: utf-8 -*-

import os
import sys
from behave import *
import json
import subprocess
from results import *

SCANNED_JAR = 'fixtures/java-jar'
SCANNED_PACKAGE_JSON = 'fixtures/javascript'
SCANNED_SETUP_PY = 'fixtures/python'
SCANNED_RUBY = 'fixtures/ruby'
SCANNED_DOTNET = 'fixtures/dotnet'
SCANNED_RUST = 'fixtures/rust'
SCANNED_HASKELL = 'fixtures/haskell'

result_dict = {'javascript': javascript_result,
               'java': java_result,
               'ruby': ruby_result,
               'dotnet': dotnet_result,
               'python': python_result,
               'rustcargo': rust_result,
               'haskell': haskell_result
               }


def compare_dictionaries(a, b):
    def mapper(item):
        if isinstance(item, list):
            return frozenset(map(mapper, item))
        if isinstance(item, dict):
            return frozenset({mapper(k): mapper(v) for k, v in item.items()}.items())
        return item

    mapped_a = mapper(a)
    mapped_b = mapper(b)
    res = mapped_a == mapped_b

    if not res:
        print(json.dumps(a, sort_keys=True, separators=(',', ': '), indent=2), file=sys.stderr)
        print(json.dumps(b, sort_keys=True, separators=(',', ': '), indent=2), file=sys.stderr)

    return res


@given('We have mercator installed')
def step_impl(context):
    # this is workaround until mercator -h returns true
    subprocess.check_output(['ls', '/usr/bin/mercator'])


@when('Scanning the jar file')
def get_scan_info(context):
    context.out = subprocess.check_output(['mercator', SCANNED_JAR], env=dict(os.environ, MERCATOR_JAVA_RESOLVE_POMS="true")).decode('utf-8')
    context.ecosystem = 'java'


@when('Scanning the package.json file')
def get_scan_info(context):
    context.out = subprocess.check_output(['mercator', SCANNED_PACKAGE_JSON]).decode('utf-8')
    context.ecosystem = 'javascript'


@when('Scanning the setup.py file')
def get_scan_info(context):
    os.environ["MERCATOR_INTERPRET_SETUP_PY"] = "true"
    context.out = subprocess.check_output(['mercator', SCANNED_SETUP_PY]).decode('utf-8')
    context.ecosystem = 'python'


@when('Scanning the dll file')
def get_scan_info(context):
    context.out = subprocess.check_output(['mercator', SCANNED_DOTNET]).decode('utf-8')
    context.ecosystem = 'dotnet'


@when('Scanning the gemspec file')
def get_scan_info(context):
    context.out = subprocess.check_output(['mercator', SCANNED_RUBY]).decode('utf-8')
    context.ecosystem = 'ruby'


@when('Scanning the Cargo.toml file')
def get_scan_info(context):
    context.out = subprocess.check_output(['mercator', SCANNED_RUST]).decode('utf-8')
    context.ecosystem = 'rustcargo'


@when('Scanning the Cabal file')
def get_scan_info(context):
    context.out = subprocess.check_output(['mercator', SCANNED_HASKELL]).decode('utf-8')
    context.ecosystem = 'haskell'


@then('We have correct output')
def check_scan_info(context):
    test_case = json.loads(context.out)
    assert len(test_case["items"]) == 1
    tc = test_case["items"][0]
    del tc['time']
    if context.ecosystem == 'ruby':
        # date is updated each scan so I don't want to check it
        del tc['result']['date']
        assert 'rubygems_version' in tc['result']
        # do not check rubygems version is it can vary
        del tc['result']['rubygems_version']
    del tc['path']
    assert compare_dictionaries(tc, result_dict[context.ecosystem])
