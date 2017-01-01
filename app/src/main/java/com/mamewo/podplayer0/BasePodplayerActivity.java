package com.mamewo.podplayer0;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.json.JSONException;

import com.mamewo.lib.podcast_parser.BaseGetPodcastTask;
import com.mamewo.lib.podcast_parser.EpisodeInfo;
import com.mamewo.lib.podcast_parser.PodcastInfo;

import static com.mamewo.podplayer0.Const.*;

import android.media.AudioManager;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import okhttp3.OkHttpClient;
import okhttp3.Cache;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.engine.cache.ExternalCacheDiskCacheFactory;
import android.support.v7.app.AppCompatActivity;

//common activity + gesture
abstract public class BasePodplayerActivity
    extends AppCompatActivity
    implements OnSharedPreferenceChangeListener,
    OnGesturePerformedListener,
    ServiceConnection
{
    final static
    private PodcastInfo[] DUMMY_INFO_LIST = new PodcastInfo[0];
    final static
    public String HTTP_CACHE_DIR = "http_cache";
    //16mbyte
    final static
    public long HTTP_CACHE_SIZE = 16*1024*1024; 
    
    protected PlayerService player_ = null;
    protected GestureLibrary gestureLib_;
    protected double gestureScoreThreshold_;
    protected BaseGetPodcastTask loadTask_;
    protected PodplayerState state_;
    protected boolean finishServiceOnExit_;
    protected ServiceConnection connection_;
    protected boolean showPodcastIcon_;
    private boolean uiSettingChanged_;

    public OkHttpClient client_;

    static final
    public int ICON_DISK_CACHE_BYTES = 64*1024*1024;
    protected int currentOrder_;

    abstract protected void onPodcastListChanged(boolean start);
    abstract protected void notifyOrderChanged(int order);

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ServiceConnection conn = this;
        Class<?> userClass = this.getClass();
        Intent intent = new Intent(this, PlayerService.class);
        startService(intent);
        finishServiceOnExit_ = false;
        uiSettingChanged_ = false;
        
        final Resources res = getResources();

        state_ = new PodplayerState();
        connection_ = conn;
        //TODO: handle error
        bindService(intent, conn, Context.BIND_AUTO_CREATE);
        loadTask_ = null;
        SharedPreferences pref=
                PreferenceManager.getDefaultSharedPreferences(this);
        pref.registerOnSharedPreferenceChangeListener(this);
        currentOrder_ = Integer.valueOf(pref.getString("episode_order", "0"));

        ExternalCacheDiskCacheFactory factory = new ExternalCacheDiskCacheFactory(this, "podcast_icon", ICON_DISK_CACHE_BYTES);
        //TODO: use new api
        if(!Glide.isSetup()){
            GlideBuilder builder = new GlideBuilder(getApplicationContext()).setDiskCache(factory);
            //obsolete API....
            Glide.setup(builder);
        }
        long timeoutSec = (long)Integer.valueOf(pref.getString("read_timeout", res.getString(R.string.default_read_timeout)));
        File cacheDir = new File(getExternalCacheDir(), HTTP_CACHE_DIR);
        client_ = new OkHttpClient.Builder()
            .readTimeout(timeoutSec, TimeUnit.SECONDS)
            .cache(new Cache(cacheDir, HTTP_CACHE_SIZE))
            .build();
    }

    @Override
    public void onDestroy() {
        SharedPreferences pref =
                PreferenceManager.getDefaultSharedPreferences(this);
        pref.unregisterOnSharedPreferenceChangeListener(this);
        if (null != loadTask_) {
            loadTask_.cancel(true);
        }
        boolean playing = player_.isPlaying();
        if(finishServiceOnExit_ && playing) {
            player_.stopMusic();
        }
        unbindService(connection_);
        if (finishServiceOnExit_ || ! playing) {
            Intent intent = new Intent(this, PlayerService.class);
            stopService(intent);
        }
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        //TODO: check current activity and preference
        if (uiSettingChanged_) {
            Intent intent = new Intent(this, MainActivity.class);
            //TODO: add flag of restart
            startActivity(intent);
            finish();
        }
    }
    
    // @Override
    // protected void onSaveInstanceState(Bundle outState) {
    //     outState.putSerializable("state", state_);
    //     super.onSaveInstanceState(outState);
    // }

    public void updatePlaylist() {
        //boolean reversed = currentOrder_ == REVERSE_APPEARANCE_ORDER;
        player_.setPlaylist(state_.list(currentOrder_));
    }

    public boolean isLoading() {
        return (null != loadTask_ && loadTask_.getStatus() == AsyncTask.Status.RUNNING);
    }
    
    public void startLoading(BaseGetPodcastTask task) {
        if (isLoading()) {
            Log.d(TAG, "startLoading: already loading");
            return;
        }
        //state_.loadedEpisode_.clear();
        loadTask_ = task;
        loadTask_.execute(state_.podcastList_.toArray(DUMMY_INFO_LIST));
    }

    public void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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
        case R.id.podcast_list_menu:
            startActivity(new Intent(this, PodcastListPreference.class));
            handled = true;
            break;
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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
        Log.d(TAG, "onSharedPreferneceChanged: " + key);
        syncPreference(pref, key);
    }

    public void savePodcastList() {
        try{
            PodcastListPreference.saveSetting(this, state_.podcastList_);
        }
        catch(JSONException e){
            Log.d(TAG, "saveError", e);
        }
        catch(IOException e){
            Log.d(TAG, "saveError", e);
        }
    }
 
    protected void syncPreference(SharedPreferences pref, String key){
        Log.d(TAG, "syncPreference: " + key);
        boolean updateAll = "ALL".equals(key);
        if ("view_mode".equals(key)) {
            uiSettingChanged_ = true;
        }
        Resources res = getResources();
        if (updateAll || "enable_gesture".equals(key)) {
            boolean useGesture = pref.getBoolean("enable_gesture", res.getBoolean(R.bool.default_enable_gesture));
            GestureOverlayView gestureView =
                    (GestureOverlayView)findViewById(R.id.gesture_view);
            if(useGesture) {
                gestureLib_ = GestureLibraries.fromRawResource(this, R.raw.gestures);
                if(! gestureLib_.load()){
                    Log.i(TAG, "gesture load failed");
                }
                gestureView.addOnGesturePerformedListener(this);
            }
            else {
                gestureView.removeOnGesturePerformedListener(this);
                gestureLib_ = null;
            }
            gestureView.setEnabled(useGesture);
        }
        if (updateAll || "gesture_score_threshold".equals(key)) {
            gestureScoreThreshold_ =
                    Double.valueOf(pref.getString("gesture_score_threshold",
                                                res.getString(R.string.default_gesture_score_threshold)));
        }
        if (updateAll || "show_podcast_icon".equals(key)) {
            showPodcastIcon_ = pref.getBoolean("show_podcast_icon", 
                                                res.getBoolean(R.bool.default_show_podcast_icon));
            //TODO: modify interface........
            currentOrder_ = Integer.valueOf(pref.getString("episode_order", "0"));
            notifyOrderChanged(currentOrder_);
        }
        if("clear_response_cache".equals(key)){
            try{
                Log.d(TAG, "HTTP response cache is cleared");
                client_.cache().evictAll();
                Glide.get(getApplicationContext()).clearMemory();
                final Context context = getApplicationContext();
                new Thread(){
                    public void run(){
                        try{
                            Glide.get(context).clearDiskCache();
                        }
                        catch(Exception e){
                            Log.d(TAG, "clearDiskCache: ", e);
                        }
                    }
                }.start();
                //Toast
            }
            catch(IOException e){
                Log.d(TAG, "cache remove failed");
            }
        }
        if(updateAll || "episode_order".equals(key)){
            currentOrder_ = Integer.valueOf(pref.getString("episode_order", "0"));
            notifyOrderChanged(currentOrder_);
        }
        //following block should be last one of this function
        if (updateAll || "podcastlist2".equals(key)) {
            Log.d(TAG, "podcastList load and update");
            state_.podcastList_ = PodcastListPreference.loadSetting(this);
            //TODO: reuse loaded episode
            state_.loadedEpisode_.clear();
            state_.latestList_.clear();
            //TODO: clear all icon
            for(int i = 0; i < state_.podcastList_.size(); i++){
                state_.loadedEpisode_.add(new ArrayList<EpisodeInfo>());
            }
            Log.d(TAG, "loadedEpisode_:" + state_.loadedEpisode_.size() + "; " + state_.podcastList_.size());
            onPodcastListChanged(updateAll);
        }
    }

    @Override
    public void onGesturePerformed(GestureOverlayView view, Gesture gesture) {
        ArrayList<Prediction> predictions = gestureLib_.recognize(gesture);
        if(predictions.size() == 0){
            showMessage("unknown gesture");
            return;
        }
        //predictions is sorted by score
        Prediction p = predictions.get(0);
        if(p.score < gestureScoreThreshold_) {
            //showMessage(String.format("gesture with low score: %.2f", p.score));
            return;
        }
        if("next".equals(p.name)) {
            player_.playNext();
        }
        else if("play".equals(p.name)) {
            updatePlaylist();
            if(! player_.restartMusic()) {
                player_.playMusic();
            }
        }
        else if("pause".equals(p.name)) {
            player_.pauseMusic();
        }
        else if("back".equals(p.name)) {
            player_.stopMusic();
            player_.playMusic();
        }
        showMessage(p.name);
    }

    final public static
    class PodplayerState
        implements Serializable
    {
        private static final long serialVersionUID = 1L;
        protected List<PodcastInfo> podcastList_;
        //same order with podcastList_
        protected List<List<EpisodeInfo>> loadedEpisode_;
        protected Date lastUpdatedDate_;
        protected List<EpisodeInfo> latestList_;
        
        private PodplayerState() {
            loadedEpisode_ = new ArrayList<List<EpisodeInfo>>();
            podcastList_ = new ArrayList<PodcastInfo>();
            lastUpdatedDate_ = null;
            latestList_ = new ArrayList<EpisodeInfo>();
        }
        
        // static
        // public void sortEpisodeByDate(List<EpisodeInfo> lst, boolean latestFirst){
        //     //dummy
        //     Collections.sort(lst, new EpisodeInfo.PubdateComparator());
        //     if(latestFirst){
        //         Collections.reverse(lst);
        //     }
        // }
        
        public List<EpisodeInfo> list(int orderSetting){
            //Log.d(TAG, "list: "+orderSetting);
            List l = new ArrayList<EpisodeInfo>();
            for(List<EpisodeInfo> loaded: loadedEpisode_){
                if(REVERSE_APPEARANCE_ORDER == orderSetting){
                    for(int i = 0; i < loaded.size(); i++){
                        l.add(loaded.get(loaded.size()-1-i));
                    }
                }
                else {
                    l.addAll(loaded);
                }
            }
            //l: filitered
            //TODO:check config
            // if(PUBDATE_ORDER == orderSetting || REVERSE_PUBDATE_ORDER == orderSetting){
            //     sortEpisodeByDate(l, REVERSE_PUBDATE_ORDER == orderSetting);
            // }
            latestList_.clear();
            latestList_.addAll(l);
            return l;
        }

        //TODO: use hash
        public void mergeEpisode(EpisodeInfo episode){
            //called by AsyncTask ...
            if(loadedEpisode_.size() <= episode.index_){
                return;
            }
            
            //TODO: binary search by date? (sort by date first)
            List<EpisodeInfo> targetList = loadedEpisode_.get(episode.index_);
            int s = targetList.size();
            for(int i = 0; i < s; i++){
                EpisodeInfo existing = targetList.get(s-1-i);
                //different episode
                if(episode.index_ != existing.index_){
                    targetList.add(episode);
                    return;
                }
                else if(episode.equalEpisode(existing)){
                    return;
                }
            }
            targetList.add(episode);
        }
    }
}
