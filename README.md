<img src="https://github.com/mamewotoko/podplayer/raw/pullupdate/res/drawable-hdpi/ic_launcher.png" width="40" height="40">podplayer - An android podcast player
=====================================
[![Build Status](https://travis-ci.org/mamewotoko/podplayer.svg?branch=gradle)](https://travis-ci.org/mamewotoko/podplayer)

Screenshot
----------
![main screen](https://raw.githubusercontent.com/mamewotoko/podplayer/master/doc/mainscreen.png)
![gestures](https://raw.githubusercontent.com/mamewotoko/podplayer/master/doc/gesture_dialog.png)

Video
-----
[![podplayer](https://img.youtube.com/vi/WbFhnjb_RTg/0.jpg)](https://www.youtube.com/watch?v=WbFhnjb_RTg "podplayer")

Google Play
------------
[<img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" width="192" height="75">](https://play.google.com/store/apps/details?id=com.mamewo.podplayer0)
<img src="https://github.com/mamewotoko/podplayer/raw/pullupdate/res/drawable/qr.png" width="86" height="86">
  
https://play.google.com/store/apps/details?id=com.mamewo.podplayer0

How to build
------------
1. Install "Android support repository" using [Android SDK Manager](https://developer.android.com/studio/intro/update.html?hl=ja#sdk-manager)

  Install script (in spapci): ` ci/snapci/init_android.sh ` 

2. Clone source

    ```
    git clone https://github.com/mamewotoko/podplayer.git
    git submodule update --init
    ```

### Gradle
1. Build

    ```
    ./gradlew assembleDebug
    ```

apk file created in ` ./app/build/outputs/apk/app-debug.apk `

A file bin/podplayer-debug.apk is created if succeed.

How to run UI test using Robotium
---------------------------------
1. Connect android device or start android emulator
2. Start testing

    ```
    ./gradlew spoon
    ```
3. A test report is created as ` app/build/spoon/debug/index.html `
4. To select test class, pass spoonClassName gradle property with test class name

   ```
   ./gradlew -PspoonClassName=com.mamewo.podplayer0.tests.TestPodplayerExpActivity spoon
   ```

TODO
----
* display author info of episode/podcast
* ask stop playing episode when exit menu is selected
* realm integration
  * putlltorefresh view
    * filter
  * card view
  * exp view
* add settings
  * cache size
    * icon, podcast xml
    * display current used cache size
    * modify size in MB
* Try navutil
* spoon test
  * bug: MainActivityTest, PodcastActivity then testAbortReload blocks...
  * add test of share function
    * QR code
    * mail
    * Twitter
  * test multiple test classes once / merge test result
    * current situation: freeze?
    * set "package" instrumentationArgs to start all test classes
  * add test of content description (manual test)
  * preference
  * notification
  * landscape UI
  * enable episode long click
* fix bug
  * open Podcast list editor, press detail button, rotate screen
    -> crash
    -> selectedPodcastInfo_ is null, touched
* podcast detail as activity
  * channel/description or itunes:subtitle
  * channel/copyright
* add podcast to podcast site
  * Luke's ENGLISH Podcast - Learn British English with Luke Thompson
    https://audioboom.com/channels/1919834.rss
* fix bugs
  * when prepare error occurs, cursor moves to next episode
    * stop playing
      * or mark error item and try playing next unerror item
  * test: Solo.clickOnMenuItem does not work (when screen is landscape? e.g. 1280x800)
    * menu key does not work
* fast load
  * improve incremental update algorithm to reduce cpu usage
    * problem: displayed episode is not played by click while loading episodes
* podcast title should be nullable?    
* change color of category text in preference screen   
* add option to exit by pressing back button
  * display confirm dialog: exit or playing background, live as servcie and display notification
* cache
  * podcast xml
  * episode item
    * introduce simple memory based cache
* display toast when network error occurs
* handle opml as input to specify podcast list
* share podcast with friend / author (add to preset)
  * bluetooth
  * Google+
  * Facebook
  * as text
* set default item / show notification in paused state
  * start player service
* use minifyEnabled option of build.gradle
  * disable logging
* fix lint warnings
  * review lint.xml and enable appropriate options
  * check report of code inspection of Android Studio
  * enable Google App Indexing
* improve UI
  * design layout with AndroidStudio
  * use com.android.support:design (material design)
  * notification layout
  * use RecyclerView
    * animation
    * efficient memory use?
  * material icon: change loading icon of pulltorefresh  
* support http auth for fetching podcast xml and episodes (#2)
  * cache auth info for each host?
  * support preemptive authentication: http://hc.apache.org/httpclient-3.x/authentication.html
    * use username and password for podcast xml to get episode file
    * ver1: simple authenticator
      host,port,realm,user,password,auth_method
    * ver2: use AccountManager to manage host, username, password
* build podcast parser with gradle
  * fix dependency
    * commons-io
    * okhttp3 (linked with podcast_parser)
* sort by pubdate
  * fix sort crash
    * or sort on database
  * add test
* modify icon
  * use opaque orange
* just add podcast url without check (preference)? 
* use database to manage loaded episodes
  * display mark which is already played, new item etc...
  * to reduce reload of podcast
  * show description of playing episode
  * save podcast list and updated time as state
  * add/mark listened item list
    * ? not listened / ! listened
    * remember listened timestamp
* add score thershold preference dialog 
* add preference to display pubdate as "X days before"
* add activity for tv?
* save latest few item to savedInstanceState
* filter not listened item only (preference)
* move state to service
  * remove array adapter contents?
* use large notification
  * change notification icon for Android4.0
  * show both podcast title and episode title
  * avoid flicker when button on notification pressed
* write additional podcast url to sd card?
* add confirm dialog to open web site
* improve UI to add podcast URL
  * update main activity when setting is changed
* reset playing position after podcast selection is changed
* optimize initialization of podplayer
  * setContentView takes long time
* Autoload: load when create activity or when podcast list setting is changed
* play episode which is clicked while preparing other episode
* add episode search UI
* add slider to show current playing position
* add error handling
* write user guide?
* display playing icon in group of expandable list
* upload demo movie to youtube

Third-party libraries
---------------------
The podplayer uses the following software, resource and tool.
* [Pull To Refresh for Android](https://github.com/johannilsson/android-pulltorefresh)
  Copyright (c) 2011 [Johan Nilsson](http://markupartist.com)  
* [Glide](https://github.com/bumptech/glide)  
* [Meterial icons](https://design.google.com/icons/)
* Gesture data is built with GestureBuilder sample application of android (legacy/GestureBuilder)

License
-------

    Copyright (c) 2012-2016 Takashi Masuyama. All rights reserved.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

Keywords
----------
Android, MediaPlayer, Podcast, AsyncTask, PullToRefresh, Gesture, Gradle,
Spoon, FalconSpoon, Robotium, Glide, Travis CI

----
Takashi Masuyama < mamewotoko@gmail.com >  
http://mamewo.ddo.jp/
