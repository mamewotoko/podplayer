package com.mamewo.podplayer0;

/**
 * @author Takashi Masuyama <mamewotoko@gmail.com>
 * http://www002.upp.so-net.ne.jp/mamewo/
 */

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.mamewo.podplayer0.PlayerService.MusicInfo;
import com.markupartist.android.widget.PullToRefreshListView;

public class PodplayerActivity
	extends BasePodplayerActivity
	implements OnClickListener,
	OnLongClickListener,
	ServiceConnection,
	OnItemClickListener,
	OnItemLongClickListener,
	OnItemSelectedListener,
	PlayerService.PlayerStateListener,
	PullToRefreshListView.OnRefreshListener,
	PullToRefreshListView.OnCancelListener
{
	private ToggleButton playButton_;
	private Spinner selector_;
	private PullToRefreshListView episodeListView_;
	private ArrayAdapter<MusicInfo> adapter_;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState, this, PodplayerActivity.class);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.main);
		playButton_ = (ToggleButton) findViewById(R.id.play_button);
		playButton_.setOnClickListener(this);
		playButton_.setOnLongClickListener(this);
		playButton_.setEnabled(false);
		selector_ = (Spinner) findViewById(R.id.podcast_selector);
		selector_.setOnItemSelectedListener(this);
		episodeListView_ = (PullToRefreshListView) findViewById(R.id.list);
		episodeListView_.setOnItemClickListener(this);
		episodeListView_.setOnItemLongClickListener(this);
		episodeListView_.setOnRefreshListener(this);
		episodeListView_.setOnCancelListener(this);
		adapter_ = new EpisodeAdapter(this);
		episodeListView_.setAdapter(adapter_);
	}

	private void updateUI() {
		if(null == player_) {
			return;
		}
		adapter_.notifyDataSetChanged();
		playButton_.setChecked(player_.isPlaying());
	}

	private void loadPodcast() {
		if (isLoading()) {
			Log.i(TAG, "Already loading");
			return;
		}
		adapter_.clear();
		setProgressBarIndeterminateVisibility(true);
		GetPodcastTask task = new GetPodcastTask();
		startLoading(task);
	}
	
	@Override
	public void onClick(View v) {
		//add option to load onStart
		if (v == playButton_) {
			if(player_.isPlaying()) {
				player_.pauseMusic();
			}
			else {
				if (null == state_.loadedEpisode_ || state_.loadedEpisode_.isEmpty()) {
					return;
				}
				updatePlaylist();
				if(! player_.restartMusic()) {
					player_.playMusic();
				}
			}
			playButton_.setChecked(player_.isPlaying());
		}
	}

	@Override
	public boolean onLongClick(View view) {
		if (view == playButton_) {
			//TODO: add preference to enable this 
			Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			if (vibrator != null) {
				vibrator.vibrate(100);
			}
			if (player_.isPlaying()) {
				player_.stopMusic();
			}
			else {
				player_.playMusic();
			}
			return true;
		}
		return false;
	}

	@Override
	public void onItemClick(AdapterView<?> list, View view, int pos, long id) {
		//refresh header is added....
		MusicInfo info = adapter_.getItem(pos-1);
		MusicInfo current = player_.getCurrentPodInfo();
		if(current != null && current.url_.equals(info.url_)) {
			Log.d(TAG, "onItemClick: URL: " + current.url_);
			if(player_.isPlaying()) {
				Log.d(TAG, "onItemClick1");
				player_.pauseMusic();
			}
			else {
				Log.d(TAG, "onItemClick2");
				if(! player_.restartMusic()){
					Log.d(TAG, "onItemClick3");
					playByInfo(info);
				}
			}
		}
		else {
			updatePlaylist();
			boolean result = playByInfo(info);
			Log.d(TAG, "onItemClick4: " + result);
		}
	}

	private boolean playByInfo(MusicInfo info) {
		//umm...
		int playPos = -1;
		for(playPos = 0; playPos < state_.loadedEpisode_.size(); playPos++) {
			if(state_.loadedEpisode_.get(playPos) == info) {
				break;
			}
		}
		if (playPos < 0){
			Log.i(TAG, "playByInfo: info is not found: " + info.url_);
			return false;
		}
		return player_.playNth(playPos);
	}

	public class EpisodeAdapter
		extends ArrayAdapter<MusicInfo>
	{
		public EpisodeAdapter(Context context) {
			super(context, R.layout.episode_item);
		}
		
		@Override
		public View getView (int position, View convertView, ViewGroup parent) {
			View view;
			if (null == convertView) {
				view = View.inflate(PodplayerActivity.this, R.layout.episode_item, null);
			}
			else {
				view = convertView;
			}
			MusicInfo info = getItem(position);
			TextView titleView = (TextView)view.findViewById(R.id.episode_title);
			TextView timeView = (TextView)view.findViewById(R.id.episode_time);
			titleView.setText(info.title_);
			timeView.setText(info.pubdate_);
			ImageView stateIcon = (ImageView)view.findViewById(R.id.play_icon);
			ImageView episodeIcon = (ImageView)view.findViewById(R.id.episode_icon);
			MusicInfo current = player_.getCurrentPodInfo();
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
			if(showPodcastIcon_ && null != state_.podcastList_.get(info.index_).icon_){
				episodeIcon.setImageDrawable(state_.podcastList_.get(info.index_).icon_);
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
	public void onStartMusic(MusicInfo info) {
		setProgressBarIndeterminateVisibility(false);
		updateUI();
	}

	@Override
	public void onStartLoadingMusic(MusicInfo info) {
		setProgressBarIndeterminateVisibility(true);
		updateUI();
	}

	@Override
	public void onStopMusic(int mode) {
		setProgressBarIndeterminateVisibility(false);
		updateUI();
	}
	// end of callback methods

	private void addEpisodeItemsToAdapter(MusicInfo[] values) {
		for (int i = 0; i < values.length; i++) {
			MusicInfo info = values[i];
			int selectorPos = selector_.getSelectedItemPosition();
			if(selectorPos == 0) {
				//ALL is selected
				adapter_.add(info);
				adapter_.notifyDataSetChanged();
			}
			else {
				String selectedTitle = (String)selector_.getSelectedItem();
				int index = podcastTitle2Index(selectedTitle);
				if(index == info.index_) {
					adapter_.add(info);
					adapter_.notifyDataSetChanged();
				}
			}
		}
	}
	
	private class GetPodcastTask
		extends BaseGetPodcastTask
	{
		public GetPodcastTask() {
			super(PodplayerActivity.this);
		}

		@Override
		protected void onProgressUpdate(MusicInfo... values){
			for (int i = 0; i < values.length; i++) {
				state_.loadedEpisode_.add(values[i]);
			}
			addEpisodeItemsToAdapter(values);
		}

		private void onFinished() {
			if(adapter_.isEmpty()) {
				episodeListView_.setLastUpdated("");
			}
			else {
				DateFormat df = DateFormat.getDateTimeInstance();
				//TODO: change format of date
				state_.lastUpdated_ = df.format(new Date());
				episodeListView_.setLastUpdated(getString(R.string.header_lastupdated) + state_.lastUpdated_);
			}
			setProgressBarIndeterminateVisibility(false);
			episodeListView_.onRefreshComplete();
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
		loadPodcast();
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
		MusicInfo info = adapter_.getItem(pos-1);
		SharedPreferences pref=
				PreferenceManager.getDefaultSharedPreferences(this);
		boolean enableLongClick = pref.getBoolean("enable_long_click", PodplayerPreference.DEFAULT_ENABLE_LONG_CLICK);
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
		startActivity(i);
		return true;
	}

	//Filter is changed
	@Override
	public void onItemSelected(AdapterView<?> adapter, View view, int pos, long id) {
		//0: all
		adapter_.clear();
		if(pos == 0){
			for(int i = 0; i < state_.loadedEpisode_.size(); i++) {
				MusicInfo info = state_.loadedEpisode_.get(i);
				adapter_.add(info);
			}
		}
		else {
			String selectedTitle = (String)adapter.getItemAtPosition(pos);
			int selectedIndex = podcastTitle2Index(selectedTitle);
			for(int i = 0; i < state_.loadedEpisode_.size(); i++) {
				MusicInfo info = state_.loadedEpisode_.get(i);
				if (selectedIndex == info.index_) {
					adapter_.add(info);
				}
				else if (selectedIndex < info.index_) {
					break;
				}
			}
		}
		if (! isLoading()) {
			episodeListView_.hideHeader();
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> adapter) {
		for(int i = 0; i < state_.loadedEpisode_.size(); i++) {
			MusicInfo info = state_.loadedEpisode_.get(i);
			adapter_.add(info);
		}
	}
	
	private int podcastTitle2Index(String title){
		List<PodcastInfo> list = state_.podcastList_;
		for(int i = 0; i < list.size(); i++) {
			PodcastInfo info = list.get(i);
			if(title.equals(info.title_)) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder binder) {
		player_ = ((PlayerService.LocalBinder)binder).getService();
		player_.setOnStartMusicListener(this);
		playButton_.setEnabled(true);
		//TODO: move to base?
		List<MusicInfo> playlist = player_.getCurrentPlaylist();
		if (null != playlist) {
			state_.loadedEpisode_ = playlist;
		}
		SharedPreferences pref=
				PreferenceManager.getDefaultSharedPreferences(this);
		syncPreference(pref, "ALL");
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		player_.clearOnStartMusicListener();
		player_ = null;
	}

	@Override
	protected void onPodcastListChanged(boolean start) {
		Log.d(TAG, "onPodcastListChanged");
		SharedPreferences pref=
				PreferenceManager.getDefaultSharedPreferences(this);
		List<String> list = new ArrayList<String>();
		list.add(getString(R.string.selector_all));
		//stop loading?
		for(int i = 0; i < state_.podcastList_.size(); i++) {
			PodcastInfo info = state_.podcastList_.get(i);
			if (info.enabled_) {
				list.add(info.title_);
			}
		}
		ArrayAdapter<String> adapter =
				new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
		//TODO: load if selected item is changed
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		selector_.setAdapter(adapter);
		boolean doLoad = pref.getBoolean("load_on_start", PodplayerPreference.DEFAULT_LOAD_ON_START);
		List<MusicInfo> playlist = state_.loadedEpisode_;
		Log.d(TAG, "podcastListChanged: " + state_.loadedEpisode_.size());
		if (start && doLoad && playlist.isEmpty()) {
			//reload
			episodeListView_.startRefresh();
		}
		else if (playlist != null && ! playlist.isEmpty()) {
			//update list by loaded items
			adapter_.clear();
			addEpisodeItemsToAdapter(playlist.toArray(new MusicInfo[0]));
			episodeListView_.onRefreshComplete(state_.lastUpdated_);
		}
		updateUI();
	}
}
