package com.mamewo.podplayer0;

import java.util.ArrayList;
import java.util.List;

import com.mamewo.podplayer0.parser.BaseGetPodcastTask;
import com.mamewo.podplayer0.db.PodcastRealm;
import com.mamewo.podplayer0.db.EpisodeRealm;

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
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageView;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;
import android.widget.ImageButton;

import com.bumptech.glide.Glide;
import android.support.v7.widget.Toolbar;
import android.support.v7.app.ActionBar;
import io.realm.Realm;
import io.realm.RealmResults;

public class PodplayerExpActivity
    extends BasePodplayerActivity
    implements OnClickListener,
    OnLongClickListener,
    OnItemLongClickListener,
    PlayerService.PlayerStateListener,
    OnChildClickListener
{
    private ImageButton playButton_;
	private ImageView reloadButton_;
    private ImageButton expandButton_;
    private ImageButton collapseButton_;
    private ExpandableListView expandableList_;
    private ExpAdapter adapter_;
    //private int currentOrder_;
    //private List<Integer> filteredItemIndex_;
    private List<RealmResults<EpisodeRealm>> groupList_;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.expandable_main);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayShowTitleEnabled(false);
        
		reloadButton_ = (ImageView) findViewById(R.id.reload_button);
        reloadButton_.setOnClickListener(this);
        playButton_ = (ImageButton) findViewById(R.id.play_button);
        playButton_.setOnClickListener(this);
        playButton_.setOnLongClickListener(this);
        playButton_.setEnabled(false);
        //XXX filteredItemIndex_ -> adapter_
        //filteredItemIndex_ = new ArrayList<Integer>();
        expandableList_ =
                (ExpandableListView) findViewById(R.id.exp_list);
        expandableList_.setOnItemLongClickListener(this);
        expandableList_.setOnChildClickListener(this);
        Realm realm = Realm.getDefaultInstance();
        groupList_ = new ArrayList<RealmResults<EpisodeRealm>>();
        for(int i = 0; i < state_.podcastList_.size(); i++){
            RealmResults<EpisodeRealm> result = realm.where(EpisodeRealm.class).equalTo("podcast.id", state_.podcastList_.get(i).getId()).findAll();
            groupList_.add(result);
            //TODO: add listener
        }
        adapter_ = new ExpAdapter();
        expandableList_.setAdapter(adapter_);
        expandButton_ = (ImageButton) findViewById(R.id.expand_button);
        expandButton_.setOnClickListener(this);
        collapseButton_ = (ImageButton) findViewById(R.id.collapse_button);
        collapseButton_.setOnClickListener(this);
    }

    private void updateUI() {
        if(null == player_) {
            return;
        }
        adapter_.notifyDataSetChanged();
        if(player_.isPlaying()){
            playButton_.setContentDescription(getResources().getString(R.string.pause));
            playButton_.setImageResource(R.drawable.ic_pause_white_24dp);
        }
        else {
            playButton_.setContentDescription(getResources().getString(R.string.play));
            playButton_.setImageResource(R.drawable.ic_play_arrow_white_24dp);
        }
    }

    //must be called from UI thread
    private void loadPodcast(){
        if (isLoading()) {
            Log.d(TAG, "Already loading");
            return;
        }
        reloadButton_.setContentDescription(getResources().getString(R.string.action_abort));
		reloadButton_.setImageResource(R.drawable.ic_clear_white_24dp);
        updateUI();
        SharedPreferences pref=
                PreferenceManager.getDefaultSharedPreferences(this);
        Resources res = getResources();
        int limit = Integer.valueOf(pref.getString("episode_limit", res.getString(R.string.default_episode_limit)));
        Log.d(TAG, "loadPodcast");
        GetPodcastTask task = new GetPodcastTask(limit);
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
                updatePlaylist(null);
                if(! player_.restartMusic()) {
                    player_.playMusic();
                }
            }
            //playButton_.setChecked(player_.isPlaying());
            updateUI();
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
        Log.d(TAG, "onServiceConnected: exp");
        player_ = ((PlayerService.LocalBinder)binder).getService();
        player_.setOnStartMusicListener(this);
        playButton_.setEnabled(true);
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
        EpisodeRealm episode = (EpisodeRealm)adapter_.getChild(groupPosition, childPosition);
        EpisodeRealm current = player_.getCurrentPodInfo();
        Log.d(TAG, "onChildClick");
        Log.d(TAG, "clicked: "+episode);
        Log.d(TAG, "current: "+current);
        if(current != null && current.getId() == current.getId()) {
            if(player_.isPlaying()) {
                player_.pauseMusic();
            }
            else {
                if(! player_.restartMusic()){
                    playByInfo(episode);
                }
            }
        }
        else {
            Log.d(TAG, "onChildClick: playByInfo");
            updatePlaylist(null);
            playByInfo(episode);
        }
        return true;
    }

    private void playByInfo(EpisodeRealm episode) {
        //umm...
        if(state_.latestList_ == null){
            return;
        }
        int playPos = -1;
        //skip! use list size
        for(int pos = 0; pos < state_.latestList_.size(); pos++) {
            if(state_.latestList_.get(pos) == episode) {
                playPos = pos;
                break;
            }
        }
        if (playPos < 0){
            Log.i(TAG, "playByInfo: info is not found: " + episode.getURL());
            return;
        }
        player_.playNth(playPos);
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
            return groupList_.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            //return state_.loadedEpisode_.get(filteredItemIndex_.get(groupPosition)).size();
            return groupList_.get(groupPosition).size();
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
            return groupList_.get(groupPosition);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            //List<EpisodeInfo> group = state_.loadedEpisode_.get(filteredItemIndex_.get(groupPosition));
            // int pos;
            // switch(currentOrder_){
            // case REVERSE_APPEARANCE_ORDER:
            //     pos = group.size()-1-childPosition;
            //     break;
            // case APPEARANCE_ORDER:
            //     //fall through
            // default:
            //     pos = childPosition;
            //     break;
            // }
            // return group.get(pos);
            return groupList_.get(groupPosition).get(childPosition);
        }
        
        @Override
        public View getGroupView(int groupPosition,
                                 boolean isExpanded,
                                 View convertView,
                                 ViewGroup parent)
        {
            View view;
            if(convertView == null){
                view = View.inflate(PodplayerExpActivity.this, R.layout.expandable_list_item2, null);
            }
            else {
                view = convertView;
            }
            TextView titleView = (TextView)view.findViewById(R.id.text1);
            TextView countView = (TextView)view.findViewById(R.id.text2);
            PodcastRealm info = state_.podcastList_.get(groupPosition);
            titleView.setText(info.getTitle());
            int childNum = groupList_.get(groupPosition).size();
            String numStr;
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
            View view;
            EpisodeHolder holder;
            if(convertView == null){
                view = View.inflate(PodplayerExpActivity.this, R.layout.episode_item, null);
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
            EpisodeRealm episode = (EpisodeRealm)getChild(groupPosition, childPosition);
            holder.titleView_.setText(episode.getTitle());
            holder.timeView_.setText(episode.getPubdateStr(dateFormat_));
            EpisodeRealm current = player_.getCurrentPodInfo();
            if(current != null && current.getURL().equals(episode.getURL())) {
                //cache!
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

            //TODO: use string or uri
            String iconURL = episode.getPodcast().getIconURL();
            //String iconURL = state_.podcastList_.get(episode.getIndex()).getIconURL();
            if(showPodcastIcon_ && null != iconURL){
                //to avoid image flicker
                String displayedIconURL = holder.displayedIconURL_;
                if(View.GONE == holder.episodeIcon_.getVisibility()
                   || null == displayedIconURL
                   || !displayedIconURL.equals(iconURL)){
                    Glide
                        .with(getApplicationContext())
                        .load(iconURL)
                        .into(holder.episodeIcon_);
                }
                holder.episodeIcon_.setVisibility(View.VISIBLE);
            }
            else {
                Glide.clear(holder.episodeIcon_);
                holder.episodeIcon_.setVisibility(View.GONE);
            }
            holder.displayedIconURL_ = iconURL;
            return view;
        }
    }

    @Override
    public void onStopMusic(int mode) {
        updateUI();
    }
    // end of callback methods

    private class GetPodcastTask
        extends BaseGetPodcastTask
    {
        public GetPodcastTask(int limit) {
            super(PodplayerExpActivity.this, client_, limit);
        }

        @Override
        protected void onProgressUpdate(String... values){
            updateUI();
        }

        private void onFinished(){
            loadTask_ = null;
            reloadButton_.setContentDescription(getResources().getString(R.string.action_reload));
			reloadButton_.setImageResource(R.drawable.ic_sync_white_24dp);            
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

        EpisodeRealm episode = (EpisodeRealm)adapter_.getChild(groupPosition, childPosition);
        if(episode.getLink() == null){
            return true;
        }
        //TODO: use link of podcast.xml (global one)
        //TODO: display url before connect
        //episode.link refers audio file...
        Intent i =
            new Intent(Intent.ACTION_VIEW, Uri.parse(episode.getLink()));
        startActivity(i);
        return true;
    }

    //TODO: fetch current playing episode to update currentPodInfo
    // @Override
    // protected void onPodcastListChanged(boolean start) {
    //     filteredItemIndex_.clear();
    //     for(int i = 0; i < state_.podcastList_.size(); i++) {
    //         if(state_.podcastList_.get(i).getEnabled()){
    //             filteredItemIndex_.add(i);
    //         }
    //     }
    //     SharedPreferences pref =
    //             PreferenceManager.getDefaultSharedPreferences(this);
    //     Resources res = getResources();
    //     boolean expandInDefault = pref.getBoolean("expand_in_default", res.getBoolean(R.bool.default_expand_in_default));
    //     if (expandInDefault) { 
    //         expandOrCollapseAll(true);
    //     }
    //     expandableList_.setOnChildClickListener(this);
    //     boolean doLoad = pref.getBoolean("load_on_start", res.getBoolean(R.bool.default_load_on_start));
    //     updateUI();
    //     //List<EpisodeInfo> playlist = state_.loadedEpisode_;
    //     if ((!start) || doLoad) {
    //         loadPodcast();
    //     }
    // }

    @Override
    public void onStartLoadingMusic(long episodeId) {
        updateUI();
    }

    @Override
    public void onStartMusic(long episodeId) {
        updateUI();
    }

    // @Override
    // protected void notifyLatestListChanged(){
    //     adapter_.notifyDataSetChanged();
    // }
    
    // @Override
    // public void notifyOrderChanged(int order){
    //     updatePlaylist();
    //     adapter_.notifyDataSetChanged();
    // }

    @Override
    public void notifyPodcastListChanged(RealmResults<PodcastRealm> results){
        groupList_.clear();
        Realm realm = Realm.getDefaultInstance();

        for(int i = 0; i < state_.podcastList_.size(); i++){
            RealmResults<EpisodeRealm> result = realm.where(EpisodeRealm.class).equalTo("podcast.id", state_.podcastList_.get(i).getId()).findAll();
            groupList_.add(result);
            //TODO: add listener
        }
        adapter_.notifyDataSetChanged();
    }

    @Override
    public void notifyLatestListChanged(RealmResults<EpisodeRealm> results){
        adapter_.notifyDataSetChanged();
    }
    
    static
    private class EpisodeHolder {
        TextView titleView_;
        TextView timeView_;
        ImageView stateIcon_;
        ImageView episodeIcon_;
        String displayedIconURL_;
    }
}
