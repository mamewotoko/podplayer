#! /bin/sh
URL=http://robotium.googlecode.com/files/robotium-solo-3.2.1.jar
if [ ! -f robotium-solo-3.2.1.jar ]; then
    wget $URL
fi

URL2=http://scirocco.googlecode.com/files/scirocco_v2.0.jar
if [ ! -f scirocco_v2.0.jar ]; then
    wget $URL2
fi
