===============================================================================
podplayer - a simple android podcast player
===============================================================================

What?
----------
<img src="https://github.com/mamewotoko/podplayer/raw/pullupdate/res/drawable-hdpi/ic_launcher.png" width="80" height="80">
A simple podcast player android application.

Screenshot
----------
![main screen](https://github.com/mamewotoko/podplayer/raw/pullupdate/doc/mainscreen.png)    
![preference](https://github.com/mamewotoko/podplayer/raw/pullupdate/doc/preference.png)

Google Play
------------
<img src="https://github.com/mamewotoko/podplayer/raw/pullupdate/res/drawable/qr.png" width="86" height="86>

https://play.google.com/store/apps/details?id=com.mamewo.podplayer0

How to build
------------
1. Clone source
2. Update project
    * In this directory

            android update project -p . -n podplayer -t android-10
    * In the libsrc/pulltorefresh/pulltorefresh directory

            android update project -p . -n pulltoupdate -t android-10

### ant
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
* fix a bug that when only one episode is registered, list is in tap to refresh mode after loaded
* add UI to add podcast URL
    * Web browser displays text/xml as content (I want intent...)
* support native player
* save podcast list and updated time as state
* use Google Reader to manage listened episode
* add confirm dialog to open web site
* play episode which is clicked while preparing other item
* sort episode by date
* add episode search UI
* show description of playing episode
* add cool widget to play episode
* implement play after pause
* add slider to show current playing position
* add Wifi only mode
* acquire [wifi lock](http://developer.android.com/reference/android/net/wifi/WifiManager.WifiLock.html)
* add error handling
* display mark which is already played, new item etc...
* display icon of each podcast
* add link to episode list
* set default value of MultiListPreference by array name not by string value
* support video cast??
* write user guide?

License
----------
* podplayer: Copyright (c) 2012 Takashi Masuyama. All rights reserved. 
Licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)
* podplayer uses the following software which is licensed under the 
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html) 
Copyright (c) 2011 [Johan Nilsson](http://markupartist.com) 
https://github.com/johannilsson/android-pulltorefresh

----
Takashi Masuyama < mamewotoko@gmail.com >  
http://www002.upp.so-net.ne.jp/mamewo/
