package com.mamewo.podplayer0;

/**
 * @author Takashi Masuyama <mamewotoko@gmail.com>
 * http://www002.upp.so-net.ne.jp/mamewo/
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.ListActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.gesture.Prediction;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.mamewo.podplayer0.PlayerService.PodInfo;
import com.markupartist.android.widget.PullToRefreshListView;

public class PodplayerActivity
	extends Activity
	implements OnClickListener,
	ServiceConnection,
	OnItemClickListener,
	OnItemLongClickListener,
	OnItemSelectedListener,
	PlayerService.PlayerStateListener,
	OnSharedPreferenceChangeListener,
	PullToRefreshListView.OnRefreshListener,
	PullToRefreshListView.OnCancelListener,
	OnGesturePerformedListener
{
	private PodplayerState state_;
	private ToggleButton playButton_;
	private Spinner selector_;
	private PullToRefreshListView episodeList_;
	private ArrayAdapter<PodInfo> adapter_;
	//TODO: wait until player_ is not null (service is connected)
	private PlayerService player_ = null;
	private boolean finishServiceOnExit = false;
	private GetPodcastTask loadTask_;
	private GestureLibrary gestureLib_;
	private double gestureScoreThreshold_;
	private Drawable[] iconData_;
	private boolean showPodcastIcon_;
	private String[] allTitles_;
	private String[] allURLs_;
	
	final static
	private String DEFAULT_PODCAST_LIST = "http://www.nhk.or.jp/rj/podcast/rss/english.xml"
			+ "!http://feeds.voanews.com/ps/getRSS?client=Standard&PID=_veJ_N_q3IUpwj2Z5GBO2DYqWDEodojd&startIndex=1&endIndex=500"
			+ "!http://computersciencepodcast.com/compucast.rss!http://www.discovery.com/radio/xml/news.xml"
			+ "!http://downloads.bbc.co.uk/podcasts/worldservice/tae/rss.xml"
			+ "!http://feeds.wsjonline.com/wsj/podcast_wall_street_journal_this_morning?format=xml";
	final static
	private boolean DEFAULT_USE_GESTURE = true;
	final static
	private URL[] DUMMY_URL_LIST = new URL[0];

	final static
	private String TAG = "podplayer";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		state_ = null;
		if(null != savedInstanceState){
			state_ = (PodplayerState) savedInstanceState.get("state");
		}
		if(null == state_){
			state_ = new PodplayerState();
		}
		loadTask_ = null;
		playButton_ = (ToggleButton) findViewById(R.id.play_button);
		playButton_.setOnClickListener(this);
		playButton_.setEnabled(false);
		selector_ = (Spinner) findViewById(R.id.podcast_selector);
		selector_.setOnItemSelectedListener(this);
		episodeList_ = (PullToRefreshListView) findViewById(R.id.list);
		episodeList_.setOnItemClickListener(this);
		episodeList_.setOnItemLongClickListener(this);
		episodeList_.setOnRefreshListener(this);
		episodeList_.setOnCancelListener(this);
		adapter_ = new EpisodeAdapter(this);
		episodeList_.setAdapter(adapter_);

		Intent intent = new Intent(this, PlayerService.class);
		startService(intent);
		boolean result = bindService(intent, this, Context.BIND_AUTO_CREATE);
		Log.d(TAG, "bindService: " + result);
		SharedPreferences pref=
				PreferenceManager.getDefaultSharedPreferences(this);
		pref.registerOnSharedPreferenceChangeListener(this);
		gestureLib_ = null;
		gestureScoreThreshold_ = 0.0;
		allTitles_ = getResources().getStringArray(R.array.pref_podcastlist_keys);
		allURLs_ = getResources().getStringArray(R.array.pref_podcastlist_urls);
		//TODO: refactor
		iconData_ = new Drawable[allTitles_.length];
		state_.iconURLs_ = new URL[allTitles_.length];
	}

	@Override
	public void onDestroy(){
		if (null != loadTask_) {
			loadTask_.cancel(true);
		}
		SharedPreferences pref=
				PreferenceManager.getDefaultSharedPreferences(this);
		pref.unregisterOnSharedPreferenceChangeListener(this);
		boolean playing = player_.isPlaying();
		iconData_ = null;
		if(finishServiceOnExit && playing) {
			player_.stopMusic();
		}
		unbindService(this);
		if (finishServiceOnExit || ! playing) {
			Intent intent = new Intent(this, PlayerService.class);
			stopService(intent);
		}
		super.onDestroy();
	}

	//TODO: fetch current playing episode to update currentPodInfo
	@Override
	public void onResume(){
		super.onResume();
		SharedPreferences pref=
				PreferenceManager.getDefaultSharedPreferences(this);
		syncPreference(pref, "ALL");
		List<String> list = new ArrayList<String>();
		list.add("All");
		//stop loading?
		int j = 0;
		for(int i = 0; i < state_.podcastURLList_.size(); i++) {
			String podcastURL = state_.podcastURLList_.get(i).toString();
			for ( ; j < allURLs_.length; j++) {
				if(podcastURL.equals(allURLs_[j])) {
					list.add(allTitles_[j++]);
					break;
				}
			}
		}
		ArrayAdapter<String> adapter =
				new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
		//TODO: load if selected item is changed
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		selector_.setAdapter(adapter);
		boolean doLoad = pref.getBoolean("load_on_start", true);
		updateUI();
		if(doLoad && adapter_.getCount() == 0){
			episodeList_.startRefresh();
		}
		else {
			episodeList_.onRefreshComplete(state_.lastUpdated_);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putSerializable("state", state_);
	}

	private void updateUI() {
		if(null == player_) {
			return;
		}
		adapter_.notifyDataSetChanged();
		playButton_.setChecked(player_.isPlaying());
	}

	private void updatePodcast(){
		if(null != loadTask_ && loadTask_.getStatus() == AsyncTask.Status.RUNNING){
			Log.d(TAG, "Already loading");
			return;
		}
		Log.d(TAG, "updatePodcast starts: " + loadTask_);
		adapter_.clear();
		SharedPreferences pref =
				PreferenceManager.getDefaultSharedPreferences(PodplayerActivity.this);
		boolean showPodcastIcon = pref.getBoolean("show_episode_icon", true);
		int timeout = Integer.valueOf(pref.getString("read_timeout", "30"));
		loadTask_ = new GetPodcastTask(showPodcastIcon, timeout);
		loadTask_.execute(state_.podcastURLList_.toArray(DUMMY_URL_LIST));
	}
	
	private void updatePlaylist() {
		player_.setPlaylist(state_.loadedEpisode_);
	}
	
	@Override
	public void onClick(View v) {
		//add option to load onStart
		if (v == playButton_) {
			if(player_.isPlaying()) {
				player_.pauseMusic();
			}
			else {
				updatePlaylist();
				if(! player_.restartMusic()) {
					player_.playMusic();
				}
			}
			playButton_.setChecked(player_.isPlaying());
		}
	}
	
	public static void showMessage(Context c, String message) {
		Toast.makeText(c, message, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder binder) {
		player_ = ((PlayerService.LocalBinder)binder).getService();
		player_.setOnStartMusicListener(this);
		playButton_.setEnabled(true);
		updateUI();
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		player_.clearOnStartMusicListener();
		player_ = null;
	}

	@Override
	public void onItemClick(AdapterView<?> list, View view, int pos, long id) {
		//refresh header is added....
		PodInfo info = adapter_.getItem(pos-1);
		PodInfo current = player_.getCurrentPodInfo();
		if(current != null && current.url_.equals(info.url_)) {
			if(player_.isPlaying()) {
				player_.pauseMusic();
			}
			else {
				if(! player_.restartMusic()){
					playByInfo(info);
				}
			}
		}
		else {
			Log.d(TAG, "clicked: " + pos + " " + info.title_);
			updatePlaylist();
			playByInfo(info);
		}
	}

	private void playByInfo(PodInfo info) {
		//umm...
		int playPos = -1;
		for(playPos = 0; playPos < state_.loadedEpisode_.size(); playPos++) {
			if(state_.loadedEpisode_.get(playPos) == info) {
				break;
			}
		}
		if (playPos < 0){
			Log.d(TAG, "playByInfo: info is not found: " + info.url_);
			return;
		}
		player_.playNth(playPos);
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
			finishServiceOnExit = true;
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
	
	public class EpisodeAdapter
		extends ArrayAdapter<PodInfo> {

		public EpisodeAdapter(Context context) {
			super(context, R.layout.episode_item);
		}
		
		//TODO: optimize
		@Override
		public View getView (int position, View convertView, ViewGroup parent) {
			View view;
			if (null == convertView) {
				view = View.inflate(PodplayerActivity.this, R.layout.episode_item, null);
			}
			else {
				view = convertView;
			}
			PodInfo info = getItem(position);
			TextView titleView = (TextView)view.findViewById(R.id.episode_title);
			TextView timeView = (TextView)view.findViewById(R.id.episode_time);
			titleView.setText(info.title_);
			timeView.setText(info.pubdate_);
			ImageView stateIcon = (ImageView)view.findViewById(R.id.play_icon);
			ImageView episodeIcon = (ImageView)view.findViewById(R.id.episode_icon);
			PodInfo current = player_.getCurrentPodInfo();
			if(current != null && current.url_.equals(info.url_)) {
				//cache!
				if(player_.isPlaying()) {
					stateIcon.setImageResource(android.R.drawable.ic_media_play);
				}
				else {
					stateIcon.setImageResource(android.R.drawable.ic_media_pause);
				}
				stateIcon.setVisibility(View.VISIBLE);
			}
			else {
				stateIcon.setVisibility(View.GONE);
			}
			if(showPodcastIcon_ && null != iconData_[info.index_]){
				episodeIcon.setImageDrawable(iconData_[info.index_]);
				episodeIcon.setVisibility(View.VISIBLE);
			}
			else {
				episodeIcon.setVisibility(View.GONE);
			}
			return view;
		}
	}

	//UI is updated in following callback methods
	@Override
	public void onStartMusic(PodInfo info) {
		updateUI();
	}

	@Override
	public void onStartLoadingMusic(PodInfo info) {
		updateUI();
	}

	@Override
	public void onStopMusic(int mode) {
		Log.d(TAG, "onStopMusic");
		updateUI();
	}
	// end of callback methods

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
					Log.d(TAG, "gesture load failed");
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
			Log.d(TAG, "showEpisodeIcon: " + showPodcastIcon_);
		}
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
		Log.d(TAG, "onSharedPreferneceChanged: " + key);
		syncPreference(pref, key);
	}

	enum TagName {
		TITLE, PUBDATE, LINK, NONE
	};

	private InputStream getInputStreamFromURL(URL url) throws IOException{
		URLConnection conn = url.openConnection();
		SharedPreferences pref =
				PreferenceManager.getDefaultSharedPreferences(PodplayerActivity.this);
		int timeout = Integer.valueOf(pref.getString("read_timeout", "30"));
		timeout = timeout * 1000;
		conn.setReadTimeout(timeout);
		return conn.getInputStream();
	}

	private class GetPodcastTask
		extends BaseGetPodcastTask
	{
		public GetPodcastTask(boolean showPodcastIcon, int timeout) {
			super(PodplayerActivity.this, allURLs_, state_.iconURLs_, iconData_, showPodcastIcon, timeout);
		}

		@Override
		protected void onProgressUpdate(PodInfo... values){
			for (int i = 0; i < values.length; i++) {
				PodInfo info = values[i];
				state_.loadedEpisode_.add(info);
				int selectorPos = selector_.getSelectedItemPosition();
				if(selectorPos == 0) {
					//ALL is selected
					adapter_.add(info);
				}
				else {
					String selectedTitle = (String)selector_.getSelectedItem();
					int index = podcastTitle2Index(selectedTitle);
					if(index == info.index_) {
						adapter_.add(info);
					}
				}
			}
		}

		private void onFinished() {
			if(adapter_.isEmpty()) {
				episodeList_.setLastUpdated("");
			}
			else {
				DateFormat df = DateFormat.getDateTimeInstance();
				state_.lastUpdated_ = df.format(new Date());
				episodeList_.setLastUpdated("Last updated: " + state_.lastUpdated_);
			}
			episodeList_.onRefreshComplete();
			loadTask_ = null;
			//TODO: Sync playlist
			updatePlaylist();
		}
		
		@Override
		protected void onPostExecute(Void result) {
			onFinished();
		}
		
		@Override
		protected void onCancelled() {
			onFinished();
		}
	}

	@Override
	public void onRefresh() {
		updatePodcast();
	}

	@Override
	public void onCancel() {
		if (null != loadTask_) {
			loadTask_.cancel(true);
			loadTask_ = null;
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> adapter, View view, int pos, long id) {
		PodInfo info = adapter_.getItem(pos-1);
		Log.d(TAG, "onlongclick: " + info.link_ + " pos: " + pos);
		SharedPreferences pref=
				PreferenceManager.getDefaultSharedPreferences(this);
		boolean enableLongClick = pref.getBoolean("enable_long_click", false);
		if ((! enableLongClick) || null == info.link_) {
			return false;
		}
		//TODO: add preference to enable this 
		Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		if (vibrator != null) {
			vibrator.vibrate(100);
		}
		Intent i =
				new Intent(Intent.ACTION_VIEW, Uri.parse(info.link_));
		startActivity(new Intent(i));
		return true;
	}

	//Filter is changed
	@Override
	public void onItemSelected(AdapterView<?> adapter, View view, int pos, long id) {
		//0: all
		//Log.d(TAG, "selector: pos " + pos);
		adapter_.clear();
		if(pos == 0){
			for(int i = 0; i < state_.loadedEpisode_.size(); i++) {
				PodInfo info = state_.loadedEpisode_.get(i);
				adapter_.add(info);
			}
		}
		else {
			String selectedTitle = (String)adapter.getItemAtPosition(pos);
			int selectedIndex = podcastTitle2Index(selectedTitle);
			for(int i = 0; i < state_.loadedEpisode_.size(); i++) {
				PodInfo info = state_.loadedEpisode_.get(i);
				//Log.d(TAG, "onItemSelected: " + info.index_ + " " + info.title_);
				if(selectedIndex == info.index_){
					adapter_.add(info);
				}
				else if(selectedIndex < info.index_) {
					break;
				}
			}
		}
		if (null == loadTask_) {
			episodeList_.hideHeader();
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> adapter) {
		for(int i = 0; i < state_.loadedEpisode_.size(); i++) {
			PodInfo info = state_.loadedEpisode_.get(i);
			adapter_.add(info);
		}
	}
	
	private int podcastTitle2Index(String title){
		for(int i = 0; i < allTitles_.length; i++) {
			if(title.equals(allTitles_[i])) {
				return i;
			}
		}
		return -1;
	}
	
	final public static
	class PodplayerState
		implements Serializable
	{
		private static final long serialVersionUID = 1L;
		private List<PodInfo> loadedEpisode_;
		private List<URL> podcastURLList_;
		private String lastUpdated_;
		private URL[] iconURLs_;

		private PodplayerState() {
			loadedEpisode_ = new ArrayList<PodInfo>();
			podcastURLList_ = new ArrayList<URL>();
			lastUpdated_ = "";
			iconURLs_ = null;
		}
	}

	@Override
	public void onGesturePerformed(GestureOverlayView view, Gesture gesture) {
		ArrayList<Prediction> predictions = gestureLib_.recognize(gesture);
		if(predictions.size() == 0){
			showMessage(this, "unknown gesture");
			return;
		}
		//predictions is sorted by score
		Prediction p = predictions.get(0);
		if(p.score < gestureScoreThreshold_) {
			showMessage(this, "gesture with low score: " + p.score);
			return;
		}
		if("next".equals(p.name)) {
			player_.playNext();
		}
		else if("play".equals(p.name)) {
			Log.d(TAG, "play by gesture");
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
		showMessage(this, p.name);
	}
}
