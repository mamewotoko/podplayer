<img src="https://github.com/mamewotoko/podplayer/raw/pullupdate/res/drawable-hdpi/ic_launcher.png" width="40" height="40">podplayer - An android podcast player
=====================================
[![Build Status](https://travis-ci.org/mamewotoko/podplayer.svg?branch=gradle)](https://travis-ci.org/mamewotoko/podplayer)

Screenshot
----------
![main screen](https://raw.githubusercontent.com/mamewotoko/podplayer/master/doc/mainscreen.png)
![gestures](https://raw.githubusercontent.com/mamewotoko/podplayer/master/doc/gesture_dialog.png)

Google Play
------------
[![play web page](http://www.android.com/images/brand/get_it_on_play_logo_small.png)](https://play.google.com/store/apps/details?id=com.mamewo.podplayer0)
<img src="https://github.com/mamewotoko/podplayer/raw/pullupdate/res/drawable/qr.png" width="86" height="86">
  
https://play.google.com/store/apps/details?id=com.mamewo.podplayer0

How to build
------------
1. Install "Android support repository" using [Android SDK Manager](https://developer.android.com/studio/intro/update.html?hl=ja#sdk-manager)
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
* fast load
  * improbe incremental update algorith to reduce cpu usage
    * problem: displayed episode is not played by click while loading episodes
    * fix flicker while loading episode
* add option to exit by pressing back button
  * display confirm dialog: exit or playing background, live as servcie and display notification
* cache
  * podcast xml
  * episode item
    * introduce simple memory based cache
* reduce apk file size
* select http lib
  * point of view
    * http response cache
    * authentication (w/ cache?)
      * preemptive
  * libraries
    * HttpURLConnection
    * OkHTTP 2.x
    * OkHTTP 3.x
      https://github.com/square/okhttp/wiki/Recipes
      * cache
      * auth, counting retry
    * Volley
      * cache
      * customize http header to implement authentication
    * Android Asynchronous Http Client
      http://loopj.com/android-async-http/
* http authenticate for MediaPlayer
  * add username, password into url
    http://user:password@hostname/path
    * supports only basic authentication
* display toast when network error ocurr
* handle opml
* share podcast with friend / author (add to preset)
  * bluetooth
  * IR code
  * mail?
  * post to website
* thanks dialog?
  * FIXED: checkbox of preference is gray based
    * http://stackoverflow.com/questions/27091845/android-appcompat-dark-theme-settings-checkbox
* set default item / show notification in paused state
  * start player service
* fix #1
  * content corrupted?
  * debug: using TeeInputStream of Commons IO
* improve UI
  * design layout with AndroidStudio
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
* fix bugs
  * checkbox of preference is gray based
    * http://stackoverflow.com/questions/27091845/android-appcompat-dark-theme-settings-checkbox
  * when prepare error occurs, cursor moves to next episode
    * stop playing
      * or mark error item and try playing next unerror item
  * when only one episode is registered, list is in tap to refresh mode after loaded
* use database to manage loaded episodes
  * display mark which is already played, new item etc...
  * to reduce reload of podcast
  * show description of playing episode
  * save podcast list and updated time as state
  * add/mark listened item list
    * ? not listened / ! listened
    * remember listened timestamp
* add score thershold preference dialog 
* fix lint warnings
  * contentDescription of image view
* spoon test
   * test multiple test classes once
    * current situation: freeze?
    * set "package" instrumentationArgs to start all test classes
* add preference to display pubdate as "X days before"
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
* http://developer.android.com/intl/ja/training/improving-layouts/smooth-scrolling.html#ViewHolder
* add / update test
  * MainActivityTest, PodcastActivity then testAbortReload blocks...
  * preference
  * notification
  * gesture?
    * gesture command
  * landscape UI
* gesture score histogram?
* improve UI to add podcast URL
  * update main activity when setting is changed
* reset playing position after podcast selection is changed
* optimize initialization of podplayer
  * setContentView takes long time
* Autoload: load when create activity or when podcast list setting is changed
* play episode which is clicked while preparing other episode
* add episode search UI
* add cool widget to play episode
* add slider to show current playing position
* add error handling
* write user guide?
* display playing icon in group of expandable list
* upload demo movie to youtube

License
----------
* podplayer: Copyright (c) 2012-2016 Takashi Masuyama. All rights reserved. 
Licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)

* podplayer uses the following software and resource
  * Copyright (c) 2011 [Johan Nilsson](http://markupartist.com)  
    https://github.com/johannilsson/android-pulltorefresh
  * [Glide](https://github.com/bumptech/glide)  
    https://github.com/bumptech/glide
  * [Meterial icons](https://design.google.com/icons/)

* Gesture data is built using GestureBuilder sample application of android (legacy/GestureBuilder)

Keywords
----------
Android, MediaPlayer, Podcast, AsyncTask, PullToRefresh, Gesture, Gradle, Spoon, FalconSpoon, Robotium, Glide, TravisCI

----
Takashi Masuyama < mamewotoko@gmail.com >  
http://mamewo.ddo.jp/
