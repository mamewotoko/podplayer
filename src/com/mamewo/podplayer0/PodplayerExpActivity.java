package com.mamewo.podplayer0;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mamewo.lib.podcast_parser.BaseGetPodcastTask;
import com.mamewo.lib.podcast_parser.EpisodeInfo;
import com.mamewo.lib.podcast_parser.PodcastInfo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.net.Uri;
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
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.ToggleButton;

public class PodplayerExpActivity
	extends BasePodplayerActivity
	implements OnClickListener,
	OnLongClickListener,
	ServiceConnection,
	OnItemLongClickListener,
	PlayerService.PlayerStateListener,
	OnSharedPreferenceChangeListener,
	OnChildClickListener
{
	private ToggleButton playButton_;
	private ImageView reloadButton_;
	private Button expandButton_;
	private Button collapseButton_;
	private ExpandableListView expandableList_;
	private SimpleExpandableListAdapter expandableAdapter_;
	private int[] filteredItemIndex_;
	private List<Map<String,String>> groupData_;
	private List<List<Map<String, Object>>> childData_;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState, this, PodplayerExpActivity.class);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.expandable_main);
		reloadButton_ = (ImageView) findViewById(R.id.reload_button);
		reloadButton_.setOnClickListener(this);
		playButton_ = (ToggleButton) findViewById(R.id.play_button);
		playButton_.setOnClickListener(this);
		playButton_.setOnLongClickListener(this);
		playButton_.setEnabled(false);
		expandableList_ =
				(ExpandableListView) findViewById(R.id.exp_list);
		expandableList_.setOnItemLongClickListener(this);
		expandButton_ = (Button) findViewById(R.id.expand_button);
		expandButton_.setOnClickListener(this);
		collapseButton_ = (Button) findViewById(R.id.collapse_button);
		collapseButton_.setOnClickListener(this);
		groupData_ = new ArrayList<Map<String, String>>();
		childData_ = new ArrayList<List<Map<String, Object>>>();
		filteredItemIndex_ = null;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putSerializable("state", state_);
	}

	private void updateUI() {
		if(null == player_) {
			return;
		}
		expandableAdapter_.notifyDataSetChanged();
		playButton_.setChecked(player_.isPlaying());
	}

	//must be called from UI thread
	private void loadPodcast(){
		if (isLoading()) {
			Log.d(TAG, "Already loading");
			return;
		}
		reloadButton_.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
		for (int i = 0; i < childData_.size(); i++) {
			childData_.get(i).clear();
		}
		setProgressBarIndeterminateVisibility(true);
		updateUI();
		SharedPreferences pref=
				PreferenceManager.getDefaultSharedPreferences(this);
		Resources res = getResources();
		int limit = Integer.valueOf(pref.getString("episode_limit", res.getString(R.string.default_episode_limit)));
		int timeoutSec = Integer.valueOf(pref.getString("read_timeout", res.getString(R.string.default_read_timeout)));
		boolean getIcon = pref.getBoolean("show_podcast_icon", res.getBoolean(R.bool.default_show_podcast_icon));
		GetPodcastTask task = new GetPodcastTask(limit, timeoutSec, getIcon);
		startLoading(task);
	}

	@Override
	public void onClick(View view) {
		//add option to load onStart
		if (view == playButton_) {
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
		else if (view == reloadButton_) {
			if (isLoading()) {
				loadTask_.cancel(true);
			}
			else {
				loadPodcast();
			}
		}
		else if (view == expandButton_) {
			for (int i = 0; i < groupData_.size(); i++) {
				expandOrCollapseAll(true);
			}
		}
		else if (view == collapseButton_) {
			for (int i = 0; i < groupData_.size(); i++) {
				expandOrCollapseAll(false);
			}
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

	private void expandOrCollapseAll(boolean expand) {
		for (int i = 0; i < groupData_.size(); i++) {
			if (expand) {
				expandableList_.expandGroup(i);
			}
			else {
				expandableList_.collapseGroup(i);
			}
		}
	}
	
	@Override
	public void onServiceConnected(ComponentName name, IBinder binder) {
		player_ = ((PlayerService.LocalBinder)binder).getService();
		player_.setOnStartMusicListener(this);
		playButton_.setEnabled(true);
		List<EpisodeInfo> playlist = player_.getCurrentPlaylist();
		if (null != playlist) {
			state_.loadedEpisode_ = playlist;
		}
		SharedPreferences pref =
				PreferenceManager.getDefaultSharedPreferences(this);
		syncPreference(pref, "ALL");
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		player_.clearOnStartMusicListener();
		player_ = null;
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v,
								int groupPosition, int childPosition, long id) {
		Log.d(TAG, "ExpActivity.onChildClick: " + groupPosition + " " + childPosition);
		//refresh header is added....
		@SuppressWarnings("unchecked")
		HashMap<String,Object> map =
			(HashMap<String, Object>) expandableAdapter_.getChild(groupPosition, childPosition);
		EpisodeInfo info = (EpisodeInfo) map.get("DATA");
		EpisodeInfo current = player_.getCurrentPodInfo();
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
			updatePlaylist();
			playByInfo(info);
		}
		return true;
	}

	private void playByInfo(EpisodeInfo info) {
		//umm...
		int playPos = -1;
		for(playPos = 0; playPos < state_.loadedEpisode_.size(); playPos++) {
			if(state_.loadedEpisode_.get(playPos) == info) {
				break;
			}
		}
		if (playPos < 0){
			Log.i(TAG, "playByInfo: info is not found: " + info.url_);
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
	
	public class ExpAdapter
		extends SimpleExpandableListAdapter
	{
		public ExpAdapter(Context context,
				List<? extends Map<String, ?>> groupData,
				int groupLayout,
				String[] groupFrom, int[] groupTo,
				List<? extends List<? extends Map<String, ?>>> childData,
				int childLayout, String[] childFrom,
				int[] childTo) {
			super(context, groupData, groupLayout, groupFrom,
					groupTo, childData, childLayout, childFrom, childTo);
		}

		@Override
		public View getChildView (int groupPosition, int childPosition, boolean isLastChild,
								View convertView, ViewGroup parent)
		{
			int childNum = getChildrenCount(groupPosition);
			if(childPosition > childNum) {
				return null;
			}
			View view;
			if (null == convertView) {
				view = View.inflate(PodplayerExpActivity.this, R.layout.episode_item, null);
			}
			else {
				view = convertView;
			}
			@SuppressWarnings("unchecked")
			HashMap<String, Object> map = (HashMap<String, Object>)getChild(groupPosition, childPosition);
			EpisodeInfo info = (EpisodeInfo)map.get("DATA");
			TextView titleView = (TextView)view.findViewById(R.id.episode_title);
			TextView timeView = (TextView)view.findViewById(R.id.episode_time);
			titleView.setText(info.title_);
			timeView.setText(info.pubdate_);
			ImageView stateIcon = (ImageView)view.findViewById(R.id.play_icon);
			ImageView episodeIcon = (ImageView)view.findViewById(R.id.episode_icon);
			EpisodeInfo current = player_.getCurrentPodInfo();
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

	@Override
	public void onStopMusic(int mode) {
		setProgressBarIndeterminateVisibility(false);
		updateUI();
	}
	// end of callback methods

	private void addEpisodeItemsToAdapter(EpisodeInfo[] values) {
		int groupMin = groupData_.size() - 1;
		int groupMax = 0;
		for (int i = 0; i < values.length; i++) {
			EpisodeInfo info = values[i];
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("TITLE", info.title_);
			map.put("DATA", info);
			int groupIndex = filteredItemIndex_[info.index_];
			childData_.get(groupIndex).add(map);
			if (groupIndex < groupMin) {
				groupMin = groupIndex;
			}
			if (groupIndex > groupMax) {
				groupMax = groupIndex;
			}
		}
		for (int i = groupMin; i <= groupMax; i++) {
			int childNum = childData_.get(i).size();
			String numStr;
			if (childNum <= 1) {
				//TODO: localize
				numStr = " item";
			}
			else {
				numStr = " items";
			}
			groupData_.get(i).put("COUNT", childNum + numStr);
		}
		expandableAdapter_.notifyDataSetChanged();
	}
	
	private class GetPodcastTask
		extends BaseGetPodcastTask
	{
		public GetPodcastTask(int limit, int timeoutSec, boolean getIcon) {
			super(PodplayerExpActivity.this, limit, timeoutSec, getIcon);
		}

		@Override
		protected void onProgressUpdate(EpisodeInfo... values){
			for (EpisodeInfo info: values) {
				state_.loadedEpisode_.add(info);
			}
			addEpisodeItemsToAdapter(values);
		}

		private void onFinished(){
			loadTask_ = null;
			setProgressBarIndeterminateVisibility(false);
			//TODO: merge playlist
			updatePlaylist();
			reloadButton_.setImageResource(android.R.drawable.ic_popup_sync);
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
	public boolean onItemLongClick(AdapterView<?> adapter, View view, int pos, long id) {
		SharedPreferences pref=
				PreferenceManager.getDefaultSharedPreferences(this);
		Resources res = getResources();
		boolean enableLongClick = pref.getBoolean("enable_long_click", res.getBoolean(R.bool.default_enable_long_click));
		if (! enableLongClick) {
			return false;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> map =
				(Map<String, Object>)adapter.getItemAtPosition(pos);
		EpisodeInfo info = (EpisodeInfo)map.get("DATA");
		if (null == info) {
			//parent is long clicked
			return false;
		}
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

	//TODO: fetch current playing episode to update currentPodInfo
	@Override
	protected void onPodcastListChanged(boolean start) {
		if (null == filteredItemIndex_ || filteredItemIndex_.length != state_.podcastList_.size()) {
			filteredItemIndex_ = new int[state_.podcastList_.size()];
		}
		for(int i = 0; i < filteredItemIndex_.length; i++) {
			filteredItemIndex_[i] = -1;
		}
		int j = 0;
		groupData_.clear();
		childData_.clear();
		for (int i = 0; i < state_.podcastList_.size(); i++) {
			PodcastInfo info = state_.podcastList_.get(i);
			if (!info.enabled_) {
				continue;
			}
			Map<String, String> groupItem = new HashMap<String, String>();
			filteredItemIndex_[i] = j;
			j++;
			groupItem.put("TITLE", info.title_);
			groupItem.put("COUNT", "");
			groupData_.add(groupItem);
			childData_.add(new ArrayList<Map<String, Object>>());
		}
		expandableAdapter_ = new ExpAdapter(
				this,
				groupData_,
				R.layout.expandable_list_item2,
				new String[] {"TITLE", "COUNT"},
				new int[] { R.id.text1, R.id.text2 },
				childData_,
				R.layout.episode_item,
				null, null);
		expandableList_.setAdapter(expandableAdapter_);
		SharedPreferences pref =
				PreferenceManager.getDefaultSharedPreferences(this);
		Resources res = getResources();
		boolean expandInDefault = pref.getBoolean("expand_in_default", res.getBoolean(R.bool.default_expand_in_default));
		if (expandInDefault) { 
			expandOrCollapseAll(true);
		}
		expandableList_.setOnChildClickListener(this);
		boolean doLoad = pref.getBoolean("load_on_start", res.getBoolean(R.bool.default_load_on_start));
		updateUI();
		List<EpisodeInfo> playlist = state_.loadedEpisode_;
		if (!start || doLoad) {
			loadPodcast();
		}
		else if (null != playlist && ! playlist.isEmpty()) {
			//use list
			addEpisodeItemsToAdapter(playlist.toArray(new EpisodeInfo[0]));
		}
	}

	@Override
	public void onStartLoadingMusic(EpisodeInfo info) {
		setProgressBarIndeterminateVisibility(false);
		updateUI();		
	}

	@Override
	public void onStartMusic(EpisodeInfo info) {
		setProgressBarIndeterminateVisibility(true);
		updateUI();
	}
}
