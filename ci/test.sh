#! /bin/bash
bash -x ci/snapci/02_test.sh en us 480x800 android-19 "default;x86"
sh ci/upload.sh app/build/
