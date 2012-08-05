package com.mamewo.podplayer0;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;


import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
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
	public String JACK_UNPLUGGED_ACTION = PACKAGE_NAME + ".JUCK_UNPLUGGED_ACTION";
	final static
	private Class<PodplayerActivity> userClass_ = PodplayerActivity.class;
	final static
	private String TAG = "podplayer";
	final static
	private int NOTIFY_PLAYING_ID = 1;
	private final IBinder binder_ = new LocalBinder();
	private List<PodInfo> currentPlaylist_;
	private int playCursor_;
	private MediaPlayer player_;
	private PlayerStateListener listener_;
	private Receiver receiver_;
	private boolean isPreparing_;
	private boolean abortPreparing_;
	
	static
	public boolean isNetworkConnected(Context context) {
		ConnectivityManager connMgr =
				(ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		return (networkInfo != null && networkInfo.isConnected());
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		String action = intent.getAction();
		Log.d(TAG, "onStartCommand: " + action);
		if (STOP_MUSIC_ACTION.equals(action)) {
			stopMusic();
		}
		else if(JACK_UNPLUGGED_ACTION.equals(action)) {
			SharedPreferences pref =
					PreferenceManager.getDefaultSharedPreferences(this);
			boolean pause = pref.getBoolean("pause_on_unplugged", true);
			if (pause) {
				pauseMusic();
				//TODO: implement to start continuation (playMusic)
			}
		}
		return START_STICKY;
	}
	
	public boolean isPlaying() {
		return (! abortPreparing_) && (isPreparing_ || player_.isPlaying());
	}

	public boolean playNext() {
		if (isPlaying()) {
			stopMusic();
		}
		playCursor_ = (playCursor_ + 1) % currentPlaylist_.size();
		return playMusic();
	}

	public void setPlaylist(List<PodInfo> playlist) {
		currentPlaylist_ = playlist;
	}

	public boolean playNth(int pos) {
		if(currentPlaylist_.size() == 0) {
			return false;
		}
		playCursor_ = pos % currentPlaylist_.size();
		return playMusic();
	}

	public boolean playMusic() {
		if (isPreparing_) {
			return false;
		}
		if (null == currentPlaylist_ || currentPlaylist_.isEmpty()) {
			Log.i(TAG, "playMusic: playlist is null");
			return false;
		}
		PodInfo info = currentPlaylist_.get(playCursor_);
		//skip unsupported files filtering by filename ...
		Log.i(TAG, "playMusic: " + info.url_);
		try {
			player_.reset();
			player_.setDataSource(info.url_);
			player_.prepareAsync();
			isPreparing_ = true;
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
		if (isPreparing_) {
			abortPreparing_ = true;
		}
		else if(player_.isPlaying()){
			player_.stop();
		}
		stopForeground(true);
		if(null != listener_){
			Log.d(TAG, "call onStopMusic");
			listener_.onStopMusic();
		}
	}

	public void pauseMusic() {
		if (isPreparing_) {
			abortPreparing_ = true;
		}
		else if(player_.isPlaying()){
			player_.pause();
		}
		stopForeground(true);
		if(null != listener_){
			listener_.onStopMusic();
		}
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
		receiver_ = new Receiver();
		registerReceiver(receiver_,
						new IntentFilter(Intent.ACTION_HEADSET_PLUG));
		isPreparing_ = false;
		abortPreparing_ = false;
	}
	
	@Override
	public void onDestroy() {
		unregisterReceiver(receiver_);
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

	static
	public class PodInfo
		implements Serializable
	{
		final public String url_;
		final public String title_;
		final public String pubdate_;
		final public String link_;
		final public int index_;

		public PodInfo(String url, String title, String pubdate, String link, int index) {
			url_ = url;
			title_ = title;
			pubdate_ = pubdate;
			link_ = link;
			index_ = index;
		}
	}

	@Override
	public void onPrepared(MediaPlayer player) {
		Log.d(TAG, "onPrepared");
		isPreparing_ = false;
		if(abortPreparing_) {
			Log.d(TAG, "onPrepared aborted");
			abortPreparing_ = false;
			return;
		}
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
		PodInfo info = currentPlaylist_.get(playCursor_);
		Log.i(TAG, "onError: what: " + what + " extra: " + extra + " url: " + info.url_);
		if(info.url_.startsWith("http:") && ! isNetworkConnected(this)) {
			stopMusic();
		}
		else {
			playNext();
		}
		return true;
	}

	//use intent instead?
	public void setOnStartMusicListener(PlayerStateListener listener) {
		listener_ = listener;
	}
	public void clearOnStartMusicListener() {
		listener_ = null;
	}
	
	public interface PlayerStateListener {
		public void onStartLoadingMusic(PodInfo info);
		public void onStartMusic(PodInfo info);
		public void onStopMusic();
	}
	
	final static
	public class Receiver
		extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "onReceive is called");
			String action = intent.getAction();
			if (null == action) {
				return;
			}
			if(Intent.ACTION_HEADSET_PLUG.equals(action)) {
				if(intent.getIntExtra("state", 1) == 0) {
					Log.d(TAG, "unplugged");
					//unplugged
					Intent i = new Intent(context, PlayerService.class);
					i.setAction(JACK_UNPLUGGED_ACTION);
					context.startService(i);
				}
			}
		}
	}
}