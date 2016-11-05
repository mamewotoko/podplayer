package com.mamewo.podplayer0.tests;

import com.mamewo.podplayer0.MainActivity;
import com.mamewo.podplayer0.PodplayerActivity;
import com.mamewo.podplayer0.PodplayerExpActivity;

import com.robotium.solo.Solo;
import com.robotium.solo.Solo.Config;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

public class TestMainActivity
    extends ActivityInstrumentationTestCase2<MainActivity>
{
    private Solo solo_;
    static final
    private String TAG = "podtest";
    
    public TestMainActivity() {
        super("com.mamewo.podplayer0", MainActivity.class);
    }
    
    @Override
    public void setUp() {
        Config config = new Config();
        solo_ = new Solo(getInstrumentation(), config, getActivity());
    }
    
    @Override
    public void tearDown() throws Exception {
        try {
            solo_.finalize();
        }
        catch(Throwable e) {
            Log.i(TAG, "tearDown error", e);
        }
        if (! getActivity().isFinishing()) {
            getActivity().finish();
        }
        super.tearDown();
    }
    
    public void testStart() {
        Activity activity = getActivity();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean prevValue = pref.getBoolean("use_expandable_ui", false);
        String activityClass;
        if (prevValue) {
            activityClass = PodplayerExpActivity.class.getName();
        }
        else {
            activityClass = PodplayerActivity.class.getName();
        }
//        Activity last = solo_.getActivityMonitor().getLastActivity();
        solo_.sleep(3000);
        Log.d(TAG, "testStart: " + prevValue + " / " + activityClass);
        //assertEquals(activityClass, last.getClass().getName());
        pref.edit().putBoolean("use_expandable_ui", !prevValue).commit();
    }
    
    public void testStart2() {
        Activity activity = getActivity();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean prevValue = pref.getBoolean("use_expandable_ui", false);
        String activityClass;
        if (prevValue) {
            activityClass = PodplayerExpActivity.class.getName();
        }
        else {
            activityClass = PodplayerActivity.class.getName();
        }
//        Activity last = solo_.getActivityMonitor().getLastActivity();
        solo_.sleep(3000);
        Log.d(TAG, "testStart: " + prevValue + " / " + activityClass);
        //assertEquals(activityClass, last.getClass().getName());
        pref.edit().putBoolean("use_expandable_ui", !prevValue).commit();
    }
}
