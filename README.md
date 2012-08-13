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
    git submodule init
    git submodule update

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

How to test
-----------
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
* use database to manage loaded episodes
   * display mark which is already played, new item etc...
* add UI to add podcast URL
    * Web browser displays text/xml as content (I want intent...)
* optimize initialization of podplayer
    * setContentView takes long time
* show summary of preference item
    * gesture score
    * read timeout
* use expandable list
* add close button to dialog
* add table of gestures as preference
* fix a bug that when only one episode is registered, list is in tap to refresh mode after loaded
* save podcast list and updated time as state
* use Google Reader or some other service to manage listened episode
* add confirm dialog to open web site
* play episode which is clicked while preparing other item
* sort episode by date
* add episode search UI
* show description of playing episode
* add cool widget to play episode
* add slider to show current playing position
* add Wifi only mode
* acquire [wifi lock](http://developer.android.com/reference/android/net/wifi/WifiManager.WifiLock.html)
* add error handling
* set default value of MultiListPreference by array name not by string value
* support video cast??
* support native player?
* write user guide?
* check apk size

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
