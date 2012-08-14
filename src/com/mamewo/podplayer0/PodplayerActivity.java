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
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.mamewo.podplayer0.PlayerService.PodInfo;
import com.markupartist.android.widget.PullToRefreshListView;

public class PodplayerActivity
	extends BasePodplayerActivity
	implements OnClickListener,
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
	private ArrayAdapter<PodInfo> adapter_;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState, this);
		setContentView(R.layout.main);
		playButton_ = (ToggleButton) findViewById(R.id.play_button);
		playButton_.setOnClickListener(this);
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

	//TODO: fetch current playing episode to update currentPodInfo
	@Override
	public void onResume(){
		super.onResume();
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
		SharedPreferences pref=
				PreferenceManager.getDefaultSharedPreferences(this);
		boolean doLoad = pref.getBoolean("load_on_start", true);
		updateUI();
		if(doLoad && adapter_.getCount() == 0){
			episodeListView_.startRefresh();
		}
		else {
			episodeListView_.onRefreshComplete(state_.lastUpdated_);
		}
	}

	private void updateUI() {
		if(null == player_) {
			return;
		}
		adapter_.notifyDataSetChanged();
		playButton_.setChecked(player_.isPlaying());
	}

	private void loadPodcast(){
		if (isLoading()) {
			Log.d(TAG, "Already loading");
			return;
		}
		Log.d(TAG, "updatePodcast starts: " + loadTask_);
		adapter_.clear();
		SharedPreferences pref =
				PreferenceManager.getDefaultSharedPreferences(PodplayerActivity.this);
		int timeout = Integer.valueOf(pref.getString("read_timeout", "30"));
		GetPodcastTask task = new GetPodcastTask(showPodcastIcon_, timeout);
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
				updatePlaylist();
				if(! player_.restartMusic()) {
					player_.playMusic();
				}
			}
			playButton_.setChecked(player_.isPlaying());
		}
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

	public class EpisodeAdapter
		extends ArrayAdapter<PodInfo>
	{
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
				episodeListView_.setLastUpdated("");
			}
			else {
				DateFormat df = DateFormat.getDateTimeInstance();
				state_.lastUpdated_ = df.format(new Date());
				episodeListView_.setLastUpdated("Last updated: " + state_.lastUpdated_);
			}
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
}
