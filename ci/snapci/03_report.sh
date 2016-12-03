#! /bin/sh

( cd app/build; python ../../ci/snapci/merge_spoon.py spoon*/debug/index.html > merged.html; rm -f static; ls -d1 spoon*/debug/static | head -n1 | xargs -I {}  ln -s {} static )
#cp -r $(dirname $1)/static .

