#! /usr/local/bin/groovy
node {
  //TODO: use docker....
  env.ANDROID_HOME='/opt/android-sdk-linux/'
  env.PATH="${env.ANDROID_HOME}/tools}:${env.PATH}"

  stage 'Build'	      
  sh './gradlew assembleDebug lint'
}