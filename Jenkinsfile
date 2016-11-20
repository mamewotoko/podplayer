#! groovy
node {
  stage('Checkout') {
    checkout scm
    sh 'git submodule update --init'
  }

  stage('Build') {
    env.PATH="${env.JAVA_HOME}/bin:${env.ANDROID_HOME}/tools:${env.ANDROID_HOME}/platform-tools:${env.PATH}"
    sh './gradlew assembleDebug lint'
  }

  stage('Build test'){
    sh './gradlew assembleAndroidTest'
  }

  stage('Run test'){
    sh 'sh ci/snapci/02_test.sh "" "" "" "" default/x86'
  }
  
  stage('Report'){
    step([$class: 'LintPublisher'])
  }
}
