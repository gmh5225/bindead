#!/bin/sh
## Requires Apache Maven installed and an internet connection as Maven will download lots of things the first time!
set -e #immediately exit on errors

# start with a clean build
mvn clean
# install 3rd party libraries to the maven repository
# this is executed automatically by the compile, install, package phases so no need to call it separately
# mvn initialize
# do the normal install/package but skip tests for a faster build
mvn package -DskipTests
# copy the standalone analyzer to the toplevel directory
cp binspot/target/bindead-*-shaded.jar bindead.jar