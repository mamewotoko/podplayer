#! /bin/bash

# add avd
echo no | android create avd -n emu-10 -t android-10 -c 32M
emulator -avd emu-10 -no-window &
sleep 90

adb logcat > logcat.log &
./gradlew spoonDebug

adb shell reboot -p
