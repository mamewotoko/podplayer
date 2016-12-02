#! /bin/sh

python ci/snapci/merge_spoon.py $* > merged.html
cp -r $(dirname $1)/static .
