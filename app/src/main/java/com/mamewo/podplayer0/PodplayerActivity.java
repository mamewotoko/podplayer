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

import com.mamewo.lib.podcast_parser.BaseGetPodcastTask;
import com.mamewo.lib.podcast_parser.EpisodeInfo;
import com.mamewo.lib.podcast_parser.PodcastInfo;
import com.markupartist.android.widget.PullToRefreshListView;

import com.bumptech.glide.Glide;
import android.support.v7.widget.Toolbar;
import android.support.v7.app.ActionBar;
import android.widget.ImageButton;

public class PodplayerActivity
    extends BasePodplayerActivity
    implements OnClickListener,
    OnLongClickListener,
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
    private List<EpisodeInfo> currentList_;
    //number of items for one screen (small phone)
    static final
    public int EPISODE_BUF_SIZE = 10;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.main);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayShowTitleEnabled(false);

        //debug: check
        // View v = findViewById(R.id.podcast_selector);
        // Log.d(TAG, "spinner class: "+v.getClass().getName());
        //end debug
        
		playButton_ = (ImageButton) findViewById(R.id.play_button);
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
        currentList_ = state_.latestList_;
        adapter_ = new EpisodeAdapter();
        episodeListView_.setAdapter(adapter_);
        //currentPlayPosition_ = (SeekBar) findViewById(R.id.seekbar);
        //currentPlayPosition_.setOnSeekBarChangeListener(this);
    }

    private void updateUI() {
        if(null == player_) {
            return;
        }
        adapter_.notifyDataSetChanged();
        updatePlayButton();
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
        GetPodcastTask task = new GetPodcastTask(limit);
        startLoading(task);
    }

    private void updatePlayButton(){
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
            updatePlayButton();
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
        EpisodeInfo info = (EpisodeInfo)adapter_.getItem(pos-1);
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
        int playPos;
        for(playPos = 0; playPos < state_.latestList_.size(); playPos++) {
            if(state_.latestList_.get(playPos) == info) {
                break;
            }
        }
        if (playPos < 0){
            Log.i(TAG, "playByInfo: info is not found: " + info.url_);
            return false;
        }

        return player_.playNth(playPos);
    }

    //UI is updated in following callback methods
    @Override
    public void onStartMusic(EpisodeInfo info) {
		//setProgressBarIndeterminateVisibility(false);
		//currentPlayPosition_.setMax(player_.getDuration());
		//int pos = player_.getCurrentPositionMsec();
        //currentPlayPosition_.setProgress(pos);
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

    public class EpisodeAdapter
        extends BaseAdapter
    {
        public EpisodeAdapter(){
        }
        
        @Override
        public int getCount(){
            return currentList_.size();
        }

        @Override
        public Object getItem(int position){
            return currentList_.get(position);
        }
        
        @Override
        public long getItemId(int position){
            return position;
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (null == convertView) {
                view = View.inflate(PodplayerActivity.this, R.layout.episode_item, null);
            }
            else {
                view = convertView;
            }
            //TODO: get image url
            EpisodeInfo info = (EpisodeInfo)getItem(position);
            TextView titleView = (TextView)view.findViewById(R.id.episode_title);
            TextView timeView = (TextView)view.findViewById(R.id.episode_time);
            titleView.setText(info.title_);
            timeView.setText(info.getPubdateString());
            ImageView stateIcon = (ImageView)view.findViewById(R.id.play_icon);
            ImageView episodeIcon = (ImageView)view.findViewById(R.id.episode_icon);
            EpisodeInfo current = player_.getCurrentPodInfo();
            if(current != null && current.url_.equals(info.url_)) {
                //cache!
                if(player_.isPlaying()) {
                    stateIcon.setImageResource(R.drawable.ic_play_arrow_white_24dp);
                    stateIcon.setContentDescription(getString(R.string.icon_desc_playing));
                }
                else {
                    stateIcon.setImageResource(R.drawable.ic_pause_white_24dp);
                    stateIcon.setContentDescription(getString(R.string.icon_desc_pausing));
                }
                stateIcon.setVisibility(View.VISIBLE);
            }
            else {
                stateIcon.setVisibility(View.GONE);
            }

            String iconURL = state_.podcastList_.get(info.index_).getIconURL();
            if(showPodcastIcon_ && null != iconURL){
                Glide
                    .with(getApplicationContext())
                    .load(iconURL)
                    .into(episodeIcon);
                episodeIcon.setContentDescription(info.title_);
                episodeIcon.setVisibility(View.VISIBLE);
            }
            else {
                episodeIcon.setContentDescription(getString(R.string.icon_desc_episode_none));
                episodeIcon.setVisibility(View.GONE);
            }
            return view;
        }
    }

    private class GetPodcastTask
        extends BaseGetPodcastTask
    {
        public GetPodcastTask(int limit) {
            super(PodplayerActivity.this, client_, limit, EPISODE_BUF_SIZE);
        }

        @Override
        protected void onProgressUpdate(EpisodeInfo... values){
            for (int i = 0; i < values.length; i++) {
                state_.mergeEpisode(values[i]);
                //adapter_.add(values[i]);
            }
            //Log.d(TAG, "onProgressUpdate");
            filterSelectedPodcast();
            //adapter_.notifyDataSetChanged();
        }

        private void onFinished() {
            if(adapter_.isEmpty()) {
                episodeListView_.setLastUpdated("");
            }
            else {
                DateFormat df = DateFormat.getDateTimeInstance();
                //TODO: change format of date
                state_.lastUpdatedDate_ = new Date();
                episodeListView_.setLastUpdated(getString(R.string.header_lastupdated) + df.format(state_.lastUpdatedDate_));
            }
            setProgressBarIndeterminateVisibility(false);
            episodeListView_.onRefreshComplete();
            episodeListView_.hideHeader();
            loadTask_ = null;
            savePodcastList();
            //dummy
            //sortEpisodeByDate(true);
            //TODO: Sync playlist
            updatePlaylist();
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

    // public void sortEpisodeByDate(boolean latestFirst){
    //     //dummy
    //     Log.d(TAG, "sort by pubdate");
    //     Collections.sort(currentList_, new EpisodeInfo.PubdateComparator());
    //     if(latestFirst){
    //         Collections.reverse(currentList_);
    //     }
    // }
    
    private void filterSelectedPodcast(){
        List<EpisodeInfo> l;
        //TODO: design incremnetal add 
        if(selector_.getSelectedItemPosition() == 0){
            //-1: all

            //TODO: reduce call?
            updatePlaylist();
            l = state_.latestList_;
        }
        else {
            String title = (String)selector_.getSelectedItem();
            int selected = podcastTitle2Index(title);

            l = state_.loadedEpisode_.get(selected);
            if(currentOrder_ == REVERSE_APPEARANCE_ORDER){
                ///XXX provide view
                List<EpisodeInfo> reversed = new ArrayList<EpisodeInfo>();
                for(int i = 0; i < l.size(); i++){
                    reversed.add(l.get(l.size()-1-i));
                }
                l = reversed;
            }
        }
        currentList_ = l;
        //Log.d(TAG, "filterSelectedPodcast: "+ currentList_.size());
        if (! isLoading()) {
            episodeListView_.hideHeader();
        }
        adapter_.notifyDataSetChanged();
    }

    @Override
    public void onItemSelected(AdapterView<?> adapter, View view, int pos, long id) {
        filterSelectedPodcast();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapter) {
        filterSelectedPodcast();
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
		adapter.setDropDownViewResource(android.support.v7.appcompat.R.layout.support_simple_spinner_dropdown_item);

        selector_.setAdapter(adapter);
        Resources res = getResources();
        boolean doLoad = pref.getBoolean("load_on_start", res.getBoolean(R.bool.default_load_on_start));
        List<EpisodeInfo> playlist = state_.latestList_;
        if ((!start) || doLoad) {
            //reload
            episodeListView_.startRefresh();
        }
        else if (playlist != null && ! playlist.isEmpty()) {
            //update list by loaded items
            filterSelectedPodcast();
            DateFormat df = DateFormat.getDateTimeInstance();
            episodeListView_.onRefreshComplete(df.format(state_.lastUpdatedDate_));
        }
        updateUI();
    }

    @Override
    public void notifyOrderChanged(int order){
        updatePlaylist();
        adapter_.notifyDataSetChanged();
    }

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
}
