#! /bin/bash
set -e
./gradlew clean assembleDebug assembleDebugAndroidTest lint
