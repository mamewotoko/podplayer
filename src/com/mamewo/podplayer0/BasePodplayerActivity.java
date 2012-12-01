package com.mamewo.podplayer0;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import com.mamewo.podplayer0.PlayerService.MusicInfo;

import android.media.AudioManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

//common activity + gesture
abstract public class BasePodplayerActivity
	extends Activity
	implements OnSharedPreferenceChangeListener,
	OnGesturePerformedListener
{
	final static
	protected PodcastInfo[] DUMMY_INFO_LIST = new PodcastInfo[0];
	protected PlayerService player_ = null;
	protected GestureLibrary gestureLib_;
	protected double gestureScoreThreshold_;
	protected BaseGetPodcastTask loadTask_;
	protected PodplayerState state_;
	protected boolean finishServiceOnExit_;
	protected ServiceConnection connection_;
	protected boolean showPodcastIcon_;
	private boolean uiSettingChanged_;

	//TODO: add preference
	// 10 Mbyteq
	static final
	private long HTTP_CACHE_SIZE = 10 * 1024 * 1024;
	private File httpCacheDir_;

	final static
	public String TAG = "podplayer";

	abstract protected void onPodcastListChanged(boolean start);
	Object cacheObject_ = null;
	static final public int CACHERESPONSE_API_LEVEL = 13;

	public void onCreate(Bundle savedInstanceState, ServiceConnection conn, Class<?> userClass) {
		super.onCreate(savedInstanceState);
		Intent intent = new Intent(this, PlayerService.class);
		startService(intent);
		finishServiceOnExit_ = false;
		state_ = null;
		uiSettingChanged_ = false;
		
		if(null != savedInstanceState){
			state_ = (PodplayerState) savedInstanceState.get("state");
		}
		if(null == state_){
			state_ = new PodplayerState();
		}
		connection_ = conn;
		//TODO: handle error
		bindService(intent, conn, Context.BIND_AUTO_CREATE);
		loadTask_ = null;
		SharedPreferences pref=
				PreferenceManager.getDefaultSharedPreferences(this);
		pref.registerOnSharedPreferenceChangeListener(this);
		httpCacheDir_ = null;
		cacheObject_ = null;
	}

	private Object enableHttpResponseCache(File cacheDir) {
		try {
			return Class.forName("android.net.http.HttpResponseCache")
				.getMethod("install", File.class, long.class)
				.invoke(null, cacheDir, HTTP_CACHE_SIZE);
		} catch (Exception e) {
			//nop
		}
		return null;
	}
	
	private boolean disableHttpResponseCache(Object cacheObj){
		if(null == cacheObj){
			return false;
		}
		try {
			Class.forName("android.net.http.HttpResponseCache")
				.getMethod("close")
				.invoke(cacheObj);
			return true;
		} catch (Exception e) {
			//nop
		}
		return false;
	}

	@Override
	public void onDestroy() {
		for (PodcastInfo info : state_.podcastList_) {
			if (null != info.icon_) {
				Bitmap bitmap = info.icon_.getBitmap();
				bitmap.recycle();
			}
		}
		SharedPreferences pref =
				PreferenceManager.getDefaultSharedPreferences(this);
		pref.unregisterOnSharedPreferenceChangeListener(this);
		if (null != loadTask_) {
			loadTask_.cancel(true);
		}
		boolean playing = player_.isPlaying();
		if(finishServiceOnExit_ && playing) {
			player_.stopMusic();
		}
		unbindService(connection_);
		if (finishServiceOnExit_ || ! playing) {
			Intent intent = new Intent(this, PlayerService.class);
			stopService(intent);
		}
		super.onDestroy();
	}

	@Override
	protected void onStart() {
		super.onStart();
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		//TODO: check current activity and preference
		if (uiSettingChanged_) {
			Intent intent = new Intent(this, MainActivity.class);
			//TODO: add flag of restart
			startActivity(intent);
			finish();
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putSerializable("state", state_);
	}

	public void updatePlaylist() {
		player_.setPlaylist(state_.loadedEpisode_);
	}

	public boolean isLoading() {
		return (null != loadTask_ && loadTask_.getStatus() == AsyncTask.Status.RUNNING);
	}
	
	public void startLoading(BaseGetPodcastTask task) {
		if (isLoading()) {
			Log.d(TAG, "startLoading: already loading");
			return;
		}
		state_.loadedEpisode_.clear();
		loadTask_ = task;
		loadTask_.execute(state_.podcastList_.toArray(DUMMY_INFO_LIST));
	}

	public void showMessage(String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mainmenu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean handled = false;
		switch(item.getItemId()) {
		case R.id.exit_menu:
			finishServiceOnExit_ = true;
			finish();
			handled = true;
			break;
		case R.id.pref_menu:
			startActivity(new Intent(this, PodplayerPreference.class));
			handled = true;
			break;
		default:
			break;
		}
		return handled;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
		Log.d(TAG, "onSharedPreferneceChanged: " + key);
		syncPreference(pref, key);
	}
	
	protected void syncPreference(SharedPreferences pref, String key){
		Log.d(TAG, "syncPreference: " + key);
		boolean updateAll = "ALL".equals(key);
		if ("use_expandable_ui".equals(key)) {
			uiSettingChanged_ = true;
		}
		if (updateAll || "enable_gesture".equals(key)) {
			boolean useGesture = pref.getBoolean("enable_gesture", PodplayerPreference.DEFAULT_USE_GESTURE);
			GestureOverlayView gestureView =
					(GestureOverlayView)findViewById(R.id.gesture_view);
			if(useGesture) {
				gestureLib_ = GestureLibraries.fromRawResource(this, R.raw.gestures);
				if(! gestureLib_.load()){
					Log.i(TAG, "gesture load failed");
				}
				gestureView.addOnGesturePerformedListener(this);
			}
			else {
				gestureView.removeOnGesturePerformedListener(this);
				gestureLib_ = null;
			}
			gestureView.setEnabled(useGesture);
		}
		if (updateAll || "gesture_score_threshold".equals(key)) {
			gestureScoreThreshold_ =
					Double.valueOf(pref.getString("gesture_score_threshold", PodplayerPreference.DEFAULT_GESTURE_SCORE));
		}
		if (updateAll || "show_podcast_icon".equals(key)) {
			showPodcastIcon_ = pref.getBoolean("show_podcast_icon", PodplayerPreference.DEFAULT_SHOW_ICON);
		}
		if (updateAll || "use_reponse_cache".equals(key)){
			boolean useCache = pref.getBoolean("use_reponse_cache", PodplayerPreference.DEFAULT_USE_RESPONSE_CACHE);
			if(useCache){
				if(null == httpCacheDir_){
					httpCacheDir_ = new File(getCacheDir(), "http");
				}
				cacheObject_ = enableHttpResponseCache(httpCacheDir_);
			}
			else {
				disableHttpResponseCache(cacheObject_);
			}
		}
		if("clear_response_cache".equals(key)){
			if(null != cacheObject_){
				try{
					Log.d(TAG, "clear cache");
					Class.forName("android.net.http.HttpResponseCache")
						.getMethod("delete")
						.invoke(cacheObject_);
				}
				catch(Exception e){
					Log.d(TAG, "cache delete method", e);
				}
			}
		}
		//following block should be last one of this function
		if (updateAll || "podcastlist2".equals(key)) {
			state_.podcastList_ = PodcastListPreference.loadSetting(this);
			onPodcastListChanged(updateAll);
		}
	}

	@Override
	public void onGesturePerformed(GestureOverlayView view, Gesture gesture) {
		ArrayList<Prediction> predictions = gestureLib_.recognize(gesture);
		if(predictions.size() == 0){
			showMessage("unknown gesture");
			return;
		}
		//predictions is sorted by score
		Prediction p = predictions.get(0);
		if(p.score < gestureScoreThreshold_) {
			showMessage(String.format("gesture with low score: %.2f", p.score));
			return;
		}
		if("next".equals(p.name)) {
			player_.playNext();
		}
		else if("play".equals(p.name)) {
			updatePlaylist();
			if(! player_.restartMusic()) {
				player_.playMusic();
			}
		}
		else if("pause".equals(p.name)) {
			player_.pauseMusic();
		}
		else if("back".equals(p.name)) {
			player_.stopMusic();
			player_.playMusic();
		}
		showMessage(p.name);
	}

	final public static
	class PodplayerState
		implements Serializable
	{
		private static final long serialVersionUID = 1L;
		protected List<MusicInfo> loadedEpisode_;
		protected List<PodcastInfo> podcastList_;
		protected String lastUpdated_;

		private PodplayerState() {
			loadedEpisode_ = new ArrayList<MusicInfo>();
			podcastList_ = new ArrayList<PodcastInfo>();
			lastUpdated_ = "";
		}
	}
}
