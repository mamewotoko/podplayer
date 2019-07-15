#! /bin/bash

./gradlew build
curl -L 'https://drive.google.com/uc?id=1Ej8VgsW5RgK66Btb9p74tSdHMH3p4UNb&export=download' > gdrive
chmod +x gdrive
sh ci/upload.sh app/build/outputs/apk
