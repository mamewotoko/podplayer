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
https://play.google.com/store/apps/details?id=com.mamewo.podplayer0

How to build
------------
1. clone source
2. update project
    * In this directory

     `android update project -p . -n podplayer -t android-10`
    * In the libsrc/pulltorefresh/pulltorefresh directory

     `android update project -p . -n pulltoupdate -t android-10`
### ant

    ant debug
A file bin/podplayer-debug.apk is created if succeed.

### Eclipse
1. import this directory, whose name is podplayer.
2. import the libsrc/pulltorefresh/pulltorefresh directory, whose name is pulltorefresh
3. add project reference
    1. right click podplayer project -> properties. 
    2. select Project References. 
    3. check pulltorefresh. 
4. run podplayer as Android Application

TODO
----------
* play episode which is clicked while preparing other item
* set proxy of MediaPlayer if any
* show proxy setting as summary of preference
* sort episode by date
* add episode search UI
* show description of playing episode
* add cool widget to play episode
* implement play after pause
* add slider to show current playing position
* acquire wifi lock?
http://developer.android.com/guide/topics/media/mediaplayer.html
* add error handling
* add UI to add podcast URL
    * Web browser displays text/xml as content (I want intent...)
* display mark which is already played, new item etc...
* show web page of podcast
* display site icon of podcast
* add link to episode list
* set default value of MultiListPreference by array name not by string value
* add test

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
