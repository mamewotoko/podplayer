#! groovy
node('podplayer_pipeline') {
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
    archiveArtifaccts 'app/build'
    sh 'pwd'
    sh 'ls -l'
    echo 'publishHTML'
    publishHTML(target: [allowMissing: true,
                         alwaysLinkToLastBuild: false,
                         keepAll: true,
                         reportDir: 'app/build/spoon',
                         reportFiles: 'index'.html,
                         reportName: 'Spoon result'
                        ]);
    // echo 'copyArtifact'
    // step([$class: 'CopyArtifact',
    //       projectName: 'podplayer_pipeline',
    //       target: 'app/build']);
  }
}
