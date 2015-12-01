podplayer - An android podcast player [![Build Status](https://travis-ci.org/mamewotoko/podplayer.svg?branch=master)](https://travis-ci.org/mamewotoko/podplayer)
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
```bash
git clone git://github.com/mamewotoko/podplayer.git
git submodule update --init
```

### ant
1. Update project
    * In this directory
      ```bash
      android update project -p . -n podplayer -t android-10
      ```
    * In the libsrc/pulltorefresh/pulltorefresh directory
      ```bash
      android update project -p . -n pulltoupdate -t android-10
      ```
    * copy support lib 
    ```
    cp <android-sdk-path>/extras/android/support/v4/android-support-v4.jar libs
    ```
2. Build
      ```bash
      ant debug
      ```
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
1. update project
```bash
  cd ../
  android update test-project -m ../../ -n podtest -p .
```

### ant
```bash
  ant debug install
  ant test
```

### Eclipse
1. Import the test/podtest directory as an Eclipse projct
2. Run as "Android JUnit Test" or "Scirocco JUnit Test"

TODO
----
* add/mark listened item list
* use database to manage loaded episodes
    * display mark which is already played, new item etc...
    * to reduce reload of podcast
    * show description of playing episode
    * save podcast list and updated time as state
* move state to service
  * remove array adapter?
* use large notification
* http proxy setting?
* write additional podcast url to sd card?
* cache podcast icon
    * avoid loading podcast when UI is switched 
* fix bugs
    * fix memory leak (bitmap problem)
        * check heap size and class histogram
    * when prepare error occurs, cursor moves to next episode
        * stop playing
        * or mark error item and try playing next unerror item
* add / update test
    * preference
    * notification
    * gesture?
    * landscape UI
* define smoke test and full test. smoke test can be run with SmokeTestSuiteBuilder.
* display playing icon in group of expandable list
* improve UI to add podcast URL
    * update main activity when setting is changed
* change notification icon for Android4.0
* reset playing position after podcast selection is changed
* translate
    <string name="pref_episode_limit">Limit of episodes for each podcast</string>
    <string name="pref_episode_limit_title">Limit of episodes</string>
* optimize initialization of podplayer
    * setContentView takes long time
* Autoload: load when create activity or when podcast list setting is changed
* fix a bug that when only one episode is registered, list is in tap to refresh mode after loaded
* add confirm dialog to open web site
* play episode which is clicked while preparing other episode
* sort episode by date
* add episode search UI
* add cool widget to play episode
* add slider to show current playing position
* add error handling
* write user guide?
* check apk size
* MainActivityTest, PodcastActivity then testAbortReload blocks...
* add Wifi only mode
    * acquire [wifi lock](http://developer.android.com/reference/android/net/wifi/WifiManager.WifiLock.html)

License
----------
* podplayer: Copyright (c) 2012-2015 Takashi Masuyama. All rights reserved. 
Licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)

* podplayer uses the following software which is licensed under the 
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html) 
Copyright (c) 2011 [Johan Nilsson](http://markupartist.com) 
https://github.com/johannilsson/android-pulltorefresh

* podtest uses nbandroid-utils to save JUnit test result as XML
http://code.google.com/p/nbandroid-utils/wiki/InstrumentationTestRunner

Keywords
----------
Android, MediaPlayer, Podcast, AsyncTask, PullToRefresh, Gesture, JUnit, Robotium

----
Takashi Masuyama < mamewotoko@gmail.com >  
http://mamewo.ddo.jp/
