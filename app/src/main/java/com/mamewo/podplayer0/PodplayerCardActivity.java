package com.mamewo.podplayer0;

import java.util.List;
import java.util.ArrayList;

import android.util.Log;
import android.os.Bundle;
import android.os.IBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.LinearLayoutManager;
import android.view.ViewGroup;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.preference.PreferenceManager;
import android.content.res.Resources;
import android.widget.Spinner;
import android.widget.ImageButton;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

import com.mamewo.lib.podcast_parser.BaseGetPodcastTask;
import com.mamewo.lib.podcast_parser.EpisodeInfo;
import com.mamewo.lib.podcast_parser.PodcastInfo;

import com.bumptech.glide.Glide;
import static com.mamewo.podplayer0.Const.*;
import android.support.v7.widget.Toolbar;
import android.support.v7.app.ActionBar;

public class PodplayerCardActivity
    extends BasePodplayerActivity
    implements OnClickListener,
               OnItemSelectedListener,
               PlayerService.PlayerStateListener
{
    static
    private int EPISODE_BUF_SIZE = 10;
    private RecyclerView recyclerView_;
    private LinearLayoutManager layoutManager_;
    private EpisodeAdapter adapter_;
    private Spinner selector_;
    private ImageButton playButton_;
    //filtered list
    private List<EpisodeInfo> currentList_;
        
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.card_main);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayShowTitleEnabled(false);

		playButton_ = (ImageButton) findViewById(R.id.play_button);
        playButton_.setOnClickListener(this);
        playButton_.setEnabled(false);

        recyclerView_ = (RecyclerView)findViewById(R.id.recycler_view);
        layoutManager_ = new LinearLayoutManager(this);
        recyclerView_.setLayoutManager(layoutManager_);

        selector_ = (Spinner) findViewById(R.id.podcast_selector);
        selector_.setOnItemSelectedListener(this);
        
        SharedPreferences pref=
            PreferenceManager.getDefaultSharedPreferences(this);
        syncPreference(pref, "ALL");

        currentList_ = state_.latestList_;
        adapter_ = new EpisodeAdapter();
        recyclerView_.setAdapter(adapter_);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        player_ = ((PlayerService.LocalBinder)binder).getService();
        player_.setOnStartMusicListener(this);
        playButton_.setEnabled(true);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        player_.clearOnStartMusicListener();
        player_ = null;
    }

    private int podcastTitle2Index(String title){
        List<PodcastInfo> list = state_.podcastList_;
        for(int i = 0; i < list.size(); i++) {
            PodcastInfo info = list.get(i);
            if(title.equals(info.getTitle())) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onItemSelected(AdapterView<?> adapter, View view, int pos, long id) {
        filterSelectedPodcast();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapter) {
        filterSelectedPodcast();
    }
    
    //TODO: tuning
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
        //TODO: selected item is removed
        currentList_ = l;
        adapter_.notifyDataSetChanged();
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
            if (info.getEnabled()) {
                list.add(info.getTitle());
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
            //episodeListView_.startRefresh();
            loadPodcast();
        }
        else if (playlist != null && ! playlist.isEmpty()) {
            //update list by loaded items
            filterSelectedPodcast();
        }
        //updateUI();
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
        }
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
    
    public void loadPodcast(){
        SharedPreferences pref=
                PreferenceManager.getDefaultSharedPreferences(this);
        Resources res = getResources();
        int limit = Integer.valueOf(pref.getString("episode_limit", res.getString(R.string.default_episode_limit)));
        startLoading(new GetPodcastTask(limit));
    }
    
    @Override
    public void notifyOrderChanged(int order){
        //TODO
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
            Log.i(TAG, "playByInfo: info is not found: " + info.getURL());
            return false;
        }

        return player_.playNth(playPos);
    }

    public void updateUI(){
        Log.d(TAG, "updateUI");
        updatePlayButton();
        adapter_.notifyDataSetChanged();
    }
    
    @Override
    public void onStartMusic(EpisodeInfo info) {
		//setProgressBarIndeterminateVisibility(false);
		//currentPlayPosition_.setMax(player_.getDuration());
		//int pos = player_.getCurrentPositionMsec();
        //currentPlayPosition_.setProgress(pos);
        //timer
        Log.d(TAG, "onStartMusic");
        updateUI();
    }

    @Override
    public void onStartLoadingMusic(EpisodeInfo info) {
        Log.d(TAG, "onStartLoadingMusic");
        updateUI();
    }

    @Override
    public void onStopMusic(int mode) {
        Log.d(TAG, "onStopMusic");
        updateUI();
    }

    public class ItemClickListener
        implements View.OnClickListener
    {
        private EpisodeInfo info_;
        
        public ItemClickListener(EpisodeInfo info){
            info_ = info;
        }

        @Override
        public void onClick(View view){
            Log.d(TAG, "onClick: "+view.toString());
            EpisodeInfo info = info_;
            EpisodeInfo current = player_.getCurrentPodInfo();
            if(null != current && current.getURL().equals(info.getURL())) {
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
                playByInfo(info);
            }
        }
    }
    
    private class EpisodeAdapter
        extends RecyclerView.Adapter<EpisodeHolder>
    {
        public EpisodeAdapter(){
        }
        
        @Override
        public EpisodeHolder onCreateViewHolder(ViewGroup parent, int viewType){
            View view = LayoutInflater
                .from(PodplayerCardActivity.this)
                .inflate(R.layout.episode_card_item, parent, false);
            EpisodeHolder holder = new EpisodeHolder(view);
            return holder;
        }

        @Override
        public void onBindViewHolder(EpisodeHolder holder, int position){
            final EpisodeInfo episode = currentList_.get(position);
            holder.titleView_.setText(episode.getTitle());
            holder.timeView_.setText(episode.getPubdateString());
            holder.container_.setOnClickListener(new ItemClickListener(episode));

            EpisodeInfo current = player_.getCurrentPodInfo();
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
            String iconURL = state_.podcastList_.get(episode.getIndex()).getIconURL();
            //TODO: add showicon setting
            if(null != iconURL){
                //TODO: check previous icon url
                String displayedIconURL = holder.displayedIconURL_;
                if(View.GONE == holder.episodeIcon_.getVisibility()
                   || null == displayedIconURL
                   || !displayedIconURL.equals(iconURL)){
                    Glide
                        .with(PodplayerCardActivity.this)
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
        }

        @Override
        public int getItemCount(){
            return currentList_.size();
        }
    }

    static
    private class EpisodeHolder
        extends RecyclerView.ViewHolder
    {
        public TextView titleView_;
        public TextView timeView_;
        public ImageView stateIcon_;
        public ImageView episodeIcon_;
        public View container_;
        public String displayedIconURL_;

        public EpisodeHolder(View view){
            super(view);
            titleView_ = (TextView)view.findViewById(R.id.episode_title);
            timeView_ = (TextView)view.findViewById(R.id.episode_time);
            stateIcon_ =  (ImageView)view.findViewById(R.id.play_icon);
            episodeIcon_ = (ImageView)view.findViewById(R.id.episode_icon);
            container_ = view;
            displayedIconURL_ = null;
        }
    }

    private class GetPodcastTask
        extends BaseGetPodcastTask
    {
        final
        private int UPDATE_THRES = 10;
        private int count_;
        private int displayedCount_;
        
        public GetPodcastTask(int limit) {
            super(PodplayerCardActivity.this, client_, limit, EPISODE_BUF_SIZE);
            count_ = 0;
        }
        
        @Override
        protected void onProgressUpdate(EpisodeInfo... values){
            Log.d(TAG, "loading card:");
            for (int i = 0; i < values.length; i++) {
                state_.mergeEpisode(values[i]);
            }
            count_ += values.length;
            if(displayedCount_ - count_ > UPDATE_THRES){
                updatePlaylist();
                updateUI();
                displayedCount_ = count_;
            }
        }
        
        private void onFinished() {
            Log.d(TAG, "onFinished");
            loadTask_ = null;
            //updateUI();
            updatePlaylist();
            adapter_.notifyDataSetChanged();
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
}
