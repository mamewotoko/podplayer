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
//    sh 'pwd'
//    sh 'ls -l'
    echo 'archiveArtifacts'
    archiveArtifacts 'app/build/**/*.apk'
    echo 'publishHTML'
    publishHTML(target: [allowMissing: true,
                         alwaysLinkToLastBuild: false,
                         keepAll: true,
                         reportDir: 'app/build/spoon_emu_android-10_480x800_en_us',
                         reportFiles: 'index.html',
                         reportName: 'Spoon pulltorefresh (android-10)'
                        ])
    publishHTML(target: [allowMissing: true,
                         alwaysLinkToLastBuild: false,
                         keepAll: true,
                         reportDir: 'app/build/spoon_exp_emu_android-10_480x800_en_us',
                         reportFiles: 'index.html',
                         reportName: 'Spoon exp (android-10)'
                        ])

    publishHTML(target: [allowMissing: true,
                         alwaysLinkToLastBuild: false,
                         keepAll: true,
                         reportDir: 'app/build/spoon_podlist_emu_android-10_480x800_en_us',
                         reportFiles: 'index.html',
                         reportName: 'Spoon podlist (android-10)'
                        ])
    // echo 'copyArtifact'
    // step([$class: 'CopyArtifact',
    //       projectName: 'podplayer_pipeline',
    //       target: 'app/build']);
  }
}
