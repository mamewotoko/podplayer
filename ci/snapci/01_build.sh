#! /bin/bash
set -e
./gradlew assembleDebug assembleDebugAndroidTest lint
