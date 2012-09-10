package com.mamewo.podplayer0;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;


import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;

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
	public String MEDIA_BUTTON_ACTION = PACKAGE_NAME + ".MEDIA_BUTTON_ACTION";
	final static
	public int STOP = 1;
	final static
	public int PAUSE = 2;
	final static
	private int PREV_INTERVAL_MILLIS = 3000;
	final static
	private Class<MainActivity> USER_CLASS = MainActivity.class;
	final static
	private String TAG = "podplayer";
	final static
	private int NOTIFY_PLAYING_ID = 1;
	private final IBinder binder_ = new LocalBinder();
	private List<MusicInfo> currentPlaylist_;
	private int playCursor_;
	private MediaPlayer player_;
	private PlayerStateListener listener_;
	private Receiver receiver_;
	private boolean isPreparing_;
	private boolean stopOnPrepared_;
	private boolean isPausing_;
	private ComponentName mediaButtonReceiver_;
	private long previousPrevKeyTime_;

	//TODO: check
	static
	public boolean isNetworkConnected(Context context) {
		ConnectivityManager connMgr =
				(ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		return (networkInfo != null && networkInfo.isConnected());
	}

	public void setPlaylist(List<MusicInfo> playlist) {
		currentPlaylist_ = playlist;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		String action = intent.getAction();
		Log.d(TAG, "onStartCommand: " + action);
		if (STOP_MUSIC_ACTION.equals(action)) {
			stopMusic();
		}
		else if (JACK_UNPLUGGED_ACTION.equals(action)) {
			SharedPreferences pref =
					PreferenceManager.getDefaultSharedPreferences(this);
			boolean pause = pref.getBoolean("pause_on_unplugged", true);
			if (pause && player_.isPlaying()) {
				pauseMusic();
			}
		}
		else if (MEDIA_BUTTON_ACTION.equals(action)) {
			KeyEvent event = intent.getParcelableExtra("event");
			Log.d(TAG, "SERVICE: Received media button: " + event.getKeyCode());
			if (event.getAction() != KeyEvent.ACTION_UP) {
				return START_STICKY;
			}
			switch(event.getKeyCode()) {
			case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
				if (player_.isPlaying()){
					pauseMusic();
				}
				else {
					playMusic();
				}
				break;
			case KeyEvent.KEYCODE_MEDIA_NEXT:
				if (player_.isPlaying()) {
					playNext();
				}
				break;
			case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
				long currentTime = System.currentTimeMillis();
				Log.d(TAG, "Interval: " + (currentTime - previousPrevKeyTime_));
				if ((currentTime - previousPrevKeyTime_) <= PREV_INTERVAL_MILLIS) {
					if(0 == playCursor_){
						playCursor_ = currentPlaylist_.size() - 1;
					}
					else {
						playCursor_--;
					}
					playNth(playCursor_);
				}
				else {
					//rewind
					playMusic();
				}
				previousPrevKeyTime_ = currentTime;
				break;
			default:
				break;
			}
		}
		return START_STICKY;
	}
	
	public boolean isPlaying() {
		return (! stopOnPrepared_) && (isPreparing_ || player_.isPlaying());
	}

	public List<MusicInfo> getCurrentPlaylist() {
		return currentPlaylist_;
	}
	
	/**
	 * get current playing or pausing music
	 * @return current music info
	 */
	public MusicInfo getCurrentPodInfo(){
		if(null == currentPlaylist_ || playCursor_ >= currentPlaylist_.size()){
			return null;
		}
		return currentPlaylist_.get(playCursor_);
	}
	
	public boolean playNext() {
		Log.d(TAG, "playNext");
		if(currentPlaylist_ == null || currentPlaylist_.size() == 0) {
			return false;
		}
		if (player_.isPlaying()) {
			player_.pause();
		}
		playCursor_ = (playCursor_ + 1) % currentPlaylist_.size();
		return playMusic();
	}

	/**
	 * plays music of given index
	 * @param pos index on currentPlayList_
	 * @return true if succeed
	 */
	public boolean playNth(int pos) {
		if(currentPlaylist_ == null || currentPlaylist_.size() == 0){
			return false;
		}
		if(isPreparing_){
			return false;
		}
		isPausing_ = false;
		playCursor_ = pos % currentPlaylist_.size();
		return playMusic();
	}

	/**
	 * start music from paused position
	 * @return true if succeed
	 */
	public boolean restartMusic() {
		Log.d(TAG, "restartMusic: " + isPausing_);
		if(! isPausing_) {
			return false;
		}
		if(isPreparing_) {
			stopOnPrepared_ = false;
			return true;
		}
		player_.start();
		MusicInfo info = currentPlaylist_.get(playCursor_);
		if(null != listener_){
			listener_.onStartMusic(currentPlaylist_.get(playCursor_));
		}
		startForeground("Playing podcast", info.title_);
		return true;
	}

	/**
	 * play current music from beginning
	 * @return true if succeed
	 */
	public boolean playMusic() {
		if (isPreparing_) {
			Log.d(TAG, "playMusic: preparing");
			stopOnPrepared_ = false;
			return false;
		}
		if (null == currentPlaylist_ || currentPlaylist_.isEmpty()) {
			Log.i(TAG, "playMusic: playlist is null");
			return false;
		}
		MusicInfo info = currentPlaylist_.get(playCursor_);
		Log.d(TAG, "playMusic: " + playCursor_ + ": " + info.url_);
		try {
			player_.reset();
			player_.setDataSource(info.url_);
			player_.prepareAsync();
			isPreparing_ = true;
			isPausing_ = false;
		}
		catch (IOException e) {
			return false;
		}
		if(null != listener_){
			listener_.onStartLoadingMusic(info);
		}
		startForeground(getString(R.string.notify_playing_podcast), info.title_);
		return true;
	}
	
	public void stopMusic() {
		if (isPreparing_) {
			stopOnPrepared_ = true;
		}
		else if(player_.isPlaying()){
			player_.stop();
		}
		isPausing_ = false;
		stopForeground(true);
		if(null != listener_){
			Log.d(TAG, "call onStopMusic");
			listener_.onStopMusic(STOP);
		}
	}

	//TODO: correct paused state
	public void pauseMusic() {
		Log.d(TAG, "pauseMusic: " + player_.isPlaying());
		if (isPreparing_) {
			stopOnPrepared_ = true;
		}
		else if(player_.isPlaying()){
			player_.pause();
		}
		isPausing_ = true;
		stopForeground(true);
		if(null != listener_){
			listener_.onStopMusic(PAUSE);
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
		registerReceiver(receiver_, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
		isPreparing_ = false;
		stopOnPrepared_ = false;
		isPausing_ = false;
		playCursor_ = 0;
		previousPrevKeyTime_ = 0;
		AudioManager manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		mediaButtonReceiver_ = new ComponentName(getPackageName(), Receiver.class.getName());
		manager.registerMediaButtonEventReceiver(mediaButtonReceiver_);
	}
	
	@Override
	public void onDestroy() {
		AudioManager manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		manager.unregisterMediaButtonEventReceiver(mediaButtonReceiver_);
		unregisterReceiver(receiver_);
		stopForeground(false);
		player_.setOnCompletionListener(null);
		player_.setOnErrorListener(null);
		player_.setOnPreparedListener(null);
		player_ = null;
		listener_ = null;
		currentPlaylist_ = null;
		super.onDestroy();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return binder_;
	}

	private void startForeground(String title, String description) {
		String podTitle = getString(R.string.notify_playing_podcast);
		Notification note =
				new Notification(R.drawable.ic_launcher, podTitle, 0);
		Intent ni = new Intent(this, USER_CLASS);
		PendingIntent npi = PendingIntent.getActivity(this, 0, ni, 0);
		note.setLatestEventInfo(this, title, description, npi);
		startForeground(NOTIFY_PLAYING_ID, note);
	}

	@Override
	public void onPrepared(MediaPlayer player) {
		Log.d(TAG, "onPrepared");
		isPreparing_ = false;
		if(stopOnPrepared_) {
			Log.d(TAG, "onPrepared aborted");
			stopOnPrepared_ = false;
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
		MusicInfo info = currentPlaylist_.get(playCursor_);
		Log.i(TAG, "onError: what: " + what + " extra: " + extra + " url: " + info.url_);
		stopMusic();
		if (isNetworkConnected(this)) {
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
		public void onStartLoadingMusic(MusicInfo info);
		public void onStartMusic(MusicInfo info);
		public void onStopMusic(int mode);
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
					//unplugged
					Intent i = new Intent(context, PlayerService.class);
					i.setAction(JACK_UNPLUGGED_ACTION);
					context.startService(i);
				}
			}
			else if(Intent.ACTION_MEDIA_BUTTON.equals(action)) {
				Log.d(TAG, "media button");
				Intent i = new Intent(context, PlayerService.class);
				i.setAction(MEDIA_BUTTON_ACTION);
				i.putExtra("event", intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT));
				context.startService(i);
			}
		}
	}

	static
	public class MusicInfo
		implements Serializable
	{
		private static final long serialVersionUID = 1L;
		final public String url_;
		final public String title_;
		final public String pubdate_;
		final public String link_;
		final public int index_;

		public MusicInfo(String url, String title, String pubdate, String link, int index) {
			url_ = url;
			title_ = title;
			pubdate_ = pubdate;
			link_ = link;
			index_ = index;
		}
	}
}