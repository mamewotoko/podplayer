#! /bin/bash
set -ex

bash -x ci/snapci/02_test.sh en us 480x800 android-21 "default;x86"
curl -L 'https://drive.google.com/uc?id=1Ej8VgsW5RgK66Btb9p74tSdHMH3p4UNb&export=download' > gdrive
chmod +x gdrive
sh ci/upload.sh app/build/
