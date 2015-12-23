package com.mamewo.podplayer0;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.mamewo.podplayer0.Const.*;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.ContentValues;
import android.content.res.Resources;

import android.database.Cursor;

import android.support.v4.content.CursorLoader;
import android.support.v4.widget.ResourceCursorAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
	
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
import android.widget.BaseAdapter;
import android.widget.SimpleCursorAdapter;

import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.SeekBar;

import com.mamewo.lib.podcast_parser.BaseGetPodcastTask;
import com.mamewo.lib.podcast_parser.EpisodeInfo;
import com.mamewo.lib.podcast_parser.PodcastInfo;
import com.markupartist.android.widget.PullToRefreshListView;

public class PodplayerDBActivity
	extends BasePodplayerActivity
	implements OnClickListener,
	OnLongClickListener,
	ServiceConnection,
	OnItemClickListener,
	OnItemLongClickListener,
	OnItemSelectedListener,
	PlayerService.PlayerStateListener,
	PullToRefreshListView.OnRefreshListener,
    PullToRefreshListView.OnCancelListener,
    SeekBar.OnSeekBarChangeListener,
    LoaderManager.LoaderCallbacks<Cursor>
{
	static final
	private int LOADER_ID = 0;
	private ToggleButton playButton_;
	private Spinner selector_;
	private PullToRefreshListView episodeListView_;
	//adapter_: filtered view
	//state_.loadedEpisode_: all data
	private SeekBar currentPlayPosition_;
	private EpisodeCursorAdapter adapter_;
	private List<EpisodeInfo> currentList_;
	private DateFormat dateFormat_;

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

        Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(EpisodeColumns.CONTENT_URI);
        }
        // Cursor cursor = managedQuery(getIntent().getData(),
		// 							 EpisodeColumns.LIST,
		// 							 null,
		// 							 null,
		// 							 null);
		adapter_ = new EpisodeCursorAdapter(this, R.layout.episode_item, null);
		episodeListView_.setAdapter(adapter_);
		
		currentList_ = state_.latestList_;
		currentPlayPosition_ = (SeekBar) findViewById(R.id.seekbar);
		currentPlayPosition_.setOnSeekBarChangeListener(this);
		dateFormat_ = DateFormat.getDateTimeInstance();
		getSupportLoaderManager().initLoader(LOADER_ID, null, this);
	}

	public String getSelectedPodcastURL(){
		if(selector_.getSelectedItemPosition() == 0){
			return null;
		}
		String title = (String)selector_.getSelectedItem();
		List<PodcastInfo> list = state_.podcastList_;
		for(int i = 0; i < list.size(); i++) {
			PodcastInfo info = list.get(i);
			if(title.equals(info.title_)) {
				return info.url_.toString();
			}
		}
		//TODO: error
		return null;
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
		setProgressBarIndeterminateVisibility(true);
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
		pos--;
		//selected
		Cursor c = adapter_.getCursor();
		c.moveToPosition(pos);
		String title = c.getString(EpisodeColumns.TITLE_INDEX);
		String url = c.getString(EpisodeColumns.URL_INDEX);
		String pubdate = c.getString(EpisodeColumns.PUBDATE_INDEX);
		String link = null;
		String podcastURL = c.getString(EpisodeColumns.PODCAST_INDEX);
		int index = 0;
		for(; index < state_.podcastList_.size(); index++){
			if(podcastURL.equals(state_.podcastList_.get(index))){
				break;
			}
		}
		
		EpisodeInfo info = new EpisodeInfo(url, title, pubdate, link, index);
		EpisodeInfo current = player_.getCurrentPodInfo();
	
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

	private boolean playByInfo(EpisodeInfo info) {
		//umm...
		int playPos = -1;
		for(playPos = 0; playPos < state_.latestList_.size(); playPos++) {
			if(state_.latestList_.get(playPos).equalEpisode(info)) {
				break;
			}
		}
		if (playPos < 0){
			Log.i(TAG, "playByInfo: info is not found: " + info.url_);
			return false;
		}
		Log.d(TAG, "playByInfo: pos: " + playPos + " " + info.title_);
		return player_.playNth(playPos);
	}

	//UI is updated in following callback methods
	@Override
	public void onStartMusic(EpisodeInfo info) {
		setProgressBarIndeterminateVisibility(false);
		currentPlayPosition_.setMax(player_.getDuration());
		int pos = player_.getCurrentPositionMsec();
		currentPlayPosition_.setProgress(pos);
		//timer
		updateUI();
	}

	@Override
	public void onStartLoadingMusic(EpisodeInfo info) {
		setProgressBarIndeterminateVisibility(true);
		updateUI();
	}

	@Override
	public void onStopMusic(int mode) {
		setProgressBarIndeterminateVisibility(false);
		updateUI();
	}
	// end of callback methods

	public class EpisodeCursorAdapter
		extends ResourceCursorAdapter
	{
		public EpisodeCursorAdapter(Context context, int layout, Cursor c){
			super(context, layout, c, false);
		}

		// @Override
		// public View newView(Context context, Cursor cursor, ViewGroup parent){
		// 	returnView.inflate(context, R.layout.episode_item, null);
		// }
		
		@Override
		public void bindView(View view, Context context, Cursor cursor){
			//TODO: notifyUpdate if played item changed?
			String title = cursor.getString(EpisodeColumns.TITLE_INDEX);
			String pubdate = cursor.getString(EpisodeColumns.PUBDATE_INDEX);
			long listened = cursor.getLong(EpisodeColumns.LISTENED_INDEX);
			
			TextView titleView = (TextView)view.findViewById(R.id.episode_title);
			TextView timeView = (TextView)view.findViewById(R.id.episode_time);
			titleView.setText(title);
			timeView.setText(pubdate);
			if(listened > 0){
				//TODO: add checkmark
				TextView listenedView = (TextView)view.findViewById(R.id.episode_listened);
				String listenedStr = dateFormat_.format(new Date(listened));
				//TODO: localize
				listenedView.setText("Listened: "+listenedStr);
			}
			ImageView stateIcon = (ImageView)view.findViewById(R.id.play_icon);
			ImageView episodeIcon = (ImageView)view.findViewById(R.id.episode_icon);
			//long rowId = cursor.getLong(EpisodeColumns._ID);
			//long playingRowId = player_.getCurrentEpisodeId();
			String url = cursor.getString(EpisodeColumns.URL_INDEX);
			String podcastURL = cursor.getString(EpisodeColumns.PODCAST_INDEX);
			int index = 0;
			for(; index < state_.podcastList_.size(); index++){
				if(podcastURL.equals(state_.podcastList_.get(index).url_.toString())){
					break;
				}
			}
			EpisodeInfo info = new EpisodeInfo(url, title, pubdate, null, index);
			EpisodeInfo currentInfo = player_.getCurrentPodInfo();
			Log.d(TAG, "getItem: " + info.url_ + " " + info.title_ + " " + info.index_ + " current " + currentInfo + " " + info.equalEpisode(currentInfo));
			if(currentInfo != null){
				Log.d(TAG, "  getItem current: " + currentInfo.url_ + " " + currentInfo.title_ + " " + currentInfo.index_);
			}
			if(info.equalEpisode(currentInfo)){
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
			// if(showPodcastIcon_ && null != state_.podcastList_.get(info.index_).icon_){
			// 	episodeIcon.setImageDrawable(state_.podcastList_.get(info.index_).icon_);
			// 	episodeIcon.setVisibility(View.VISIBLE);
			// }
			// else {
			// 	episodeIcon.setVisibility(View.GONE);
			// }
		}
	}

	private class GetPodcastTask
		extends BaseGetPodcastTask
	{
		public GetPodcastTask(int limit, int timeoutSec, boolean getIcon) {
			super(PodplayerDBActivity.this, limit, timeoutSec, getIcon);
		}

		@Override
		protected void onProgressUpdate(EpisodeInfo... values){
			for (int i = 0; i < values.length; i++) {
				state_.mergeEpisode(values[i]);
			}
			//Another Async Task?
			//check if exists
			for(int i = 0; i < values.length; i++){
				//EpisodeInfo info = buffer_.get(i);
				EpisodeInfo info = values[i];
				ContentValues v = new ContentValues();
				v.put(EpisodeColumns.URL, info.url_);
				v.put(EpisodeColumns.TITLE, info.title_);
				v.put(EpisodeColumns.PUBDATE, info.pubdate_);
				v.put(EpisodeColumns.LISTENED, 0);
				//TODO: link?
				v.put(EpisodeColumns.PODCAST, state_.podcastList_.get(info.index_).url_.toString());
				Log.d(TAG, "add episode " + info.title_);
				getContentResolver().insert(EpisodeColumns.CONTENT_URI, v);
			}
			adapter_.notifyDataSetChanged();
		}

		private void onFinished() {
			if(adapter_.isEmpty()) {
				episodeListView_.setLastUpdated("");
			}
			else {
				//TODO: change format of date
				state_.lastUpdated_ = dateFormat_.format(new Date());
				episodeListView_.setLastUpdated(getString(R.string.header_lastupdated) + state_.lastUpdated_);
			}
			setProgressBarIndeterminateVisibility(false);
			episodeListView_.onRefreshComplete();
			episodeListView_.hideHeader();
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
		EpisodeInfo info = (EpisodeInfo)adapter_.getItem(pos-1);
		SharedPreferences pref=
				PreferenceManager.getDefaultSharedPreferences(this);
		Resources res = getResources();
		boolean enableLongClick = pref.getBoolean("enable_long_click", res.getBoolean(R.bool.default_enable_long_click));
		if ((! enableLongClick) || null == info.link_) {
			return false;
		}
		//TODO: skip if url does not refer html?
		Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(info.link_));
		startActivity(i);
		return true;
	}
	
	private void updateListView(){
		getSupportLoaderManager().restartLoader(LOADER_ID, null, this);
	}

	@Override
	public void onItemSelected(AdapterView<?> adapter, View view, int pos, long id) {
		updateListView();
	}

	@Override
	public void onNothingSelected(AdapterView<?> adapter) {
		updateListView();
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
		SharedPreferences pref=
				PreferenceManager.getDefaultSharedPreferences(this);
		syncPreference(pref, "ALL");
		//TODO: move to base?
		List<EpisodeInfo> playlist = player_.getCurrentPlaylist();
		if (null != playlist) {
			state_.latestList_ = playlist;
			state_.loadedEpisode_ = new ArrayList<List<EpisodeInfo>>();
			for(int i = 0; i < state_.podcastList_.size(); i++){
				state_.loadedEpisode_.add(new ArrayList<EpisodeInfo>());
			}
			for(EpisodeInfo info: state_.latestList_){
				state_.loadedEpisode_.get(info.index_).add(info);
			}
		}
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
		Resources res = getResources();
		boolean doLoad = pref.getBoolean("load_on_start", res.getBoolean(R.bool.default_load_on_start));
		// List<EpisodeInfo> playlist = state_.latestList_;
		// if (!start || doLoad) {
		// 	//reload
		// 	episodeListView_.startRefresh();
		// }
		// else if (playlist != null && ! playlist.isEmpty()) {
		// 	//update list by loaded items
		// 	updateListView();
		// 	episodeListView_.onRefreshComplete(state_.lastUpdated_);
		// }
		updateUI();
	}

	@Override
	public void notifyOrderChanged(int order){
		adapter_.notifyDataSetChanged();
	}

	@Override
	public void onProgressChanged(SeekBar bar, int progress, boolean fromUser){
		if(!fromUser){
			return;
		}
		player_.seekTo(progress);
	}

	@Override
	public void onStartTrackingTouch(SeekBar bar){
		//nop
	}

	@Override
	public void onStopTrackingTouch(SeekBar bar){
		//nop
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args){
		Uri baseUri = EpisodeColumns.CONTENT_URI;
		String url = getSelectedPodcastURL();
		String selection = null;
		String[] selectionArgs = null;

		if(url != null){
			selection = EpisodeColumns.PODCAST + " = ? ";
			selectionArgs = new String[] { url };
		}
		return new CursorLoader(this, baseUri, null, selection, selectionArgs, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor c){
		if(c == null){
			adapter_.swapCursor(c);
			return;
		}
		//debug
		for(int i = 0; i < state_.podcastList_.size(); i++){
			Log.d(TAG, "Podlist url: " + state_.podcastList_.get(i).url_);
		}
		//TODO: XXXX use cursor only ?
		if(c.moveToFirst()){
			do {
				String title = c.getString(EpisodeColumns.TITLE_INDEX);
				String url = c.getString(EpisodeColumns.URL_INDEX);
				String pubdate = c.getString(EpisodeColumns.PUBDATE_INDEX);
				String link = null;
				String podcastURL = c.getString(EpisodeColumns.PODCAST_INDEX);
				int index = 0;
				for(; index < state_.podcastList_.size(); index++){
					if(podcastURL.equals(state_.podcastList_.get(index).url_.toString())){
						break;
					}
				}
				Log.d(TAG, "index: " + index + " " + title + " podurl: " + podcastURL);
				EpisodeInfo info = new EpisodeInfo(url, title, pubdate, link, index);
				state_.mergeEpisode(info);
			}
			while(c.moveToNext());
		}
		adapter_.swapCursor(c);
		adapter_.notifyDataSetChanged();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader){
		adapter_.swapCursor(null);
	}
}
