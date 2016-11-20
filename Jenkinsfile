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

  stage('Lint report'){
    step([$class: 'LintPublisher']);
  }
  
  stage('Build test'){
    sh './gradlew assembleAndroidTest'
  }

  stage('Run test'){
    sh 'bash -x ci/snapci/02_test.sh "" "" "" "" default/x86'
  }
  
  stage('Report'){
    // null pointer exception...
    // echo 'publishHTML'
    // publishHTML(target: [allowMissing: false,
    //                      allowLinkToLastBuild: false,
    //                      keepAll: true,
    //                      reportDir: 'app/build/spoon',
    //                      reportFiles: 'index',
    //                      reportFiles: 'Spoon result'
    //                     ]);
    echo 'copyArtifact'
    sh 'pwd'
    step([$class: 'CopyArtifact',
          projectName: 'podplayer_pipeline',
          target: 'app/build']);
  }
}
