Mercator 2.0
============

This is a 2.0 version of Mercator tool, rewritten from Python to Go

## Installation & Running

Make sure that your `GOPATH` is set, then the following packages are necessary to fully build without handlers:

```
cmake openssl-devel git golang make
```

Per handler dependencies:  

Ruby:  

```
ruby
```

Java:  

```
java-devel maven
```

Python:  

```
python3
```

Javascript:  

```
npm
```

Dotnet:  

```
mono-devel nuget
```

for Dotnet you have to execute this command first:

```
yes | certmgr -ssl -m https://go.microsoft.com && yes | certmgr -ss    l -m https://nuget.org
```

All handler dependencies together:  
```
ruby java-devel python3 npm mono-devel nuget
```

Note: the `which` package is required because of [bug 1396395](https://bugzilla.redhat.com/show_bug.cgi?id=1396395); when this is fixed, it can be safely removed.

If you have all the packages installed, simply invoke `make`:

```
make build
sudo make install
```

You can enable handlers which require advanced building (this is example for DOTNET):
```
make build DOTNET=YES
```
There are three such handlers: DOTNET, RUST and JAVA. JAVA is enabled by default, rest is for disabled.


After that, `Mercator` is ready to be used:

```
$ mercator jsl/
[
   {
      "path": "/home/podvody/Repos/jsl/setup.py",
      "ecosystem": "Python",
      "result": {
         "author": "Anton Romanovich",
         "author_email": "anthony.romanovich@gmail.com",
         "classifiers": [
            "Development Status :: 4 - Beta",
            "Intended Audience :: Developers",
            "License :: OSI Approved :: BSD License",
            "Operating System :: OS Independent",
            "Programming Language :: Python",
            "Programming Language :: Python :: 2",
            "Programming Language :: Python :: 2.6",
            "Programming Language :: Python :: 2.7",
            "Programming Language :: Python :: 3",
            "Programming Language :: Python :: 3.3",
            "Programming Language :: Python :: 3.4",
            "Programming Language :: Python :: Implementation :: CPython",
            "Programming Language :: Python :: Implementation :: PyPy",
            "Topic :: Software Development :: Libraries :: Python Modules"
         ],
         "description": "A Python DSL for defining JSON schemas",
         "ext_modules": [],
         "license": "BSD",
         "long_description": "JSL\n===\n\n.. image:: https://travis-ci.org/aromanovich/jsl.svg?branch=master\n    :target: https://travis-ci.org/aromanovich/jsl\n    :alt: Build Status\n\n.. image:: https://coveralls.io/repos/aromanovich/jsl/badge.svg?branch=master\n    :target: https://coveralls.io/r/aromanovich/jsl?branch=master\n    :alt: Coverage\n\n.. image:: https://readthedocs.org/projects/jsl/badge/?version=latest\n    :target: https://readthedocs.org/projects/jsl/\n    :alt: Documentation\n\n.. image:: http://img.shields.io/pypi/v/jsl.svg\n    :target: https://pypi.python.org/pypi/jsl\n    :alt: PyPI Version\n\n.. image:: http://img.shields.io/pypi/dm/jsl.svg\n    :target: https://pypi.python.org/pypi/jsl\n    :alt: PyPI Downloads\n\nDocumentation_ | GitHub_ |  PyPI_\n\nJSL is a Python DSL for defining JSON Schemas.\n\nExample\n-------\n\n::\n\n    import jsl\n\n    class Entry(jsl.Document):\n        name = jsl.StringField(required=True)\n\n    class File(Entry):\n        content = jsl.StringField(required=True)\n\n    class Directory(Entry):\n        content = jsl.ArrayField(jsl.OneOfField([\n            jsl.DocumentField(File, as_ref=True),\n            jsl.DocumentField(jsl.RECURSIVE_REFERENCE_CONSTANT)\n        ]), required=True)\n\n``Directory.get_schema(ordered=True)`` will return the following JSON schema:\n\n::\n\n    {\n        \"$schema\": \"http://json-schema.org/draft-04/schema#\",\n        \"definitions\": {\n            \"directory\": {\n                \"type\": \"object\",\n                \"properties\": {\n                    \"name\": {\"type\": \"string\"},\n                    \"content\": {\n                        \"type\": \"array\",\n                        \"items\": {\n                            \"oneOf\": [\n                                {\"$ref\": \"#/definitions/file\"},\n                                {\"$ref\": \"#/definitions/directory\"}\n                            ]\n                        }\n                    }\n                },\n                \"required\": [\"name\", \"content\"],\n                \"additionalProperties\": false\n            },\n            \"file\": {\n                \"type\": \"object\",\n                \"properties\": {\n                    \"name\": {\"type\": \"string\"},\n                    \"content\": {\"type\": \"string\"}\n                },\n                \"required\": [\"name\", \"content\"],\n                \"additionalProperties\": false\n            }\n        },\n        \"$ref\": \"#/definitions/directory\"\n    }\n\nInstalling\n----------\n\n::\n\n    pip install jsl\n\nLicense\n-------\n\n`BSD license`_\n\n.. _Documentation: http://jsl.readthedocs.org/\n.. _GitHub: https://github.com/aromanovich/jsl\n.. _PyPI: https://pypi.python.org/pypi/jsl\n.. _BSD license: https://github.com/aromanovich/jsl/blob/master/LICENSE\n",
         "name": "jsl",
         "packages": [
            "jsl",
            "jsl.fields",
            "jsl._compat"
         ],
         "url": "https://jsl.readthedocs.org",
         "version": "0.2.1"
      }
   }
]
```

## Tests
For starting tests you have to execute:
```
make check
```

## Principles

Mercator 1.0 was written mostly in Python, while Python can be considered a ubiquity in certain circles, less so in other circles. The main principle and reason for rewrite was to accomodate for the cases where Python was not installed and installing it didn't make any sense. Expecting a Java dev to install Python is a no-no. Mercator 2.0 is divided between two main components:

* Core
* Handlers

Where `Core` is a statically linked binary (thus no external dependencies) and `Handlers` are written in ecosystem specific language, the reason for this is two-fold:

* The target language is best equipped for handling it's ecosystem as it already contains all the necessary bits to handle the packaging
* The target language is already present on the box, if I'm developing in Java I have no problem running a handler written in Java since the necessary tooling already has to be there

Another crucial difference is that the handler specification is now declarative, and not some random code in some source file, but more about that below.

### Handler Definition

Ecosystem specific handlers are configurable via special file `handlers.yml`, which allows for specifying the following criteria:

```yaml
  - name: "Python"
    description: "PyPI Package (source)"
    filepatterns:
     - "^setup\\.py$"
    binary: "python"
    handler: "handlers/python"
```

* `Filepatterns` allow for selection of file based on regular expression matching a file name
* `binary` specifies the main executable supposed to execute the handler
* `handler` is the actual handler, the directory is relative to Mercator data directory (configurable in the same YAML, defaults to `/usr/share/mercator/`)

```yaml
  - name: "Ruby-Dist"
    description: "Installed RubyGem"
    pathpatterns:
     - "^.*/specifications/.*\\.gemspec$"
    handler: "handlers/ruby"
    binary: "ruby"
```

* `pathpatterns` allow for selection of file based on regular expression matching the whole absolute path

And finally, because Java always needs a special care, there are the following configuration keys available:

```yaml
  - name: "Java"
    description: "Java JAR"
    types:
     - "application/zip"
    inarchive:
     - "META-INF/MANIFEST.MF"
    handler: "handlers/java"
    binary: "java"
    args:
     - "-jar"
```

* `types` allows to specify a MIME type the file should have
* `inarchive` specifies a file that has to be present if `types` referes to an archive (currently only ZIP is supported)
* `args` additional arguments to the `binary`

### Handler Implementation

As stated above, each handler is implemented in the language of the ecosystem, that is, Python handler is written in Python, Java handler in Java etc.
If the handler needs any additional dependencies, those are bundled in the `handlers/` directory directly (currently Java and Python has some bundled dependencies).
