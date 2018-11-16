#! /bin/bash
#trap "kill 0" SIGINT

# ANDROID_HOME=/opt/android-sdk
#set -e

## usage
# lang country screen_size target abi

LANGUAGE=$1
if [ -z "$LANGUAGE" ]; then
    LANGUAGE=en
fi

COUNTRY=$2
if [ -z "$COUNTRY" ]; then
    COUNTRY=us
fi

SCREEN_SIZE=$3
if [ -z "$SCREEN_SIZE" ]; then
    #SCREEN_SIZE=480x800
    SCREEN_SIZE=1280x800
fi

TARGET=$4
if [ -z "$TARGET" ]; then
    TARGET=android-10
fi

ABI=$5
if [ -z "$ABI" ]; then
    #ABI="default;armeabi-v7a"
    ABI="default;x86"
fi

TEST=$6

AVD_NAME=emu_${TARGET}_${SCREEN_SIZE}_${LANGUAGE}_${COUNTRY}

#echo ----
#android list sdk -e --all
#echo ----

echo y | sdkmanager "build-tools;26.0.1" "system-images;${TARGET};${ABI}" emulator "platform-tools" "platforms;${TARGET}" "extras;android;m2repository" "extras;google;m2repository"

#echo "android create avd -n $AVD_NAME -b $ABI -c 32M -k "
#echo no | android create avd --force -n $AVD_NAME -c 32M  || exit 1
echo "avdmanager create avd -n $AVD_NAME -c 32M -f -k system-images;${TARGET};${ABI}"

echo no | avdmanager create avd -n $AVD_NAME -c 32M -f -k "system-images;${TARGET};${ABI}" || exit 1

#( emulator -avd $AVD_NAME -prop persist.sys.language=$LANGUAGE -prop persist.sys.country=$COUNTRY -no-window ) &
pushd $ANDROID_HOME/tools
emulator -avd $AVD_NAME -prop persist.sys.language=$LANGUAGE -prop persist.sys.country=$COUNTRY -gpu off -no-window  &
EMULATOR_PID=$!
echo emulator pid $!
popd

sleep 120
adb devices
STATUS=$(adb wait-for-device shell getprop init.svc.bootanim)
echo STATUS: stopped is expected: $STATUS
# echo STATUS1: $STATUS
# if [ "$STATUS" != "stopped" ]; then
#     sleep 60
#     STATUS=$(adb wait-for-device shell getprop init.svc.bootanim)
# fi
# echo STATUS2: $STATUS
# if [ "$STATUS" != "stopped" ]; then
#     echo emulator does not start
#     exit 1
# fi
#adb uninstall com.mamewo.podplayer0 || true

./gradlew uninstallAll installDebug
adb logcat -v time > app/build/logcat.log &
LOGCAT_PID=$!

{
    ./gradlew spoonDebug -PspoonOutput=spoon_${AVD_NAME}
    if [ -z "$TEST" ]; then
        ./gradlew spoonDebug -PspoonClassName=com.mamewo.podplayer0.tests.TestPodplayerExpActivity -PspoonOutput=spoon_exp_${AVD_NAME}
        ./gradlew spoonDebug -PspoonClassName=com.mamewo.podplayer0.tests.TestPodplayerCardActivity -PspoonOutput=spoon_card_${AVD_NAME}
        ./gradlew spoonDebug -PspoonClassName=com.mamewo.podplayer0.tests.TestPodcastListPreference -PspoonOutput=spoon_podlist_${AVD_NAME}
        ./gradlew spoonDebug -PspoonClassName=com.mamewo.podplayer0.tests.TestPodplayerActivityLand -PspoonOutput=spoon_land_${AVD_NAME}
    fi
}

# finally
## TODO: get serial id
adb devices | grep -e emulator -e online | cut -f1 | while read line; do adb -s $line emu kill || true ; done
kill $LOGCAT_PID || true
kill -9 $EMULATOR_PID || true
killall -9 qemu-system-i386 || true
adb kill-server || true
pgrep -P $$ -l

# remove
# avdmanager delete avd -n $AVD_NAME

# kill all child process
pkill -P $$
exit 0
# TODO: uninstall package sdk
