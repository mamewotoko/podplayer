#! groovy

node('podplayer_pipeline') {
  stage('Checkout') {
    checkout scm
    sh 'git submodule update --init'
    sh 'ps auxww --sort command | tee ps_before.txt'
  }

  stage('Build') {
    env.PATH="${env.JAVA_HOME}/bin:${env.ANDROID_HOME}/tools:${env.ANDROID_HOME}/tools/bin:${env.ANDROID_HOME}/platform-tools:${env.PATH}"
    sh './gradlew clean assembleDebug lint'
  }

  stage('Lint report'){
    step([$class: 'LintPublisher']);
  }
  
  stage('Build test'){
    sh './gradlew assembleAndroidTest'
  }

  //TODO: parallel
  stage('Run test'){
    timestamps {
      timeout(time: 6, unit: 'HOURS'){
        // sh 'bash -x ci/snapci/02_test.sh en us 480x800 android-10 "default;x86"'
        // sh 'bash -x ci/snapci/02_test.sh en us 480x800 android-19 "default;x86"'
        // sh 'bash -x ci/snapci/02_test.sh en us 1280x800 android-19 "default;x86"'
        // sh 'bash -x ci/snapci/02_test.sh ja jp 1280x800 android-19 "default;x86"'
        // sh 'bash -x ci/snapci/02_test.sh ja jp 480x800 android-19 "default;x86"'
        // sh 'bash -x ci/snapci/02_test.sh sv se 1280x800 android-19 "default;x86"'
        //sh 'bash -x ci/snapci/02_test.sh sv se 480x800 android-19 "default;x86"'
        sh 'bash -x ci/snapci/02_test.sh en us 480x800 android-19 "default;x86"'

        sh 'ps auxww --sort command | tee ps_after.txt'
      }
    }
  }

  stage('Report'){
    echo 'archiveArtifacts'
    archiveArtifacts 'app/build/**/*.apk'

    echo 'merge report'
    sh 'sh -x ci/snapci/03_report.sh'
    sh 'rm -rf app/build/intermediates app/build/generated app/build/tmp'
    
    publishHTML(target: [allowMissing: true,
                         alwaysLinkToLastBuild: false,
                         keepAll: true,
                         reportDir: 'app/build/',
                         reportFiles: 'merged.html',
                         reportName: 'Spoon result'
                        ])
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
                         reportDir: 'app/build/spoon_exp_emu_'+target+'_'+resolution+'_'+lang+'_'+country+'/debug',
                         reportFiles: 'index.html',
                         reportName: 'Spoon exp '+target+'_'+resolution+'_'+lang
                        ]);
    publishHTML(target: [allowMissing: true,
                         alwaysLinkToLastBuild: false,
                         keepAll: true,
                         reportDir: 'app/build/spoon_podlist_emu_'+target+'_'+resolution+'_'+lang+'_'+country+'/debug',
                         reportFiles: 'index.html',
                         reportName: 'Spoon podlist '+target+'_'+resolution+'_'+lang
                        ]);
}
