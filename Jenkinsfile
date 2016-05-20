#! /usr/local/bin/groovy
node {
  stage 'Checkout'
  checkout scm
  sh 'git submodule update --init'  

  //TODO: use docker....
  env.ANDROID_HOME='/opt/android-sdk-linux/'
  env.PATH="${env.ANDROID_HOME}/tools}:${env.PATH}"

  sh 'pwd'
  sh 'ls'
  stage 'Build'	      
  sh './gradlew assembleDebug lint'
}