#! /bin/sh

python merge_spoon.py $* > merged.html
cp -r $(dirname $1)/static .


