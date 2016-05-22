podplayer - An android podcast player [![Build Status](https://travis-ci.org/mamewotoko/podplayer.svg?branch=gradle)](https://travis-ci.org/mamewotoko/podplayer)
=====================================

What?
----------
<img src="https://github.com/mamewotoko/podplayer/raw/pullupdate/res/drawable-hdpi/ic_launcher.png" width="80" height="80">
A podcast player android application.

Screenshot
----------
![main screen](https://github.com/mamewotoko/podplayer/raw/pullupdate/doc/mainscreen.png)
![preference](https://github.com/mamewotoko/podplayer/raw/pullupdate/doc/preference.png)

Google Play
------------
 [![my play page](http://www.android.com/images/brand/get_it_on_play_logo_small.png)](http://play.google.com/store/search?q=pub:mamewo)  
<img src="https://github.com/mamewotoko/podplayer/raw/pullupdate/res/drawable/qr.png" width="86" height="86>
  
https://play.google.com/store/apps/details?id=com.mamewo.podplayer0

How to build
------------
1. Clone source

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

### Eclipse
1. Import this directory, whose name is podplayer.
2. Import the libsrc/pulltorefresh/pulltorefresh directory, whose name is pulltorefresh
3. Add project reference
    1. right click podplayer project -> properties. 
    2. select Project References. 
    3. check pulltorefresh. 
4. Run podplayer project as Android Application

How to run UI test using Robotium
---------------------------------
1. connect android device or start android emulator
2. start testing

    ```
    ./gradlew spoon
    ```
3. test report is created as ` app/build/spoon/debug/index.html `

TODO
----
* fix bugs
    * fix memory leak (bitmap problem)
        * check heap size and class histogram
        * put scaled image on memory
    * when prepare error occurs, cursor moves to next episode
        * stop playing
        * or mark error item and try playing next unerror item
   * when only one episode is registered, list is in tap to refresh mode after loaded
* add/mark listened item list
    * ? not listened / ! listened
    * remember listened timestamp
* spoon test
   * add logcat to report
   * test multiple test classes once
     * current: freeze?
* save latest few item to savedInstanceState
* filter not listened item only (preference)
* use database to manage loaded episodes
    * display mark which is already played, new item etc...
    * to reduce reload of podcast
    * show description of playing episode
    * save podcast list and updated time as state
* move state to service
    * remove array adapter contents?
* use large notification
    * change notification icon for Android4.0
* write additional podcast url to sd card?
* add confirm dialog to open web site
* cache podcast icon
    * avoid loading podcast when UI is switched (by rotation)
* http://developer.android.com/intl/ja/training/improving-layouts/smooth-scrolling.html#ViewHolder
* add / update test
    * MainActivityTest, PodcastActivity then testAbortReload blocks...
    * preference
    * notification
    * gesture?
    * landscape UI
* improve UI to add podcast URL
    * update main activity when setting is changed
* reset playing position after podcast selection is changed
* translate
    <string name="pref_episode_limit">Limit of episodes for each podcast</string>
    <string name="pref_episode_limit_title">Limit of episodes</string>
* optimize initialization of podplayer
    * setContentView takes long time
* Autoload: load when create activity or when podcast list setting is changed
* play episode which is clicked while preparing other episode
* sort episode by date
* add episode search UI
* add cool widget to play episode
* add slider to show current playing position
* add error handling
* write user guide?
* check apk size
* add Wifi only mode
    * acquire [wifi lock](http://developer.android.com/reference/android/net/wifi/WifiManager.WifiLock.html)
* display playing icon in group of expandable list

License
----------
* podplayer: Copyright (c) 2012-2016 Takashi Masuyama. All rights reserved. 
Licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)

* podplayer uses the following software which is licensed under the 
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html) 
Copyright (c) 2011 [Johan Nilsson](http://markupartist.com) 
https://github.com/johannilsson/android-pulltorefresh

Keywords
----------
Android, MediaPlayer, Podcast, AsyncTask, PullToRefresh, Gesture, Spoon, Robotium

----
Takashi Masuyama < mamewotoko@gmail.com >  
http://mamewo.ddo.jp/
