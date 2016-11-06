#!/bin/bash

# raise an error if any command fails!
set -e
cat /proc/cpuinfo
cat /etc/issue

# existance of this file indicates that all dependencies were previously installed, and any changes to this file will use a different filename.
INITIALIZATION_FILE="$ANDROID_HOME/.initialized-dependencies-$(git log -n 1 --format=%h -- $0)"

if [ ! -e ${INITIALIZATION_FILE} ]; then
    # fetch and initialize $ANDROID_HOME
    download-android

    echo y | android update sdk --no-ui --filter android-23,android-10 > /dev/null
    echo y | android update sdk --no-ui --filter tools,platform-tools > /dev/null
    echo y | android update sdk --no-ui --all --filter build-tools-23.0.1 > /dev/null
    echo y | android update sdk --no-ui --filter extra-android-support > /dev/null
    echo y | android update sdk --no-ui --filter extra-android-m2repository > /dev/null

    echo y | android update sdk --no-ui --filter sys-img-armeabi-v7a-google_apis-10 --all > /dev/null

    ## give up
    #sudo yum update
    #sudo yum install -y glibc.i686
    touch ${INITIALIZATION_FILE}
fi
