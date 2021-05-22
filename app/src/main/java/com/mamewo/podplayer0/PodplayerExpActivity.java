package com.mamewo.podplayer0;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

import com.mamewo.podplayer0.parser.BaseGetPodcastTask;
import com.mamewo.podplayer0.db.PodcastRealm;
import com.mamewo.podplayer0.db.EpisodeRealm;
import com.mamewo.podplayer0.db.SimpleQuery;

import static com.mamewo.podplayer0.Const.*;

import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import com.mamewo.podplayer0.util.Log;
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
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.ActionBar;
import io.realm.Realm;
import io.realm.RealmResults;

public class PodplayerExpActivity
    extends BasePodplayerActivity
    implements OnClickListener,
               OnLongClickListener,
               OnItemLongClickListener,
               PlayerService.PlayerStateListener,
               OnChildClickListener,
               SimpleQuery.DataChangeListener
{
    private ImageButton playButton_;
	private ImageView reloadButton_;
    private ImageButton expandButton_;
    private ImageButton collapseButton_;
    private ExpandableListView expandableList_;
    private ExpAdapter adapter_;
    private SimpleQuery currentQuery_;
    
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
        expandableList_ =
                (ExpandableListView) findViewById(R.id.exp_list);
        // Display display = getWindowManager().getDefaultDisplay(); 
        // int width = display.getWidth();
        // expandableList_.setIndicatorBounds(width-50, width);
        expandableList_.setOnItemLongClickListener(this);
        expandableList_.setOnChildClickListener(this);
       
        loadRealm();

        adapter_ = new ExpAdapter();
        expandableList_.setAdapter(adapter_);
        expandButton_ = (ImageButton) findViewById(R.id.expand_button);
        expandButton_.setOnClickListener(this);
        collapseButton_ = (ImageButton) findViewById(R.id.collapse_button);
        collapseButton_.setOnClickListener(this);
    }

    @Override
    public void onStart(){
        super.onStart();
        loadPodcast();
    }
    
    public void loadRealm(){
        //TODO: sort
        boolean skipListened = pref_.getBoolean("skip_listened_episode", getResources().getBoolean(R.bool.default_skip_listened_episode));
        int order = Integer.valueOf(pref_.getString("episode_order", "0"));
        currentQuery_ = new SimpleQuery(null, skipListened, order);
        for(PodcastRealm podcast: currentQuery_.getPodcastList()){
            currentQuery_.getEpisodeList(podcast.getId());
        }
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
        Resources res = getResources();
        Log.d(TAG, "loadPodcast");
        GetPodcastTask task = new GetPodcastTask();
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
        if(current != null && current.getId() == episode.getId()) {
            if(player_.isPlaying()) {
                player_.pauseMusic();
            }
            else {
                if(! player_.restartMusic()){
                    playEpisode(episode);
                }
            }
        }
        else {
            Log.d(TAG, "onChildClick: playByInfo");
            updatePlaylist(null);
            playEpisode(episode);
        }
        return true;
    }

    private void playEpisode(EpisodeRealm episode) {
        updatePlaylist(null);
        //TODO: pass episode id
        player_.playById(episode.getId());
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

            return currentQuery_.getPodcastList().size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            PodcastRealm podcast = currentQuery_.getPodcastList().get(groupPosition);
            int lstsize = currentQuery_.getEpisodeList(podcast.getId()).size();
            if(episodeLimit_ > 0){
                lstsize = Math.min(lstsize, episodeLimit_);
            }
            return lstsize;
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
            RealmResults<PodcastRealm> podcastList = currentQuery_.getPodcastList();
            return podcastList.get(groupPosition);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            PodcastRealm podcast = currentQuery_.getPodcastList().get(groupPosition);
            return currentQuery_.getEpisodeList(podcast.getId()).get(childPosition);
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
            PodcastRealm info = currentQuery_.getPodcastList().get(groupPosition);
            titleView.setText(info.getTitle());
            ImageView iconView = (ImageView)view.findViewById(R.id.episode_icon);
            String iconURL = currentQuery_.getPodcastList().get(groupPosition).getIconURL();
            boolean displayIcon = pref_.getBoolean("display_expand_icon_in_group",
                                                   getResources().getBoolean(R.bool.default_display_expand_icon_in_group));
            if(displayIcon){
                iconView.setVisibility(View.VISIBLE);
                if(null != iconURL){
                    Glide
                        .with(getApplicationContext())
                        .load(iconURL)
                        .into(iconView);
                }
            }
            else {
                iconView.setVisibility(View.GONE);
            }
            int childNum = getChildrenCount(groupPosition);
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
                holder.listenedView_ = (TextView)view.findViewById(R.id.listened_time);
                holder.displayedIconURL_ = null;
                view.setTag(holder);
            }
            else {
                view = convertView;
                holder = (EpisodeHolder)view.getTag();
            }
            EpisodeRealm episode = (EpisodeRealm)getChild(groupPosition, childPosition);
            holder.titleView_.setText(episode.getTitle());
            holder.timeView_.setText(getResources().getString(R.string.published_date)+" "+episode.getPubdateStr(dateFormat_));

            if(null != episode.getListened()){
                holder.listenedView_.setText(getResources().getString(R.string.listened_date)
                                             +" "+dateFormat_.format(episode.getListened()));
                holder.listenedView_.setVisibility(View.VISIBLE);
            }
            else {
                holder.listenedView_.setVisibility(View.GONE);
            }
            
            if(player_ == null){
                holder.stateIcon_.setVisibility(View.GONE);
            }
            else {
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
        public GetPodcastTask() {
            super(PodplayerExpActivity.this, client_, -1);
        }

        @Override
        protected void onProgressUpdate(String... values){
            updateUI();
        }

        private void onFinished(){
            loadTask_ = null;
            reloadButton_.setContentDescription(getResources().getString(R.string.action_reload));
            reloadButton_.setImageResource(R.drawable.ic_sync_white_24dp);
            state_.lastUpdatedDate_ = new Date();
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
    public boolean onItemLongClick(AdapterView<?> adapter, View view, int pos, long id) {
        Resources res = getResources();
        boolean enableLongClick = pref_.getBoolean("enable_long_click", res.getBoolean(R.bool.default_enable_long_click));
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

    @Override
    public void onStartLoadingMusic(long episodeId) {
        updateUI();
    }

    @Override
    public void onStartMusic(long episodeId) {
        updateUI();
    }

    @Override
    public void onCompleteMusic(long episodeId) {
        //TODO: log
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
        boolean doLoad = pref_.getBoolean("load_on_start", getResources().getBoolean(R.bool.default_load_on_start));
        if(doLoad && null == state_.lastUpdatedDate_){
            loadPodcast();
        }
        updateUI();
        boolean expandInDefault = pref_.getBoolean("expand_in_default", getResources().getBoolean(R.bool.default_expand_in_default));
        if (expandInDefault) { 
            expandOrCollapseAll(true);
        }
        adapter_.notifyDataSetChanged();
    }

    @Override
    public void notifyEpisodeListAllChanged(RealmResults<EpisodeRealm> results){
        Log.d(TAG, "exp notifyEpisodeListAllChanged");
        adapter_.notifyDataSetChanged();
    }

    @Override
    public void notifyEpisodeListGroupChanged(long podcastId, RealmResults<EpisodeRealm> results){
        Log.d(TAG, "exp notifyEpisodeListGroupChanged");
        adapter_.notifyDataSetChanged();
    }

    @Override
    public void notifyQuerySettingChanged(){
        loadRealm();
        adapter_.notifyDataSetChanged();
    }

    @Override
    public void notifyUISettingChanged(){
        adapter_.notifyDataSetChanged();
    }
    
    static
    private class EpisodeHolder {
        TextView titleView_;
        TextView timeView_;
        TextView listenedView_;
        ImageView stateIcon_;
        ImageView episodeIcon_;
        String displayedIconURL_;
    }
}
