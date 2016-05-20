#! /usr/local/bin/groovy
node {
  stage 'Checkout'
  checkout scm
  sh 'git submodule update --init'  

  stage 'Build'	      
  //TODO: use docker....
  env.ANDROID_HOME='/opt/android-sdk-linux/'
  env.JAVA_HOME='/opt/jdk1.7.0_05/'
  env.PATH="${env.JAVA_HOME}/bin:${env.ANDROID_HOME}/tools:${env.PATH}"

  sh './gradlew assembleDebug lint'
}