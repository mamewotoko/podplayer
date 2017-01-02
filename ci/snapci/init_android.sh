#!/bin/bash

# raise an error if any command fails!
set -e
MYDIR=$(realpath $(dirname "$0"))

# existance of this file indicates that all dependencies were previously installed, and any changes to this file will use a different filename.
INITIALIZATION_FILE="$ANDROID_HOME/.initialized-dependencies-$(git log -n 1 --format=%h -- $0)"

if [ ! -e ${INITIALIZATION_FILE} ]; then
    # fetch and initialize $ANDROID_HOME
    download-android

    sh $MYDIR/../init_sdk.sh

    ## give up
    #sudo yum update
    #sudo yum install -y glibc.i686
    #curl -L ci/spoon-runner.jar
    touch ${INITIALIZATION_FILE}
fi

## to merge report
sudo pip install lxml
