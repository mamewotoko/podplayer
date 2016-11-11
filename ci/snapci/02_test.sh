#! /bin/bash
#set -e

## usage
# lang country screen_size target abi

LANGUAGE=$1
if [ -z "$LANG" ]; then
    LANGUAGE=en
fi

COUNTRY=$2
if [ -z "$COUNTRY" ]; then
    COUNTRY=us
fi

SCREEN_SIZE=$3
if [ -z "$SCREEN_SIZE" ]; then
    SCREEN_SIZE=480x800
fi

TARGET=$4
if [ -z "$TARGET" ]; then
    TARGET=android-10
fi

ABI=$5
if [ -z "$ABI" ]; then
    ABI=default/armeabi-v7a
fi

AVD_NAME=emu_${TARGET}_${SCREEN_SIZE}_${LANGUAGE}_${COUNTRY}

#echo ----
#android list sdk -e --all
#echo ----

echo "android create avd -n $AVD_NAME -b $ABI -t $TARGET -c 32M --skin $SCREEN_SIZE"
echo no | android create avd --force -n $AVD_NAME --abi $ABI -t $TARGET -c 32M --skin $SCREEN_SIZE || exit 1
emulator -avd $AVD_NAME -prop persist.sys.language=$LANGUAGE -prop persist.sys.country=$COUNTRY -no-window &
sleep 90
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
adb logcat > app/build/logcat.log &

./gradlew spoonDebug -PspoonOutput=spoon_${AVD_NAME}
./gradlew spoonDebug -PspoonClassName=com.mamewo.podplayer0.tests.TestPodplayerExpActivity -PspoonOutput=spoon_exp_${AVD_NAME}
./gradlew spoonDebug -PspoonClassName=com.mamewo.podplayer0.tests.TestPodcastListPreference -PspoonOutput=spoon_podlist_${AVD_NAME}

adb shell reboot -p
sleep 90
