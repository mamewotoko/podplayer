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
import android.widget.CheckBox;
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
		//pos--;
		//selected
		DBEpisodeInfo current = (DBEpisodeInfo)(player_.getCurrentEpisodeInfo());
	
		if(current != null && current.getId() == id) {
			Log.d(TAG, "onItemClick: URL: " + current.url_);
			if(player_.isPlaying()) {
				Log.d(TAG, "onItemClick1");
				player_.pauseMusic();
			}
			else {
				Log.d(TAG, "onItemClick2");
				if(! player_.restartMusic()){
					Log.d(TAG, "onItemClick3");
					playById(id);
				}
			}
		}
		else {
			updatePlaylist();
			boolean result = playById(id);
			Log.d(TAG, "onItemClick4: " + result);
		}
	}

	private int getLatestListIndexFromId(long id){
		//umm...
		int playPos = -1;
		for(playPos = 0; playPos < state_.latestList_.size(); playPos++) {
			DBEpisodeInfo dbinfo = (DBEpisodeInfo)(state_.latestList_.get(playPos));
			if(dbinfo.getId() == id){
				break;
			}
		}
		return playPos;
	}
	
	private boolean playById(long id) {
		int playPos = getLatestListIndexFromId(id);
		if (playPos < 0){
			Log.i(TAG, "playById: info is not found: " + id);
			return false;
		}
		Log.d(TAG, "playById: pos: " + playPos + " " + id);
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

	@Override
	public void onCompleteMusic(EpisodeInfo info){
		//TODO: insert listened date by async task
		long now = System.currentTimeMillis();
		DBEpisodeInfo dbinfo = (DBEpisodeInfo)info;
		dbinfo.setListenedTime(now);
		updateListenedDB(dbinfo.getId(), now);
	}

	// end of callback methods

	private void updateListenedDB(long id, long listenedTime){
		ContentValues v = new ContentValues();
		// Uri uri = EpisodeColumns.EPISODE_URI.buildUpon()
		// 	.fragment(String.valueOf(id))
		// 	.build();
		Uri uri = EpisodeColumns.EPISODE_URI.buildUpon()
			.appendPath(String.valueOf(id))
			.build();
		Log.d(TAG, "updateDb: uri: " + uri.toString());
		//v.put(EpisodeColumns.LISTENED, dbinfo.getListenedTime());
		v.put(EpisodeColumns.LISTENED, listenedTime);
		getContentResolver().update(uri, v, null, null);
		updateQuery();
	}
	
	public class EpisodeCursorAdapter
		extends ResourceCursorAdapter
	{
		public EpisodeCursorAdapter(Context context, int layout, Cursor c){
			super(context, layout, c, false);
		}
	
		@Override
		public void bindView(View view, Context context, Cursor cursor){
			//TODO: notifyUpdate if played item changed?
			String title = cursor.getString(EpisodeColumns.TITLE_INDEX);
			String pubdate = cursor.getString(EpisodeColumns.PUBDATE_INDEX);
			
			TextView titleView = (TextView)view.findViewById(R.id.episode_title);
			TextView timeView = (TextView)view.findViewById(R.id.episode_time);
			titleView.setText(title);
			timeView.setText(pubdate);
			TextView listenedView = (TextView)view.findViewById(R.id.episode_listened);
			CheckBox listenedCheck = (CheckBox)view.findViewById(R.id.listened_check);

			//listenedCheck.setOnClickListener(PodplayerDBActivity.this);
			listenedCheck.setOnClickListener(new OnClickListener()
				{
					@Override
					public void onClick(View v){
						//mark listened
						CheckBox ck = (CheckBox)v;
						long id = (Long)ck.getTag();
						//Log.d(TAG, "checkbox clicked: "+id+" "+ck.isChecked());

						int playPos = getLatestListIndexFromId(id);
						if(playPos < 0){
							Log.d(TAG, "not found: " + id);
						}
						DBEpisodeInfo dbinfo = (DBEpisodeInfo)(state_.latestList_.get(playPos));
						long time = 0;
						if(!ck.isChecked()){
							time = 0;
							//ck.setChecked(false);
						}
						else {
							time = System.currentTimeMillis();
							//ck.setChecked(true);
						}
						dbinfo.setListenedTime(time);
						updateListenedDB(id, time);
						adapter_.notifyDataSetChanged();
					}
				});
			listenedCheck.setTag(Long.valueOf(cursor.getLong(EpisodeColumns.ID_INDEX)));
			long listened = cursor.getLong(EpisodeColumns.LISTENED_INDEX);
			//Log.d(TAG, "bindView: " + listened + " " + title);
			if(listened > 0){
				//TODO: add checkmark
				String listenedStr = dateFormat_.format(new Date(listened));
				//TODO: localize
				listenedView.setText("Listened: "+listenedStr);
				listenedView.setVisibility(View.VISIBLE);
				listenedCheck.setChecked(true);
			}
			else {
				listenedView.setVisibility(View.GONE);
				listenedCheck.setChecked(false);
			}
			ImageView stateIcon = (ImageView)view.findViewById(R.id.play_icon);
			ImageView episodeIcon = (ImageView)view.findViewById(R.id.episode_icon);

			int id = cursor.getInt(EpisodeColumns.ID_INDEX);
			DBEpisodeInfo dbinfo = (DBEpisodeInfo)(player_.getCurrentEpisodeInfo());
			
			if(dbinfo != null && id == dbinfo.getId()) {
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
			// for (int i = 0; i < values.length; i++) {
			// 	state_.mergeEpisode(values[i]);
			// }
			//Another Async Task?
			//check if exists
			for(int i = 0; i < values.length; i++){
				//EpisodeInfo info = buffer_.get(i);
				EpisodeInfo info = values[i];
				ContentValues v = new ContentValues();
				v.put(EpisodeColumns.URL, info.url_);
				v.put(EpisodeColumns.TITLE, info.title_);
				v.put(EpisodeColumns.PUBDATE, info.pubdate_);
				//TODO: listened
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

			//updatePlaylist();
			updateQuery();
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
		// EpisodeInfo info = (EpisodeInfo)adapter_.getItem(pos-1);
		// SharedPreferences pref=
		// 		PreferenceManager.getDefaultSharedPreferences(this);
		// Resources res = getResources();
		// boolean enableLongClick = pref.getBoolean("enable_long_click", res.getBoolean(R.bool.default_enable_long_click));
		// if ((! enableLongClick) || null == info.link_) {
		// 	return false;
		// }
		// //TODO: skip if url does not refer html?
		// Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(info.link_));
		// startActivity(i);
		return false;
	}
	
	private void updateQuery(){
		getSupportLoaderManager().restartLoader(LOADER_ID, null, this);
	}

	@Override
	public void onItemSelected(AdapterView<?> adapter, View view, int pos, long id) {
		updateQuery();
	}

	@Override
	public void onNothingSelected(AdapterView<?> adapter) {
		//updateListView();
		updateQuery();
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
		Log.d(TAG, "onLoadFinished: cursor " +c);
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
				//Log.d(TAG, "index: " + index + " " + title + " podurl: " + podcastURL);
				long id = c.getLong(EpisodeColumns.ID_INDEX);
				long listenedTime = c.getLong(EpisodeColumns.LISTENED_INDEX);
				EpisodeInfo info = new DBEpisodeInfo(url, title, pubdate, link, index, id, listenedTime);
				state_.mergeEpisode(info);
			}
			while(c.moveToNext());
		}
		adapter_.swapCursor(c);
		updatePlaylist();
		adapter_.notifyDataSetChanged();
		episodeListView_.hideHeader();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader){
		adapter_.swapCursor(null);
	}
}
