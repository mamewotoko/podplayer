#! /bin/sh
COVERAGE_FILE=/data/data/com.mamewo.podplayer0/coverage.ec
REPORT_FILE=/data/data/com.mamewo.podplayer0/files/TEST-all.xml
CLASS_LIST="com.mamewo.podplayer0.tests.PodplayerActivityTest"
#com.mamewo.podplayer0.tests.PodplayerExpActivityTest,\
#com.mamewo.podplayer0.tests.PodcastListPreferenceTest,\
#com.mamewo.podplayer0.tests.MainActivityTest"
RUNNER=com.neenbedankt.android.test.InstrumentationTestRunner
ADB=$ANDROID_HOME/platform-tools/adb

#adb shell rm -f $COVERAGE_FILE
$ADB shell am instrument -w \
  -e class $CLASS_LIST \
  com.mamewo.podplayer0.tests/$RUNNER

#  -e coverage true  -e coverageFile $COVERAGE_FILE \
#$ADB pull $COVERAGE_FILE bin/coverage.ec
# $ADB pull $REPORT_FILE TEST-all.xml
# java -cp $ANDROID_HOME/tools/lib/emma.jar emma report -r html -in ../../bin/coverage.em,bin/coverage.ec -sp ../../src
# rm -rf bin/coverage
# if [ -d coverage ]; then
#     mv coverage bin
# fi
