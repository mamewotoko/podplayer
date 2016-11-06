package com.mamewo.podplayer0;

import static com.mamewo.podplayer0.Const.*;

import java.text.MessageFormat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.util.Log;

import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v7.preference.ListPreference;

import android.support.v4.app.DialogFragment;

public class PodplayerPreferenceFragment
    extends PreferenceFragmentCompat
    implements OnPreferenceClickListener,
    OnSharedPreferenceChangeListener
{
    static final
    private String DIALOG_FRAGMENT_TAG = "com.mamewo.podplayer.pref.dialogtag";
    
    private Preference podcastList_;
    private Preference version_;
    private Preference license_;
    private Preference mailToAuthor_;
    private Preference gestureTable_;
    private ListPreference readTimeout_;
    //private Preference scoreThreshold_;
    private Preference clearCache_;
    private Preference episodeLimit_;
    private SharedPreferences pref_;
    private ListPreference episodeOrder_;
    private String versionStr_;
    
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preference);
        version_ = findPreference("version");
        PackageInfo pi;
        try {
            pi = getActivity().getPackageManager().getPackageInfo(PodplayerPreferenceFragment.class.getPackage().getName(), 0);
            versionStr_ = pi.versionName;
        }
        catch (NameNotFoundException e) {
            versionStr_ = "unknown";
        }
        version_.setSummary(versionStr_);
        podcastList_ = findPreference("podcastlist");
        podcastList_.setOnPreferenceClickListener(this);
        readTimeout_ = (ListPreference)findPreference("read_timeout");
        //scoreThreshold_ = findPreference("gesture_score_threshold");
        episodeLimit_ = findPreference("episode_limit");
        episodeOrder_ = (ListPreference)findPreference("episode_order");
        gestureTable_ = findPreference("gesture_list");
        gestureTable_.setOnPreferenceClickListener(this);
        version_.setOnPreferenceClickListener(this);
        license_ = findPreference("license");
        license_.setOnPreferenceClickListener(this);
        mailToAuthor_ = findPreference("mail_to_author");
        mailToAuthor_.setOnPreferenceClickListener(this);
        
        //CheckBoxPreference cachePreference = (CheckBoxPreference)findPreference("use_response_cache");
        //Build.VERSION_CODES.HONEYCOMB_MR2;
        clearCache_ = findPreference("clear_response_cache");
        clearCache_.setOnPreferenceClickListener(this);

        // boolean cacheSupported = Build.VERSION.SDK_INT >= 13;
        // cachePreference.setEnabled(cacheSupported);
        // clearCache_.setEnabled(cacheSupported);
        // if(!cacheSupported){
        //     cachePreference.setChecked(false);
        // }
        // else {
        //     clearCache_.setOnPreferenceClickListener(this);
        // }
        pref_ = PreferenceManager.getDefaultSharedPreferences(getActivity());
        pref_.registerOnSharedPreferenceChangeListener(this);
        updateSummary(pref_, "ALL");
    }

    @Override
    public void onDestroy() {
        pref_.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    public boolean onPreferenceClick(Preference item) {
        if (item == podcastList_) {
            Intent i = new Intent(getActivity(), PodcastListPreference.class);
            startActivity(i);
            return true;
        }
        if (item == clearCache_){
            Log.d(TAG, "onPreferenceClick: clearCache_");
            //dummy field to clear cache later
            boolean flag = pref_.getBoolean("clear_response_cache", true);
            pref_.edit()
                .putBoolean("clear_response_cache", !flag)
                .commit();
            return true;
        }
        if(item == mailToAuthor_){
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("message/rfc822");
            i.putExtra(Intent.EXTRA_EMAIL, new String[]{"mamewotoko@gmail.com"});
            i.putExtra(Intent.EXTRA_SUBJECT, "[podplayer "+versionStr_+"] ");
            startActivity(i);
            return true;
        }
        return false;
    }
    
    public void updateSummary(SharedPreferences pref, String key) {
        boolean updateAll = "ALL".equals(key);
        Resources res = getResources();
        
        if (updateAll || "read_timeout".equals(key)) {
            readTimeout_.setSummary(readTimeout_.getEntry());
        }
        // if (updateAll || "gesture_score_threshold".equals(key)) {    
        //     double threshold = Double.valueOf(pref.getString("gesture_score_threshold", 
        //                                                     res.getString(R.string.default_gesture_score_threshold)));
        //     scoreThreshold_.setSummary(String.format("%.2f", threshold));
        // }
        if (updateAll || "episode_limit".equals(key)){
            int limit = Integer.valueOf(pref.getString("episode_limit", 
                                                        res.getString(R.string.default_episode_limit)));
            String summary = getString(R.string.pref_episode_nolimit_summary);
            if (limit > 0){
                summary = MessageFormat.format(getString(R.string.pref_episode_limit_summary), limit);
            }
            episodeLimit_.setSummary(summary);
        }
        if(updateAll || "episode_order".equals(key)){
            int order = Integer.valueOf(pref.getString("episode_order", "0"));
            episodeOrder_.setSummary(res.getStringArray(R.array.episode_item_order_entries)[order]);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
        updateSummary(pref, key);
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference){
        //TODO: check exist
        if(preference instanceof SimpleDialogPreference){
            DialogFragment f = SimpleDialogPreferenceFragment.newInstance(preference.getKey());
            f.setTargetFragment(this, 0);
            f.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
        }
        else {
            super.onDisplayPreferenceDialog(preference);
        }
    }
}
