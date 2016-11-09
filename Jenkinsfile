#! groovy
node {
  stage 'Checkout'
  checkout scm
  sh 'git submodule update --init'  

  stage 'Build'	      
   env.PATH="${env.JAVA_HOME}/bin:${env.ANDROID_HOME}/tools:${env.PATH}"

  sh './gradlew assembleDebug lint'

  stage 'Build test'
  sh './gradlew assembleAndroidTest'

  stage 'Run test'
  sh 'sh ci/snapci/02_test.sh'

  stage 'Report'
  step([$class: 'LintPublisher'])
}
