package com.mamewo.podplayer0;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.mamewo.podplayer0.PlayerService.PodInfo;

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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

//common activity + gesture
public class BasePodplayerActivity
	extends Activity
	implements OnSharedPreferenceChangeListener,
	OnGesturePerformedListener
{
	final static
	protected String DEFAULT_PODCAST_LIST = "http://www.nhk.or.jp/rj/podcast/rss/english.xml"
			+ "!http://feeds.voanews.com/ps/getRSS?client=Standard&PID=_veJ_N_q3IUpwj2Z5GBO2DYqWDEodojd&startIndex=1&endIndex=500"
			+ "!http://computersciencepodcast.com/compucast.rss!http://www.discovery.com/radio/xml/news.xml"
			+ "!http://downloads.bbc.co.uk/podcasts/worldservice/tae/rss.xml"
			+ "!http://feeds.wsjonline.com/wsj/podcast_wall_street_journal_this_morning?format=xml";
	final static
	protected boolean DEFAULT_USE_GESTURE = true;
	final static
	protected URL[] DUMMY_URL_LIST = new URL[0];
	protected String[] allTitles_;
	protected String[] allURLs_;
	//TODO: wait until player_ is not null (service is connected)
	protected PlayerService player_ = null;
	protected GestureLibrary gestureLib_;
	protected double gestureScoreThreshold_;
	protected BaseGetPodcastTask loadTask_;
	protected PodplayerState state_;
	protected Drawable[] iconData_;
	protected boolean finishServiceOnExit_;
	protected ServiceConnection connection_;
	protected boolean showPodcastIcon_;
	
	final static
	public String TAG = "podplayer";

	public void onCreate(Bundle savedInstanceState, ServiceConnection conn) {
		super.onCreate(savedInstanceState);
		Intent intent = new Intent(this, PlayerService.class);
		startService(intent);
		finishServiceOnExit_ = false;
		state_ = null;
		if(null != savedInstanceState){
			state_ = (PodplayerState) savedInstanceState.get("state");
		}
		if(null == state_){
			state_ = new PodplayerState();
		}
		connection_ = conn;
		//TODO: handle error
		bindService(intent, conn, Context.BIND_AUTO_CREATE);
		allTitles_ = getResources().getStringArray(R.array.pref_podcastlist_keys);
		allURLs_ = getResources().getStringArray(R.array.pref_podcastlist_urls);
		loadTask_ = null;
		state_.iconURLs_ = new URL[allTitles_.length];
		iconData_ = new Drawable[allTitles_.length];
		SharedPreferences pref=
				PreferenceManager.getDefaultSharedPreferences(this);
		pref.registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public void onDestroy() {
		SharedPreferences pref=
				PreferenceManager.getDefaultSharedPreferences(this);
		pref.unregisterOnSharedPreferenceChangeListener(this);
		if (null != loadTask_) {
			loadTask_.cancel(true);
		}
		iconData_ = null;
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
	public void onStart() {
		super.onStart();
		SharedPreferences pref=
				PreferenceManager.getDefaultSharedPreferences(this);
		syncPreference(pref, "ALL");
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
		loadTask_ = task;
		loadTask_.execute(state_.podcastURLList_.toArray(DUMMY_URL_LIST));
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

	private void syncPreference(SharedPreferences pref, String key){
		boolean updateAll = "ALL".equals(key);
		if(updateAll || "podcastlist".equals(key)) {
			String prefURLString = pref.getString("podcastlist", DEFAULT_PODCAST_LIST);
			String[] list = prefURLString.split(MultiListPreference.SEPARATOR);
			state_.podcastURLList_.clear();
			for (String url: list) {
				try {
					state_.podcastURLList_.add(new URL(url));
				}
				catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
		}
		if(updateAll || "enable_gesture".equals(key)) {
			boolean useGesture = pref.getBoolean("enable_gesture", DEFAULT_USE_GESTURE);
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
		if(updateAll || "gesture_score_threshold".equals(key)) {
			gestureScoreThreshold_ =
					Double.valueOf(pref.getString("gesture_score_threshold", "3.0"));
		}
		if(updateAll || "show_podcast_icon".equals(key)) {
			showPodcastIcon_ = pref.getBoolean("show_podcast_icon", true);
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
			showMessage("gesture with low score: " + p.score);
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
		protected List<PodInfo> loadedEpisode_;
		protected List<URL> podcastURLList_;
		protected String lastUpdated_;
		protected URL[] iconURLs_;

		private PodplayerState() {
			loadedEpisode_ = new ArrayList<PodInfo>();
			podcastURLList_ = new ArrayList<URL>();
			lastUpdated_ = "";
			iconURLs_ = null;
		}
	}
}
