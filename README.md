Mercator 2.0
============

Mercator is a Swiss army knife for obtaining package metadata across various package ecosystems:

| Language | Ecosystem |
|----------|-----------|
| Python   | PyPI |
| Ruby     | Gems |
| Node     | NPM |
| Java     | Maven |
| Rust     | Cargo |
| .NET     | Nuget |
| Haskell  | Hackage |

Simply point Mercator at some directory and it will walk down all child directories and collect information
about all encountered package manifests. The output is always a JSON document describing what has been
found, please note that the key/value layout of the JSON document depends on the package ecosystem
that produced it, so if you want to do some further processing or analytics you may want to [normalize the data](https://github.com/fabric8-analytics/fabric8-analytics-worker/blob/master/f8a_worker/data_normalizer.py).

## Contributing

See our [contributing guidelines](https://github.com/fabric8-analytics/common/blob/master/CONTRIBUTING.md) for more info.
 
## Installation

Necessary dependencies (package names are taken from Fedora) to build Mercator without any handlers:

```
openssl-devel git golang make
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
python3 python3-devel
```

Javascript:

```
nodejs
```

Dotnet:

```
mono-devel nuget
```

Golang:

```
glide
```

for Dotnet you have to execute this command first:

```
yes | certmgr -ssl https://go.microsoft.com && yes | certmgr -ssl https://nuget.org
```

All handler dependencies together:
```
ruby java-devel python3 python3-devel nodejs mono-devel nuget glide
```

If you have all the packages installed, make sure that your `GOPATH` is set.
You can set it to for example `$(pwd)` or `/tmp` like: `export GOPATH=/tmp`.

Then just invoke `make`:

```
make build
```

Some handlers are built/installed by default, some need to be explicitly enabled,
see beginning of [Makefile](Makefile).

If you need to build for example dotnet handler (which is disabled by default),
you either have to change `DOTNET=NO` to `DOTNET=YES` in [Makefile](Makefile) or
build it like:

```
make build DOTNET=YES
```

Note: You can also take a look at our [spec file](mercator.spec), which we use to build RPMs.

## Running

After that, `Mercator` is ready to be used:

```
$ ./mercator .

{
   "items": [
      {
         "time": "2017-12-13T12:23:22.08193682+01:00",
         "path": ".../handlers/dotnet_handler/MercatorDotNet/Properties/AssemblyInfo.cs",
         "ecosystem": "DotNetSolution",
         "result": {
            "copyright": "Copyright ©  2016",
            "file_version": "1.0.0.0",
            "guid": "3f6a93dd-da21-4333-8610-ceda4d4853da",
            "name": "MercatorDotNet",
            "product": "MercatorDotNet",
            "version": "1.0.0.0"
         },
         "digests": {
            "manifest": "1e1dcf1f04f12b25cceed23593f2202ae9884e6f"
         }
      },
      {...}
    ]
}
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
