package com.mamewo.podplayer0;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.json.JSONException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

//import com.google.firebase.analytics.FirebaseAnalytics;
import com.mamewo.podplayer0.parser.BaseGetPodcastTask;
import com.mamewo.podplayer0.parser.EpisodeInfo;
import com.mamewo.podplayer0.parser.Podcast;
//import com.mamewo.podplayer0.parser.PodcastInfo;

import com.mamewo.podplayer0.db.PodcastRealm;
import com.mamewo.podplayer0.db.EpisodeRealm;
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
import android.widget.SpinnerAdapter;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import okhttp3.OkHttpClient;
import okhttp3.Cache;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.engine.cache.ExternalCacheDiskCacheFactory;
import android.support.v7.app.AppCompatActivity;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.Sort;
import io.realm.RealmResults;
import io.realm.RealmConfiguration;
import io.realm.RealmChangeListener;

//common activity + gesture
abstract public class BasePodplayerActivity
    extends AppCompatActivity
    implements OnSharedPreferenceChangeListener,
    OnGesturePerformedListener,
    ServiceConnection
{
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
    protected int episodeLimit_;

    abstract
    public void notifyQuerySettingChanged();
    abstract
    public void notifyUISettingChanged();
    
    protected SharedPreferences pref_;
    protected DateFormat dateFormat_;
    //remove
    //private FirebaseAnalytics mFirebaseAnalytics;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Obtain the FirebaseAnalytics instance.
        //mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        Realm.init(getApplicationContext());
        RealmConfiguration realmConfig = new RealmConfiguration.Builder()
            .schemaVersion(1)
            .build();
        Realm.setDefaultConfiguration(realmConfig);
        
        pref_ = PreferenceManager.getDefaultSharedPreferences(this);
        ServiceConnection conn = this;
        Class<?> userClass = this.getClass();
        Intent intent = new Intent(this, PlayerService.class);
        startService(intent);
        finishServiceOnExit_ = false;
        uiSettingChanged_ = false;
        
        final Resources res = getResources();

        state_ = new PodplayerState();
        //
        connection_ = conn;
        //TODO: handle error
        bindService(intent, conn, Context.BIND_AUTO_CREATE);
        loadTask_ = null;
        pref_.registerOnSharedPreferenceChangeListener(this);
        currentOrder_ = Integer.valueOf(pref_.getString("episode_order", "0"));

        ExternalCacheDiskCacheFactory factory = new ExternalCacheDiskCacheFactory(this, "podcast_icon", ICON_DISK_CACHE_BYTES);
        //TODO: use new api
        if(!Glide.isSetup()){
            GlideBuilder builder = new GlideBuilder(getApplicationContext()).setDiskCache(factory);
            //obsolete API....
            Glide.setup(builder);
        }
        String formatStr = pref_.getString("date_format", YYYYMMDD_24H);
        dateFormat_ = new SimpleDateFormat(formatStr);
        long timeoutSec = (long)Integer.valueOf(pref_.getString("read_timeout", res.getString(R.string.default_read_timeout)));
        File cacheDir = new File(getExternalCacheDir(), HTTP_CACHE_DIR);
        client_ = new OkHttpClient.Builder()
            .readTimeout(timeoutSec, TimeUnit.SECONDS)
            .cache(new Cache(cacheDir, HTTP_CACHE_SIZE))
            .build();
        //TODO: async?
        PodcastListPreference.initData(this);
    }

    @Override
    public void onDestroy() {
        pref_.unregisterOnSharedPreferenceChangeListener(this);
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
    
    public void updatePlaylist(String title) {
        if(null == player_){
            return;
        }
        //boolean reversed = currentOrder_ == REVERSE_APPEARANCE_ORDER;
        //TODO: pass filter, sort order
        boolean skipListened = pref_.getBoolean("skip_listened_episode", getResources().getBoolean(R.bool.default_skip_listened_episode));
        player_.setPlaylistQuery(title, skipListened);
    }

    public boolean isLoading() {
        return (null != loadTask_ && loadTask_.getStatus() == AsyncTask.Status.RUNNING);
    }
    
    public void startLoading(BaseGetPodcastTask task) {
        if (isLoading()) {
            //Log.d(TAG, "startLoading: already loading");
            return;
        }
        //state_.loadedEpisode_.clear();
        loadTask_ = task;
        loadTask_.execute();
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
        //Log.d(TAG, "onSharedPreferneceChanged: " + key);
        syncPreference(pref, key);
    }

    protected void syncPreference(SharedPreferences pref, String key){
        //Log.d(TAG, "syncPreference: " + key);
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
        if (updateAll || "display_episode_icon".equals(key)) {
            showPodcastIcon_ = pref.getBoolean("display_episode_icon", 
                                                res.getBoolean(R.bool.default_display_episode_icon));
        }
        if("clear_response_cache".equals(key)){
            try{
                //Log.d(TAG, "HTTP response cache is cleared");
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
        }
        if(updateAll || "episode_limit".equals(key)){
            episodeLimit_ = Integer.valueOf(pref.getString("episode_limit", "-1"));
        }
        if(updateAll || "date_format".equals(key)){
            String format = pref.getString("date_format", YYYYMMDD_24H); 
            Log.d(TAG, "date_format: " + format);
            dateFormat_ = new SimpleDateFormat(format);
        }
        if("episode_order".equals(key)
           || "date_format".equals(key)
           || "skip_listened_episode".equals(key)
           || "episode_limit".equals(key)
           || "podcastlist2".equals(key)){
            if(!uiSettingChanged_){
                notifyQuerySettingChanged();
            }
        }
        else if("display_episode_icon".equals(key)
                || "display_expand_icon_in_group".equals(key)){
            notifyUISettingChanged();
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
            //TODO: apply filter
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

    final public
    class PodplayerState
        implements Serializable
    {
        private static final long serialVersionUID = 1L;
        //same order with podcastList_
        protected Date lastUpdatedDate_;
        
        private PodplayerState() {
            lastUpdatedDate_ = null;
        }
    }
}
