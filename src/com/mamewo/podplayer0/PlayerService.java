package com.mamewo.podplayer0;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

/**
 * @author Takashi Masuyama <mamewotoko@gmail.com>
 * http://www002.upp.so-net.ne.jp/mamewo/
 */
public class PlayerService
	extends Service
{
	final static
	public String PACKAGE_NAME = PlayerService.class.getPackage().getName();
	final static
	public String STOP_MUSIC_ACTION = PACKAGE_NAME + ".STOP_MUSIC_ACTION";
	
	final static
	private String TAG = "podcast";
	private final IBinder binder_ = new LocalBinder();
	private ArrayList<URL> currentPlaylist_;
	private int playCursor_;
	private MediaPlayer player_;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		String action = intent.getAction();
		if (STOP_MUSIC_ACTION.equals(action)) {
			stopMusic();
		}
		return START_STICKY;
	}
	
	public void setPlaylist(ArrayList<URL> playlist) {
		currentPlaylist_ = playlist;
	}
	
	public boolean isPlaying() {
		return player_.isPlaying();
	}

	public void playNext() {
		if (isPlaying()) {
			stopMusic();
		}
		playCursor_ = (playCursor_ + 1) % currentPlaylist_.size();
		playMusic();
	}

	public class MusicCompletionListener
	implements MediaPlayer.OnCompletionListener,
				MediaPlayer.OnErrorListener
	{
		public void onCompletion(MediaPlayer mp) {
			playNext();
		}

		// This method is not called when DRM error occurs
		public boolean onError(MediaPlayer mp, int what, int extra) {
			//TODO: show error message to GUI
			Log.i(TAG, "onError is called, cannot play this media");
			//TODO: call playNext if error occurred while playing music
			playNext();
			return true;
		}
	}

	/**
	 * play given playlist from beginning.
	 * 
	 * @param playlist playlist to play
	 * @return true if playlist is played, false if it fails.
	 */
	public boolean playMusic(ArrayList<URL> playlist) {
		currentPlaylist_ = playlist;
		if(null == playlist){
			return false;
		}
		Log.d(TAG, "playMusic playlist: playMusic");
		playCursor_ = 0;
		return playMusic();
	}

	public boolean playMusic() {
		if (null == currentPlaylist_ || currentPlaylist_.isEmpty()) {
			Log.i(TAG, "playMusic: playlist is null");
			return false;
		}
		if (player_.isPlaying()) {
			return false;
		}
		URL path = currentPlaylist_.get(playCursor_);
		//skip unsupported files filtering by filename ...
		Log.i(TAG, "playMusic: " + path);
		try {
			player_.reset();
			player_.setDataSource(path.toString());
			//TODO: use async prepare
			player_.prepare();
			player_.start();
		}
		catch (IOException e) {
			return false;
		}
		return true;
	}

	public void stopMusic() {
		if(player_.isPlaying()){
			player_.stop();
		}
	}

	public void pauseMusic() {
		if(player_.isPlaying()){
			player_.pause();
		}
		//clearNotification();
	}
	
	public void quit(){
		stopSelf();
	}
	
	public class LocalBinder
		extends Binder
	{
		public PlayerService getService() {
			return PlayerService.this;
		}
	}
	
	@Override
	public void onCreate(){
		super.onCreate();
		currentPlaylist_ = null;
		player_ = new MediaPlayer();
		MusicCompletionListener l = new MusicCompletionListener();
		player_.setOnCompletionListener(l);
		player_.setOnErrorListener(l);
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return binder_;
	}
}
