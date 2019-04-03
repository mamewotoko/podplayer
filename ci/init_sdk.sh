#! /bin/bash
ANDROID_HOME=/opt/android-sdk/

# echo y | android update sdk --no-ui --all --filter android-19,android-10,android-24 # > /dev/null
# echo y | android update sdk --no-ui --filter tools,platform-tools > /dev/null
# echo y | android update sdk --no-ui --all --filter build-tools-24.0.3 # > /dev/null
# echo y | android update sdk --no-ui --all --filter extra-android-support > /dev/null
# echo y | android update sdk --no-ui --filter extra-android-m2repository > /dev/null

# echo y | android update sdk --no-ui --all --filter sys-img-armeabi-v7a-android-10 > /dev/null
# echo y | android update sdk --no-ui --all --filter sys-img-armeabi-v7a-android-19 > /dev/null

echo y | $ANDROID_HOME/tools/bin/sdkmanager "build-tools;26.0.1" emulator "platform-tools" "extras;android;m2repository" "extras;google;m2repository" "platforms;android-24"
