package com.mamewo.podplayer0;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.URL;

import com.mamewo.lib.podcast_parser.BaseGetPodcastTask;
import com.mamewo.lib.podcast_parser.EpisodeInfo;
import com.mamewo.lib.podcast_parser.PodcastInfo;

import static com.mamewo.podplayer0.Const.*;

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
import android.widget.ExpandableListAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;
import android.widget.ImageButton;

import com.bumptech.glide.Glide;
import android.support.v7.widget.Toolbar;
import android.support.v7.app.ActionBar;

public class PodplayerExpActivity
    extends BasePodplayerActivity
    implements OnClickListener,
    OnLongClickListener,
    ServiceConnection,
    OnItemLongClickListener,
    PlayerService.PlayerStateListener,
    OnChildClickListener
{
    private ImageButton playButton_;
<<<<<<< master
    private ImageButton reloadButton_;
=======
	private ImageView reloadButton_;
>>>>>>> HEAD~32
    private ImageButton expandButton_;
    private ImageButton collapseButton_;
    private ExpandableListView expandableList_;
    private ExpAdapter adapter_;
    //private int currentOrder_;
    private List<Integer> filteredItemIndex_;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, this, PodplayerExpActivity.class);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.expandable_main);
<<<<<<< master
        reloadButton_ = (ImageButton) findViewById(R.id.reload_button);
=======

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setLogo(R.drawable.ic_status);
        actionbar.setDisplayShowTitleEnabled(false);
        
		reloadButton_ = (ImageView) findViewById(R.id.reload_button);
>>>>>>> HEAD~32
        reloadButton_.setOnClickListener(this);
        playButton_ = (ImageButton) findViewById(R.id.play_button);
        playButton_.setOnClickListener(this);
        playButton_.setOnLongClickListener(this);
        playButton_.setEnabled(false);
        //XXX filteredItemIndex_ -> adapter_
        filteredItemIndex_ = new ArrayList<Integer>();
        expandableList_ =
                (ExpandableListView) findViewById(R.id.exp_list);
        expandableList_.setOnItemLongClickListener(this);
        adapter_ = new ExpAdapter();
        expandableList_.setAdapter(adapter_);
        expandButton_ = (ImageButton) findViewById(R.id.expand_button);
        expandButton_.setOnClickListener(this);
        collapseButton_ = (ImageButton) findViewById(R.id.collapse_button);
        collapseButton_.setOnClickListener(this);
        //groupData_ = new ArrayList<Map<String, String>>();
        //childData_ = new ArrayList<List<Map<String, Object>>>();
    }

    private void updatePlayButton(){
        if(player_.isPlaying()){
            playButton_.setImageResource(android.R.drawable.ic_media_pause);
        }
        else {
            playButton_.setImageResource(android.R.drawable.ic_media_play);
        }
    }


    private void updateUI() {
        if(null == player_) {
            return;
        }
        adapter_.notifyDataSetChanged();
<<<<<<< master
        if(player_.isPlaying()){
            playButton_.setContentDescription(getResources().getString(R.string.pause));
            playButton_.setImageResource(R.drawable.ic_pause_white_48dp);
        }
        else {
            playButton_.setContentDescription(getResources().getString(R.string.play));
            playButton_.setImageResource(R.drawable.ic_play_arrow_white_48dp);
        }
=======
		//playButton_.setChecked(player_.isPlaying());
        updatePlayButton();
>>>>>>> HEAD~32
    }

    // public void onSharedPreferenceChanged(SharePreference pref, String key){
    //     super.onSharedPreferenceChanged(pref, key);
    //     //TODO: move to const or string
    //     if("episode_limit".equals(key)){
    //         currentOrder_ = Integer.valueOf(pref.getString("episode_order", "0"));
    //         adapter_.notifyDataSetChanged();
    //     }
    // }

    //must be called from UI thread
    private void loadPodcast(){
        if (isLoading()) {
            Log.d(TAG, "Already loading");
            return;
        }
        reloadButton_.setImageResource(R.drawable.ic_clear_white_48dp);
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
            //playButton_.setChecked(player_.isPlaying());
<<<<<<< master
            updateUI();
=======
            updatePlayButton();
>>>>>>> HEAD~32
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
            for (int i = 0; i < adapter_.getGroupCount(); i++) {
                expandOrCollapseAll(true);
            }
        }
        else if (view == collapseButton_) {
            for (int i = 0; i < adapter_.getGroupCount(); i++) {
                expandOrCollapseAll(false);
            }
        }
    }

    @Override
    public boolean onLongClick(View view) {
        if (view == playButton_) {
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
        for (int i = 0; i < adapter_.getGroupCount(); i++) {
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
        // List<EpisodeInfo> playlist = player_.getCurrentPlaylist();
        // if (null != playlist) {
        //     state_.loadedEpisode_ = playlist;
        // }
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
    public boolean onChildClick(ExpandableListView parent,
                                View v,
                                int groupPosition,
                                int childPosition,
                                long id)
    {
        EpisodeInfo info = (EpisodeInfo)adapter_.getChild(groupPosition, childPosition);
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
        if(state_.latestList_ == null){
            return;
        }
        int playPos = -1;
        //skip! use list size
        for(int pos = 0; pos < state_.latestList_.size(); pos++) {
            if(state_.latestList_.get(pos) == info) {
                playPos = pos;
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
        extends BaseExpandableListAdapter
    {
        public ExpAdapter(){
            
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

        @Override
        public int getGroupCount() {
            return filteredItemIndex_.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return state_.loadedEpisode_.get(filteredItemIndex_.get(groupPosition)).size();
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }
        
        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public Object getGroup(int groupPosition){
            return state_.podcastList_.get(filteredItemIndex_.get(groupPosition));
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            List<EpisodeInfo> group = state_.loadedEpisode_.get(filteredItemIndex_.get(groupPosition));
            int pos;
            switch(currentOrder_){
            case REVERSE_APPEARANCE_ORDER:
                pos = group.size()-1-childPosition;
                break;
            case APPEARANCE_ORDER:
                //fall through
            default:
                pos = childPosition;
                break;
            }
            return group.get(pos);
        }
        
        @Override
        public View getGroupView(int groupPosition,
                                 boolean isExpanded,
                                 View convertView,
                                 ViewGroup parent)
        {
            View view = convertView;
            if(view == null){
                view = View.inflate(PodplayerExpActivity.this, R.layout.expandable_list_item2, null);
            }
            TextView titleView = (TextView)view.findViewById(R.id.text1);
            TextView countView = (TextView)view.findViewById(R.id.text2);
            PodcastInfo info = state_.podcastList_.get(filteredItemIndex_.get(groupPosition));
            titleView.setText(info.title_);
            int childNum = state_.loadedEpisode_.get(filteredItemIndex_.get(groupPosition)).size();
            String numStr = "";
            if (childNum <= 1) {
                //TODO: localize
                numStr = childNum + " item";
            }
            else {
                numStr = childNum + " items";
            }
            countView.setText(numStr);
            return view;
        }

        @Override
        public View getChildView(int groupPosition,
                                 int childPosition,
                                 boolean isLastChild,
                                 View convertView,
                                 ViewGroup parent)
        {
            View view = convertView;
            if(convertView == null){
                view = View.inflate(PodplayerExpActivity.this, R.layout.episode_item, null);
            }
            EpisodeInfo info = (EpisodeInfo)getChild(groupPosition, childPosition);
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

            //TODO: use string or uri
            String iconURL = state_.podcastList_.get(info.index_).getIconURL();
            if(showPodcastIcon_ && null != iconURL){
                //episodeIcon.setImageDrawable(state_.podcastList_.get(info.index_).icon_);
                Glide
                    .with(PodplayerExpActivity.this)
                    .load(state_.podcastList_.get(info.index_).iconURL_)
                    .into(episodeIcon);
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

    private class GetPodcastTask
        extends BaseGetPodcastTask
    {
        public GetPodcastTask(int limit, int timeoutSec, boolean getIcon) {
            super(PodplayerExpActivity.this, limit, timeoutSec, getIcon);
        }

        @Override
        protected void onProgressUpdate(EpisodeInfo... values){
            for (EpisodeInfo info: values) {
                //state_.loadedEpisode_.add(info);
                state_.mergeEpisode(info);
            }
            updateUI();
        }

        private void onFinished(){
            loadTask_ = null;
            setProgressBarIndeterminateVisibility(false);
            //TODO: merge playlist
            updatePlaylist();
            reloadButton_.setImageResource(R.drawable.ic_sync_white_48dp);
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
        if(ExpandableListView.getPackedPositionType(id) != ExpandableListView.PACKED_POSITION_TYPE_CHILD){
            return false;
        }
        int groupPosition = ExpandableListView.getPackedPositionGroup(id);
        int childPosition = ExpandableListView.getPackedPositionChild(id);

        EpisodeInfo info = (EpisodeInfo)adapter_.getChild(groupPosition, childPosition);
        if(info.link_ == null){
            return true;
        }
        //TODO: use link of podcast.xml (global one)
        //TODO: display url before connect
        //episode.link refers audio file...
        Intent i =
            new Intent(Intent.ACTION_VIEW, Uri.parse(info.link_));
        startActivity(i);
        return true;
    }

    //TODO: fetch current playing episode to update currentPodInfo
    @Override
    protected void onPodcastListChanged(boolean start) {
        filteredItemIndex_.clear();
        for(int i = 0; i < state_.podcastList_.size(); i++) {
            if(state_.podcastList_.get(i).enabled_){
                filteredItemIndex_.add(i);
            }
        }
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
        //List<EpisodeInfo> playlist = state_.loadedEpisode_;
        if ((!start) || doLoad) {
            loadPodcast();
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

    @Override
    public void notifyOrderChanged(int order){
        updatePlaylist();
        adapter_.notifyDataSetChanged();
    }
}
