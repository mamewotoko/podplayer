#!/bin/bash

# raise an error if any command fails!
set -e

# prerequisite
# 1. android sdk is installed in $ANDROID_HOME 

sh ci/init_sdk.sh

## give up
#sudo yum update
#sudo yum install -y glibc.i686
#curl -L ci/spoon-runner.jar

# to merge test report
sudo pip install lxml
