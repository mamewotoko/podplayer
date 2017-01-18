package com.mamewo.podplayer0;

import java.util.List;
import java.util.ArrayList;

import com.mamewo.podplayer0.util.Log;
import android.os.Bundle;
import android.os.IBinder;

import android.content.ComponentName;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.DialogInterface;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.LinearLayoutManager;
import android.view.ViewGroup;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import android.app.AlertDialog;
import android.app.Dialog;

import com.mamewo.podplayer0.parser.BaseGetPodcastTask;
import com.mamewo.podplayer0.parser.Podcast;

import com.mamewo.podplayer0.db.PodcastRealm;
import com.mamewo.podplayer0.db.EpisodeRealm;
import com.mamewo.podplayer0.db.SimpleQuery;

import io.realm.RealmResults;
import io.realm.Realm;
import io.realm.RealmChangeListener;

import com.bumptech.glide.Glide;
import static com.mamewo.podplayer0.Const.*;
import android.support.v7.widget.Toolbar;
import android.support.v7.app.ActionBar;

public class PodplayerCardActivity
    extends BasePodplayerActivity
    implements OnClickListener,
               OnItemSelectedListener,
               PlayerService.PlayerStateListener,
               SimpleQuery.DataChangeListener
{
    static
    private int EPISODE_BUF_SIZE = 10;
    static final
    public int SHARE_EPISODE_DIALOG = 100;
    static
    public String[] LOCAL_SHARE_OPTIONS = { "Twitter", "Mail" };
    private RecyclerView recyclerView_;
    private LinearLayoutManager layoutManager_;
    private EpisodeAdapter adapter_;
    private Spinner selector_;
    private ImageButton playButton_;
    //filtered list
    private SimpleQuery currentQuery_;
    
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

        selector_ = (Spinner) findViewById(R.id.podcast_selector);
        selector_.setOnItemSelectedListener(this);

        boolean skipListened = pref_.getBoolean("skip_listened_episode", getResources().getBoolean(R.bool.default_skip_listened_episode));
        int order = Integer.valueOf(pref_.getString("episode_order", "0"));

        loadRealm();
        recyclerView_ = (RecyclerView)findViewById(R.id.recycler_view);
        adapter_ = new EpisodeAdapter();
        recyclerView_.setAdapter(adapter_);

        layoutManager_ = new LinearLayoutManager(this);
        recyclerView_.setLayoutManager(layoutManager_);
    }

    @Override
    public void onStart(){
        super.onStart();
        updateSelector();
        loadPodcast();
    }
    
    @Override
    public void notifyPodcastListChanged(RealmResults<PodcastRealm> results){
        updateSelector();
        boolean doLoad = pref_.getBoolean("load_on_start", getResources().getBoolean(R.bool.default_load_on_start));
        if(doLoad && (null == state_.lastUpdatedDate_ || adapter_.getItemCount() == 0)){
            loadPodcast();
        }
        adapter_.notifyDataSetChanged();
    }

    @Override
    public void notifyEpisodeListAllChanged(RealmResults<EpisodeRealm> results){
        adapter_.notifyDataSetChanged();
    }

    @Override
    public void notifyEpisodeListGroupChanged(long podcastId, RealmResults<EpisodeRealm> results){
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
    
    //called initialize time or rotate screen
    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        player_ = ((PlayerService.LocalBinder)binder).getService();
        player_.setOnStartMusicListener(this);
        playButton_.setEnabled(true);
        SharedPreferences pref=
            PreferenceManager.getDefaultSharedPreferences(this);
        syncPreference(pref, "ALL");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.d(TAG, "onServiceDisconnected");
        player_.clearOnStartMusicListener();
        player_ = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.cardmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean handled = super.onOptionsItemSelected(item);
        if(handled){
            return true;
        }
        switch(item.getItemId()){
        case R.id.reload_menu:
            loadPodcast();
            handled = true;
            break;
        default:
            break;
        }
        return handled;
    }

    private void updateSelector(){
        List<String> list = new ArrayList<String>();
        list.add(getString(R.string.selector_all));
        for(PodcastRealm info: currentQuery_.getPodcastList()){
            list.add(info.getTitle());
        }
        //stop loading?
        ArrayAdapter<String> adapter =
            new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
		adapter.setDropDownViewResource(android.support.v7.appcompat.R.layout.support_simple_spinner_dropdown_item);
        selector_.setAdapter(adapter);
    }

    private void filterSelectedPodcast(){
        loadRealm();
        adapter_.notifyDataSetChanged();
    }

    private String getFilterPodcastTitle(){
        if(selector_.getSelectedItemPosition() == 0){
            return null;
        }
        String title = (String)selector_.getSelectedItem();
        return title;
    }

    //filter
    public RealmResults<EpisodeRealm> getCurentEpisodeList(){
        int n = selector_.getSelectedItemPosition();
        if(n == 0 || n < 0){
            return currentQuery_.getEpisodeList();
        }
        PodcastRealm info = currentQuery_.getPodcastList().get(n-1);
        return currentQuery_.getEpisodeList(info.getId());
    }
    
    private int podcastTitle2Index(String title){
        //List<Podcast> list = state_.podcastList_;
        RealmResults<PodcastRealm> list = currentQuery_.getPodcastList();
        for(int i = 0; i < list.size(); i++) {
            Podcast info = list.get(i);
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

    public void loadRealm(){
        boolean skipListened = pref_.getBoolean("skip_listened_episode", getResources().getBoolean(R.bool.default_skip_listened_episode));
        int order = Integer.valueOf(pref_.getString("episode_order", "0"));
        currentQuery_ = new SimpleQuery(null, skipListened, order, this);
        for(PodcastRealm podcast: currentQuery_.getPodcastList()){
            currentQuery_.getEpisodeList(podcast.getId());
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
                updatePlaylist(null);
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
        if (isLoading()) {
            Log.i(TAG, "Already loading");
            return;
        }
        SharedPreferences pref=
                PreferenceManager.getDefaultSharedPreferences(this);
        Resources res = getResources();
        int limit = Integer.valueOf(pref.getString("episode_limit", res.getString(R.string.default_episode_limit)));
        startLoading(new GetPodcastTask());
    }
    
    // @Override
    // public void notifyOrderChanged(int order){
    //     //TODO
    // }

    // private boolean playByInfo(EpisodeRealm info) {
    //     //umm...
    //     int playPos;
    //     for(playPos = 0; playPos < latestList_.size(); playPos++) {
    //         if(latestList_.get(playPos) == info) {
    //             break;
    //         }
    //     }
    //     if (playPos < 0){
    //         //Log.i(TAG, "playByInfo: info is not found: " + info.getURL());
    //         return false;
    //     }

    //     return player_.playNth(playPos);
    // }

    private void playEpisode(EpisodeRealm episode) {
        updatePlaylist(null);
        //TODO: pass episode id
        player_.playById(episode.getId());
    }

    public void updateUI(){
        //Log.d(TAG, "updateUI");
        updatePlayButton();
        adapter_.notifyDataSetChanged();
    }
    
    @Override
    public void onStartMusic(long episodeId) {
		//setProgressBarIndeterminateVisibility(false);
		//currentPlayPosition_.setMax(player_.getDuration());
		//int pos = player_.getCurrentPositionMsec();
        //currentPlayPosition_.setProgress(pos);
        //timer
        //Log.d(TAG, "onStartMusic");
        updateUI();
    }

    @Override
    public void onCompleteMusic(long episodeId) {
    }
        
    @Override
    public void onStartLoadingMusic(long episodeId) {
        //Log.d(TAG, "onStartLoadingMusic");
        updateUI();
    }

    @Override
    public void onStopMusic(int mode) {
        //Log.d(TAG, "onStopMusic");
        updateUI();
    }
    
    public class ItemClickListener
        implements View.OnClickListener
    {
        private EpisodeRealm info_;
        
        public ItemClickListener(EpisodeRealm info){
            info_ = info;
        }

        @Override
        public void onClick(View view){
            Log.d(TAG, "onClick: "+view.toString());
            EpisodeRealm info = info_;
            EpisodeRealm current = player_.getCurrentPodInfo();
            if(null != current && current.getURL().equals(info.getURL())) {
                if(player_.isPlaying()) {
                    player_.pauseMusic();
                }
                else {
                    if(! player_.restartMusic()){
                        playEpisode(info);
                    }
                }
            }
            else {
                playEpisode(info);
            }
        }
    }

   
    public class ItemLongClickListener
        implements View.OnLongClickListener
    {
        private EpisodeRealm info_;
        
        public ItemLongClickListener(EpisodeRealm info){
            info_ = info;
        }

        // private void shortVibrate() {
        //     Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        //     if (vibrator != null) {
        //         vibrator.vibrate(100);
        //     }
        // }

        @Override
        public boolean onLongClick(View v){
            //share
            //short vib
            // shortVibrate();
            Bundle b = new Bundle();
            b.putCharSequence("episode_title", info_.getTitle());
            b.putCharSequence("episode_url", info_.getURL().toString());
            b.putCharSequence("podcast_title", info_.getPodcast().getTitle());
            b.putCharSequence("podcast_url", info_.getPodcast().getURL().toString());
            showDialog(SHARE_EPISODE_DIALOG, b);
            return true;
        }
    }

    public int getCurrentCount(){
        int n = selector_.getSelectedItemPosition();
        if(episodeLimit_ < 0){
            if(n == 0 || n < 0){
                return currentQuery_.getEpisodeList().size();
            }
            PodcastRealm info = currentQuery_.getPodcastList().get(n-1);
            return currentQuery_.getEpisodeList(info.getId()).size();
        }
        if(n == 0 || n < 0){
            int size = 0;
            for(PodcastRealm info: currentQuery_.getPodcastList()){
                long id = info.getId();
                size += Math.min(currentQuery_.getEpisodeList(id).size(), episodeLimit_);
            }
            return size;
        }
        PodcastRealm info = currentQuery_.getPodcastList().get(n-1);
        return Math.min(currentQuery_.getEpisodeList(info.getId()).size(), episodeLimit_);
    }

    public EpisodeRealm getCurrentItem(int pos){
        int n = selector_.getSelectedItemPosition();
        if(episodeLimit_ < 0){
            if(n == 0 || n < 0){
                return currentQuery_.getEpisodeList().get(pos);
            }
            PodcastRealm info = currentQuery_.getPodcastList().get(n-1);
            return currentQuery_.getEpisodeList(info.getId()).get(pos);
        }
        if(n == 0 || n < 0){
            int remain = pos;
            for(PodcastRealm info: currentQuery_.getPodcastList()){
                long id = info.getId();
                RealmResults<EpisodeRealm> lst = currentQuery_.getEpisodeList(id);
                int virtsize = Math.min(lst.size(), episodeLimit_);
                if(remain < virtsize){
                    return lst.get(remain);
                }
                remain -= virtsize;
            }
        }
        PodcastRealm info = currentQuery_.getPodcastList().get(n-1);
        return currentQuery_.getEpisodeList(info.getId()).get(pos);
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
            final EpisodeRealm episode = getCurrentItem(position);
            holder.titleView_.setText(episode.getTitle());
            holder.timeView_.setText(episode.getPubdateStr(dateFormat_));
            holder.container_.setOnClickListener(new ItemClickListener(episode));
            holder.container_.setOnLongClickListener(new ItemLongClickListener(episode));

            if(null == player_){
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
            String iconURL = episode.getPodcast().getIconURL();
            //state_.podcastList_.get(episode.getIndex()).getIconURL();
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
            //return getCurentEpisodeList().size();
            return getCurrentCount();
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
        
        public GetPodcastTask() {
            super(PodplayerCardActivity.this, client_, EPISODE_BUF_SIZE);
            count_ = 0;
        }
        
        @Override
        protected void onProgressUpdate(String... values){
            // for (int i = 0; i < values.length; i++) {
            //     state_.mergeEpisode(values[i]);
            // }
            count_ += values.length;
            if(count_ - displayedCount_ > UPDATE_THRES){
                displayedCount_ = count_;
                adapter_.notifyDataSetChanged();
            }
        }
        
        private void onFinished() {
            loadTask_ = null;
            updatePlaylist(null);
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

    @Override
    protected Dialog onCreateDialog(int id, Bundle bundle) {
        final String title = (String)bundle.getCharSequence("episode_title");
        final String url = (String)bundle.getCharSequence("episode_url");
        final String podcastTitle = (String)bundle.getCharSequence("podcast_title");
        final String podcastURL = (String)bundle.getCharSequence("podcast_url");
        Dialog dialog;
        switch(id){
        case SHARE_EPISODE_DIALOG:
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.share_epiode)
                .setItems(LOCAL_SHARE_OPTIONS, new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick(DialogInterface dialog, int which){
                            if("Twitter".equals(LOCAL_SHARE_OPTIONS[which])){
                                //
                                Intent i = new Intent();
                                i.setAction(Intent.ACTION_SEND);
                                i.setType("text/plain");
                                i.setPackage("com.twitter.android");
                                StringBuffer sb = new StringBuffer();
                                sb.append(title);
                                sb.append(" #podplayer ");
                                sb.append(podcastURL);
                                startActivity(i);
                                
                                i.putExtra(Intent.EXTRA_TEXT, sb.toString());
                                startActivity(i);
                            }
                            else if("Mail".equals(LOCAL_SHARE_OPTIONS[which])){
                                Intent i = new Intent();
                                i.setAction(Intent.ACTION_SEND);
                                i.setType("message/rfc822");
                                //TODO: translate
                                StringBuffer sb = new StringBuffer();
                                sb.append(title);
                                sb.append("\n");
                                sb.append(podcastTitle);
                                sb.append("\n");
                                sb.append(url);
                                sb.append("\n-----\n");
                                sb.append(podcastURL);
                                sb.append("\n-----\npodplayer (Android app): https://play.google.com/store/apps/details?id=com.mamewo.podplayer0");
                                i.putExtra(Intent.EXTRA_TEXT, sb.toString());
                                startActivity(i);
                            }
                        }                        
                    })
                .setNegativeButton("Cancel", null); //TODO: put to strings
            dialog = builder.create();
            break;
        default:
            dialog = null;
            break;
        }
        return dialog;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
    }
}
