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

  //TODO: parallel
  stage('Run test'){
    sh 'bash -x ci/snapci/02_test.sh en us 480x800 android-10 default/x86'

    sh 'bash -x ci/snapci/02_test.sh en us 480x800 android-19 default/x86'
    sh 'bash -x ci/snapci/02_test.sh en us 1280x800 android-19 default/x86'
    sh 'bash -x ci/snapci/02_test.sh ja jp 1280x800 android-19 default/x86'
    sh 'bash -x ci/snapci/02_test.sh ja jp 480x800 android-19 default/x86'
    sh 'bash -x ci/snapci/02_test.sh sv se 1280x800 android-19 default/x86'
    sh 'bash -x ci/snapci/02_test.sh sv se 480x800 android-19 default/x86'
  }

  stage('Report'){
    // null pointer exception...
//    sh 'pwd'
//    sh 'ls -l'
    echo 'archiveArtifacts'
    archiveArtifacts 'app/build/**/*.apk'
    echo 'publishHTML'

    //1
    // publishHTML(target: [allowMissing: true,
    //                      alwaysLinkToLastBuild: false,
    //                      keepAll: true,
    //                      reportDir: 'app/build/spoon_emu_android-10_480x800_en_us/debug',
    //                      reportFiles: 'index.html',
    //                      reportName: 'Spoon pulltorefresh android-10'
    //                     ])
    // publishHTML(target: [allowMissing: true,
    //                      alwaysLinkToLastBuild: false,
    //                      keepAll: true,
    //                      reportDir: 'app/build/spoon_exp_emu_android-10_480x800_en_us/debug',
    //                      reportFiles: 'index.html',
    //                      reportName: 'Spoon expd android-10'
    //                     ])

    // publishHTML(target: [allowMissing: true,
    //                      alwaysLinkToLastBuild: false,
    //                      keepAll: true,
    //                      reportDir: 'app/build/spoon_podlist_emu_android-10_480x800_en_us/debug',
    //                      reportFiles: 'index.html',
    //                      reportName: 'Spoon podlist android-10'
    //                     ])

    publishSpoonResult('android-10', '480x800', 'en', 'us')

    publishSpoonResult('android-19', '480x800', 'en', 'us')
    publishSpoonResult('android-19', '1280x800', 'en', 'us')

    publishSpoonResult('android-19', '480x800', 'ja', 'jp')
    publishSpoonResult('android-19', '1280x800', 'ja', 'jp')
    
    publishSpoonResult('android-19', '480x800', 'sv', 'se')
    publishSpoonResult('android-19', '1280x800', 'sv', 'se')

//                      echo 'copyArtifact'
       // step([$class: 'CopyArtifact',
    //       projectName: 'podplayer_pipeline',
    //       target: 'app/build']);
  }
}


def publishSpoonResult(target, resolution, lang, country){
    publishHTML(target: [allowMissing: true,
                         alwaysLinkToLastBuild: false,
                         keepAll: true,
                         reportDir: 'app/build/spoon_emu_'+target+'_'+resolution+'_'+lang+'_'+country+'/debug',
                         reportFiles: 'index.html',
                         reportName: 'Spoon pulltorefresh '+target+'_'+resolution+'_'+lang
                        ]);
    publishHTML(target: [allowMissing: true,
                         alwaysLinkToLastBuild: false,
                         keepAll: true,
                         reportDir: 'app/build/spoon_exp_emu_'+target+'+_+*'+resolution+'_'+lang+'_'+country+'/debug',
                         reportFiles: 'index.html',
                         reportName: 'Spoon exp '+target+'_'+resolution+'_'+lang
                        ]);
    publishHTML(target: [allowMissing: true,
                         alwaysLinkToLastBuild: false,
                         keepAll: true,
                         reportDir: 'app/build/spoon_podlist_emu_'+target+'+_+*'+resolution+'_'+lang+'_'+country+'/debug',
                         reportFiles: 'index.html',
                         reportName: 'Spoon podlist'+target+'_'+resolution+'_'+lang
                        ]);
}
