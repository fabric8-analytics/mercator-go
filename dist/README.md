# Dist files for Mercator Go

## How to build Mercator Go for OpenShift

1. Clone the repo from https://github.com/fabric8-analytics/mercator-go.git 
2. Go to the cloned repo dir and build `mercator` by running (follow the instructions from https://github.com/fabric8-analytics/mercator-go):

   ```sh
$ make build  # probably you would like to specify `GOPATH` environment variable as well
   ```

3. Enter `dist` dir and run `./build-for-openshift.sh` (uses `mercator.spec` specfile). Make sure you have enabled ecosystems that you want to use in `mercator.spec` (in the `%install` section).
4. Use Mercator's Fedora Copr at https://copr.fedorainfracloud.org/coprs/jpopelka/mercator/

