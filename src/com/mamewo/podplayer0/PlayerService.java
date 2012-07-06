package com.mamewo.podplayer0;

import java.io.IOException;
import java.util.List;

import android.app.Notification;
import android.app.PendingIntent;
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
	implements MediaPlayer.OnCompletionListener,
	MediaPlayer.OnErrorListener,
	MediaPlayer.OnPreparedListener
{
	final static
	public String PACKAGE_NAME = PlayerService.class.getPackage().getName();
	final static
	public String STOP_MUSIC_ACTION = PACKAGE_NAME + ".STOP_MUSIC_ACTION";
	final static
	private Class<PodplayerActivity> userClass_ = PodplayerActivity.class;
	final static
	private String TAG = "podcast";
	final static
	private int NOTIFY_PLAYING_ID = 1;
	private final IBinder binder_ = new LocalBinder();
	private List<PodInfo> currentPlaylist_;
	private int playCursor_;
	private MediaPlayer player_;
	private PlayerStateListener listener_;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		String action = intent.getAction();
		if (STOP_MUSIC_ACTION.equals(action)) {
			stopMusic();
		}
		return START_STICKY;
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

	public void setPlaylist(List<PodInfo> playlist) {
		currentPlaylist_ = playlist;
	}

	public boolean playNth(int pos) {
		playCursor_ = pos % currentPlaylist_.size();
		return playMusic();
	}

	public boolean playMusic() {
		if (null == currentPlaylist_ || currentPlaylist_.isEmpty()) {
			Log.i(TAG, "playMusic: playlist is null");
			return false;
		}
		if (player_.isPlaying()) {
			stopMusic();
		}
		PodInfo info = currentPlaylist_.get(playCursor_);
		//skip unsupported files filtering by filename ...
		Log.i(TAG, "playMusic: " + info.url_);
		try {
			player_.reset();
			player_.setDataSource(info.url_);
			player_.prepareAsync();
			if(null != listener_){
				listener_.onStartLoadingMusic(info);
			}
		}
		catch (IOException e) {
			return false;
		}
		//TODO: localize
		startForeground("Playing podcast", info.title_);
		return true;
	}

	public void startForeground(String title, String description) {
		//TODO: localize
		String podTitle = "playing podcast";
		Notification note =
				new Notification(R.drawable.ic_launcher, podTitle, 0);
		Intent ni = new Intent(this, userClass_);
		PendingIntent npi = PendingIntent.getActivity(this, 0, ni, 0);
		//TODO: localize
		note.setLatestEventInfo(this, title, description, npi);
		startForeground(NOTIFY_PLAYING_ID, note);
	}
	
	public void stopMusic() {
		if(player_.isPlaying()){
			player_.stop();
		}
		stopForeground(true);
	}

	public void pauseMusic() {
		if(player_.isPlaying()){
			player_.pause();
		}
		stopForeground(true);
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
		listener_ = null;
		player_ = new MediaPlayer();
		player_.setOnCompletionListener(this);
		player_.setOnErrorListener(this);
		player_.setOnPreparedListener(this);
	}
	
	@Override
	public void onDestroy() {
		stopForeground(false);
		player_ = null;
		listener_ = null;
		currentPlaylist_ = null;
		super.onDestroy();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return binder_;
	}

	public static class PodInfo {
		public String url_;
		public String title_;
		public String pubdate_;
		public PodInfo(String url, String title, String pubdate) {
			url_ = url;
			title_ = title;
			pubdate_ = pubdate;
		}
	}

	@Override
	public void onPrepared(MediaPlayer player) {
		Log.d(TAG, "onPrepared");
		player_.start();
		if(null != listener_){
			listener_.onStartMusic(currentPlaylist_.get(playCursor_));
		}
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		playNext();
	}

	// This method is not called when DRM error occurs
	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		//TODO: show error message to GUI
		Log.i(TAG, "onError is called, cannot play this media");
		//TODO: call playNext if error occurred while playing music
		playNext();
		return true;
	}

	//use intent instead?
	public void setOnStartMusicListener(PlayerStateListener listener) {
		listener_ = listener;
	}
	
	public interface PlayerStateListener {
		public void onStartLoadingMusic(PodInfo info);
		public void onStartMusic(PodInfo info);
	}
}