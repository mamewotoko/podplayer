===============================================================================
podplayer - An android podcast player
===============================================================================

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

    git clone git://github.com/mamewotoko/podplayer.git
    git submodule update --init

### ant
1. Update project
    * In this directory

            android update project -p . -n podplayer -t android-10
    * In the libsrc/pulltorefresh/pulltorefresh directory

            android update project -p . -n pulltoupdate -t android-10
2. Build

    ant debug
A file bin/podplayer-debug.apk is created if succeed.

### Eclipse
1. Import this directory, whose name is podplayer.
2. Import the libsrc/pulltorefresh/pulltorefresh directory, whose name is pulltorefresh
3. Add project reference
    1. right click podplayer project -> properties. 
    2. select Project References. 
    3. check pulltorefresh. 
4. Run podplayer project as Android Application

How to run Scirocco test
-------------------------
1. Download robotium and scirocco jars
 
        cd test/podtest/libs
        sh setup.sh
2. update project

        cd ../
        android update test-project -m ../../ -n podtest -p .

### ant
    ant debug install
    ant test

### Eclipse
1. Import the test/podtest directory
2. Run as "Android JUnit Test" or "Scirocco JUnit Test"

TODO
----------
* fix memory leak (bitmap problem)
    * check heap size and class histogram
* improve UI to add podcast URL
    * save state of check box
    * update main activity when setting is changed
    * add swap order function?
* fix bugs
    * when prepare error occurs, cursor moves to next episode
        * show network error message
    * play icon is displayed incorrectly when one item is clicked while other item is being prepared
* display playing icon in group of expandable list
* reset playing position after podcast selection is changed
* use database to manage loaded episodes
    * display mark which is already played, new item etc...
    * to reduce reload of podcast
    * show description of playing episode
    * cache podcast icon
    * save podcast list and updated time as state
* support play previous item when KEYCODE_MEDIA_PREVIOUS button is pressed
* add test
    * preference
    * gesture?
    * switch UI
    * landscape UI
* optimize initialization of podplayer
    * setContentView takes long time
* Autoload: load on create & when podcast list setting is changed
* add close button to dialog
* fix a bug that when only one episode is registered, list is in tap to refresh mode after loaded
* use Google Reader or some other service to manage listened episode
* add confirm dialog to open web site
* play episode which is clicked while preparing other episode
* sort episode by date
* add episode search UI
* add cool widget to play episode
* add slider to show current playing position
* add Wifi only mode
* acquire [wifi lock](http://developer.android.com/reference/android/net/wifi/WifiManager.WifiLock.html)
* add error handling
* set default value of MultiListPreference by array name not by string value
* support video cast??
* write user guide?
* check apk size
* MainActivityTest, PodcastActivity then testAbortReload blocks...

License
----------
* podplayer: Copyright (c) 2012 Takashi Masuyama. All rights reserved. 
Licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)
* podplayer uses the following software which is licensed under the 
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html) 
Copyright (c) 2011 [Johan Nilsson](http://markupartist.com) 
https://github.com/johannilsson/android-pulltorefresh

Keywords
----------
Android, MediaPlayer, Podcast, AsyncTask, PullToRefresh, Gesture

----
Takashi Masuyama < mamewotoko@gmail.com >  
http://www002.upp.so-net.ne.jp/mamewo/
