package com.mamewo.podplayer0;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.mamewo.podplayer0.Const.*;

import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
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
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.mamewo.podplayer0.parser.BaseGetPodcastTask;
//import com.mamewo.podplayer0.parser.EpisodeInfo;
//import com.mamewo.podplayer0.parser.PodcastInfo;
import com.mamewo.podplayer0.parser.Podcast;
import com.markupartist.android.widget.PullToRefreshListView;

import com.mamewo.podplayer0.db.PodcastRealm;
import com.mamewo.podplayer0.db.EpisodeRealm;
import io.realm.RealmResults;
import io.realm.Realm;
//import io.realm.RealmChangeListener;

import com.bumptech.glide.Glide;
import android.support.v7.widget.Toolbar;
import android.support.v7.app.ActionBar;
import android.widget.ImageButton;

public class PodplayerActivity
    extends BasePodplayerActivity
    implements OnClickListener,
    OnItemClickListener,
    OnItemLongClickListener,
    OnItemSelectedListener,
    PlayerService.PlayerStateListener,
    PullToRefreshListView.OnRefreshListener,
    PullToRefreshListView.OnCancelListener
               //SeekBar.OnSeekBarChangeListener           
{
    private ImageButton playButton_;
    private Spinner selector_;
    private PullToRefreshListView episodeListView_;
    //adapter_: filtered view
    //state_.loadedEpisode_: all data
    //private SeekBar currentPlayPosition_;
    private EpisodeAdapter adapter_;
    private RealmResults<EpisodeRealm> currentList_;
    //number of items for one screen (small phone)
    static final
    public int EPISODE_BUF_SIZE = 10;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayShowTitleEnabled(false);

		playButton_ = (ImageButton) findViewById(R.id.play_button);
        playButton_.setOnClickListener(this);
        playButton_.setEnabled(false);
        selector_ = (Spinner) findViewById(R.id.podcast_selector);
        selector_.setOnItemSelectedListener(this);
        episodeListView_ = (PullToRefreshListView) findViewById(R.id.list);
        episodeListView_.setOnItemClickListener(this);
        episodeListView_.setOnItemLongClickListener(this);
        episodeListView_.setOnRefreshListener(this);
        episodeListView_.setOnCancelListener(this);
        //initial dummy
        //adapter_ is initialized after player initialized
        adapter_ = new EpisodeAdapter();
        episodeListView_.setAdapter(adapter_);
        updateSelector();
        //currentPlayPosition_ = (SeekBar) findViewById(R.id.seekbar);
        //currentPlayPosition_.setOnSeekBarChangeListener(this);
    }

    @Override
    public void notifyPodcastListChanged(RealmResults<PodcastRealm> results){
        updateSelector();
    }

    @Override
    public void notifyLatestListChanged(RealmResults<EpisodeRealm> results){
        adapter_.notifyDataSetChanged();
    }
    
    private void updateUI() {
        adapter_.notifyDataSetChanged();
        updatePlayButton();
    }

    private void loadPodcast() {
        if (isLoading()) {
            Log.i(TAG, "Already loading");
            return;
        }
        Resources res = getResources();
        int limit = Integer.valueOf(pref_.getString("episode_limit", res.getString(R.string.default_episode_limit)));
        GetPodcastTask task = new GetPodcastTask(limit);
        startLoading(task);
    }

    private void updatePlayButton(){
        if(null == player_){
            return;
        }
        if(player_.isPlaying()){
            playButton_.setContentDescription(getResources().getString(R.string.action_pause));
            playButton_.setImageResource(R.drawable.ic_pause_white_24dp);
        }
        else {
            playButton_.setContentDescription(getResources().getString(R.string.action_play));
            playButton_.setImageResource(R.drawable.ic_play_arrow_white_24dp);
        }
    }

    @Override
    public void onClick(View v) {
        //add option to load onStart
        if (v == playButton_) {
            if(null == player_){
                return;
            }
            if(player_.isPlaying()) {
                player_.pauseMusic();
            }
            else {
                updatePlaylist(null);
                if(! player_.restartMusic()) {
                    player_.playMusic();
                }
            }
            updatePlayButton();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> list, View view, int pos, long id) {
        if(null == player_){
            return;
        }
        //refresh header is added....
        EpisodeRealm info = (EpisodeRealm)adapter_.getItem(pos-1);
        EpisodeRealm current = player_.getCurrentPodInfo();
        Log.d(TAG, "current: "+current);
        Log.d(TAG, "clicked: "+info);
        if(current != null && current.getId() == info.getId()) {
            Log.d(TAG, "current: title "+current.getTitle());
            Log.d(TAG, "onItemClick: URL: " + current.getURL());
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
            //pass query
            updatePlaylist(getFilterPodcastTitle());
            boolean result = playByInfo(info);
        }
    }

    private boolean playByInfo(EpisodeRealm info) {
        //umm...
        int playPos;
        for(playPos = 0; playPos < state_.latestList_.size(); playPos++) {
            if(state_.latestList_.get(playPos).getId() == info.getId()
               && state_.latestList_.get(playPos).getPodcast().getId() == info.getPodcast().getId()) {
                break;
            }
        }
        if (playPos < 0){
            Log.i(TAG, "playByInfo: info is not found: " + info.getURL());
            return false;
        }
        Log.d(TAG, "playByInfo: "+playPos);
        return player_.playNth(playPos);
    }

    //UI is updated in following callback methods
    @Override
    public void onStartMusic(long episodeId) {
		//setProgressBarIndeterminateVisibility(false);
		//currentPlayPosition_.setMax(player_.getDuration());
		//int pos = player_.getCurrentPositionMsec();
        //currentPlayPosition_.setProgress(pos);
        //timer
        updateUI();
    }

    //xxxx
    @Override
    public void onCompleteMusic(long episodeId){
        Realm realm = Realm.getDefaultInstance();
        Episode episode = realm.where(EpisodeRealm.class).equalTo("id", episodeId);
        if(episode.size() == 0){
            Log.d(TAG, "onCompleteMusic: no episode");
            return;
        }
        //TODO: async
        ListenedEpisodeRealm listened = realm.createObject(ListenedEpisodeRealm.class);
        realm.beginTransaction();
        listened.setDate(new Date());
        listened.setEpisode(episode);
        listened.setPodcastTitle(episode.getPodcast().getTitle());
        listened.setEpisodeTitle(episode.getTitle());
        realm.commitTransaction();
    }

    @Override
    public void onStartLoadingMusic(long episodeId) {
        updateUI();
    }

    @Override
    public void onStopMusic(int mode) {
        updateUI();
    }
    // end of callback methods

    public class EpisodeAdapter
        extends BaseAdapter
    {
        public EpisodeAdapter(){
        }
        
        @Override
        public int getCount(){
            return state_.latestList_.size();
        }

        @Override
        public Object getItem(int position){
            return state_.latestList_.get(position);
        }
        
        @Override
        public long getItemId(int position){
            return state_.latestList_.get(position).getId();
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            EpisodeHolder holder;
            
            if (null == convertView) {
                view = View.inflate(PodplayerActivity.this, R.layout.episode_item, null);
                holder = new EpisodeHolder();
                holder.titleView_ = (TextView)view.findViewById(R.id.episode_title);
                holder.timeView_ = (TextView)view.findViewById(R.id.episode_time);
                holder.stateIcon_ = (ImageView)view.findViewById(R.id.play_icon);
                holder.episodeIcon_ = (ImageView)view.findViewById(R.id.episode_icon);
                holder.displayedIconURL_ = null;
                view.setTag(holder);
            }
            else {
                view = convertView;
                holder = (EpisodeHolder)view.getTag();
            }
            EpisodeRealm episode = (EpisodeRealm)getItem(position);
            holder.titleView_.setText(episode.getTitle());

            holder.timeView_.setText(episode.getPubdateStr(dateFormat_));

            if(player_ == null){
                holder.stateIcon_.setVisibility(View.GONE);
            }
            else {
                EpisodeRealm current = player_.getCurrentPodInfo();
                if(current != null && current.getURL().equals(episode.getURL())) {
                //TODO: cache!
                    if(player_.isPlaying()) {
                        holder.stateIcon_.setImageResource(R.drawable.ic_play_arrow_white_24dp);
                        holder.stateIcon_.setContentDescription(getString(R.string.icon_desc_playing));
                    }
                    else {
                        holder.stateIcon_.setImageResource(R.drawable.ic_pause_white_24dp);
                        holder.stateIcon_.setContentDescription(getString(R.string.icon_desc_pausing));
                    }
                    holder.stateIcon_.setVisibility(View.VISIBLE);
                }
                else {
                    holder.stateIcon_.setVisibility(View.GONE);
                }
            }
            // Log.d(TAG, "icon: " + episode.getTitle() + " index: " + episode.getIndex()
            //       + " current: " + currentList_.size()
            //       + " podcast:" + state_.podcastList_.size());
            //String iconURL = state_.podcastList_.get(episode.getIndex()).getIconURL();
            String iconURL = episode.getPodcast().getIconURL();
            if(showPodcastIcon_ && null != iconURL){
                //TODO: check previous icon url
                String displayedIconURL = holder.displayedIconURL_;
                if(View.GONE == holder.episodeIcon_.getVisibility()
                   || null == displayedIconURL
                   || !displayedIconURL.equals(iconURL)){
                    Glide
                        .with(getApplicationContext())
                        .load(iconURL)
                        .into(holder.episodeIcon_);
                    holder.episodeIcon_.setContentDescription(episode.getTitle());
                }
                holder.episodeIcon_.setVisibility(View.VISIBLE);
            }
            else {
                Glide.clear(holder.episodeIcon_);
                holder.episodeIcon_.setContentDescription(getString(R.string.icon_desc_episode_none));
                holder.episodeIcon_.setVisibility(View.GONE);
            }
            holder.displayedIconURL_ = iconURL;
            return view;
        }
    }

    private class GetPodcastTask
        extends BaseGetPodcastTask
    {
        private Podcast prevPodInfo_;
        
        public GetPodcastTask(int limit) {
            super(PodplayerActivity.this, client_, limit, EPISODE_BUF_SIZE);
            prevPodInfo_ = null;
        }

        @Override
        protected void onProgressUpdate(String... values){
            // for (int i = 0; i < values.length; i++) {
            //     state_.mergeEpisode(values[i]);
            //     //adapter_.add(values[i]);
            // }
            //Log.d(TAG, "onProgressUpdate");
            //filterSelectedPodcast();
            //adapter_.notifyDataSetChanged();
        }

        private void onFinished() {
            //TODO: change format of date
            state_.lastUpdatedDate_ = new Date();
            episodeListView_.onRefreshComplete(getString(R.string.header_lastupdated) + dateFormat_.format(state_.lastUpdatedDate_));
            episodeListView_.hideHeader();
            loadTask_ = null;
            //dummy
            //sortEpisodeByDate(true);
            //TODO: Sync playlist
            updatePlaylist(null);
            updateUI();
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
        EpisodeRealm info = (EpisodeRealm)adapter_.getItem(pos-1);
        Resources res = getResources();
        boolean enableLongClick = pref_.getBoolean("enable_long_click", res.getBoolean(R.bool.default_enable_long_click));
        if ((! enableLongClick) || null == info.getLink()) {
            return false;
        }
        //TODO: skip if url does not refer html?
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(info.getLink()));
        startActivity(i);
        return true;
    }

    // public void sortEpisodeByDate(boolean latestFirst){
    //     //dummy
    //     Log.d(TAG, "sort by pubdate");
    //     Collections.sort(currentList_, new EpisodeInfo.PubdateComparator());
    //     if(latestFirst){
    //         Collections.reverse(currentList_);
    //     }
    // }

    //TODO: tuning
    // private void filterSelectedPodcast(){
    //     RealmResults<EpisodeRealm> l;
    //     //TODO: design incremnetal add 
    //     if(selector_.getSelectedItemPosition() == 0){
    //         //-1: all
    //         l = state_.latestList_;
    //     }
    //     else {
    //         String title = (String)selector_.getSelectedItem();
    //         Realm realm = Realm.getDefaultInstance();
    //         RealmResults<PodcastRealm> infoList = realm.where(PodcastRealm.class).equalTo("title", title).findAll();
    //         int podcastId = infoList.get(0).getId();
    //         l = realm.where(EpisodeRealm.class).equalTo("podcast.id", podcastId).findAll();
    //     }
    //     //TODO: selected item is removed
    //     currentList_ = l;
    //     //Log.d(TAG, "filterSelectedPodcast: "+ currentList_.size());
    //     if (! isLoading()) {
    //         episodeListView_.hideHeader();
    //     }
    //     adapter_.notifyDataSetChanged();
    // }

    @Override
    public void onItemSelected(AdapterView<?> adapter, View view, int pos, long id) {
        filterSelectedPodcast();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapter) {
        filterSelectedPodcast();
    }
    
    private int podcastTitle2Index(String title){
        RealmResults<PodcastRealm> list = state_.podcastList_;
        for(int i = 0; i < list.size(); i++) {
            Podcast info = list.get(i);
            if(title.equals(info.getTitle())) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        Log.d(TAG, "onServiceConnected");
        player_ = ((PlayerService.LocalBinder)binder).getService();
        player_.setOnStartMusicListener(this);
        playButton_.setEnabled(true);
        syncPreference(pref_, "ALL");
        //TODO: move to base?
        RealmResults<EpisodeRealm> playlist = player_.getCurrentPlaylist();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        player_.clearOnStartMusicListener();
        player_ = null;
    }

    private void updateSelector(){
        List<String> list = new ArrayList<String>();
        list.add(getString(R.string.selector_all));
        for(PodcastRealm info: state_.podcastList_){
            list.add(info.getTitle());
        }
        //stop loading?
        ArrayAdapter<String> adapter =
            new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
		adapter.setDropDownViewResource(android.support.v7.appcompat.R.layout.support_simple_spinner_dropdown_item);
        selector_.setAdapter(adapter);
    }

    private void filterSelectedPodcast(){
        String title = getFilterPodcastTitle();
        state_.loadRealm(title);
        episodeListView_.hideHeader();
        adapter_.notifyDataSetChanged();
    }
    
    private String getFilterPodcastTitle(){
        if(selector_.getSelectedItemPosition() == 0){
            return null;
        }
        String title = (String)selector_.getSelectedItem();
        return title;
    }
    
    // @Override
    // protected void notifyLatestListChanged(){
    //     adapter_.notifyDataSetChanged();
    // }
    
    // @Override
    // protected void onPodcastListChanged(boolean start) {
    //     Log.d(TAG, "onPodcastListChanged");
    //     List<String> list = new ArrayList<String>();
    //     list.add(getString(R.string.selector_all));
    //     //stop loading?
    //     for(int i = 0; i < state_.podcastList_.size(); i++) {
    //         Podcast info = state_.podcastList_.get(i);
    //         if (info.getEnabled()) {
    //             list.add(info.getTitle());
    //         }
    //     }
    //     ArrayAdapter<String> adapter =
    //         new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
	// 	adapter.setDropDownViewResource(android.support.v7.appcompat.R.layout.support_simple_spinner_dropdown_item);

    //     selector_.setAdapter(adapter);
    //     Resources res = getResources();
    //     boolean doLoad = pref_.getBoolean("load_on_start", res.getBoolean(R.bool.default_load_on_start));
    //     RealmResults<EpisodeRealm> playlist = state_.latestList_;
    //     if ((!start) || doLoad) {
    //         //reload
    //         episodeListView_.startRefresh();
    //     }
    //     else if (playlist != null && ! playlist.isEmpty()) {
    //         //update list by loaded items
    //         filterSelectedPodcast();
    //         episodeListView_.onRefreshComplete(getString(R.string.header_lastupdated) + dateFormat_.format(state_.lastUpdatedDate_));
    //     }
    //     updateUI();
    // }

    // @Override
    // public void notifyOrderChanged(int order){
    //     updatePlaylist();
    //     adapter_.notifyDataSetChanged();
    // }

    // @Override
    // public void onProgressChanged(SeekBar bar, int progress, boolean fromUser){
    //     if(!fromUser){
    //         return;
    //     }
    //     player_.seekTo(progress);
    // }

    // @Override
    // public void onStartTrackingTouch(SeekBar bar){
    //     //nop
    // }

    // @Override
    // public void onStopTrackingTouch(SeekBar bar){
    //     //nop
    // }

    static
    private class EpisodeHolder {
        TextView titleView_;
        TextView timeView_;
        ImageView stateIcon_;
        ImageView episodeIcon_;
        String displayedIconURL_;
    }
}
