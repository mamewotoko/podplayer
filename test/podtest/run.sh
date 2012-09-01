#! /bin/sh
COVERAGE_FILE=/data/data/com.mamewo.podplayer0/coverage.ec
CLASS_LIST="PodplayerExpActivityTest PodplayerActivityTest"

#adb shell rm -f $COVERAGE_FILE
for class in $CLASS_LIST
do
    echo $class
    adb shell am instrument -w -e coverage true -e coverageFile $COVERAGE_FILE \
	-e class com.mamewo.podplayer0.tests.$class \
	-e coverage true \
	com.mamewo.podplayer0.tests/android.test.InstrumentationTestRunner
done
adb pull $COVERAGE_FILE bin/coverage.ec
java -cp $ANDROID_HOME/tools/lib/emma.jar emma report -r html -in ../../bin/coverage.em,bin/coverage.ec -sp ../../src

