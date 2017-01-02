package com.mamewo.podplayer0.tests;

import com.robotium.solo.Solo;
import com.robotium.solo.Solo.Config;
import com.robotium.solo.Illustration;
import android.content.res.Resources;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.View;
import android.content.Context;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;

import junit.framework.Assert;

import com.mamewo.podplayer0.PodplayerCardActivity;
import static com.mamewo.podplayer0.Const.*;
    
import com.jraska.falcon.FalconSpoon;

public class TestPodplayerCardActivity
    extends ActivityInstrumentationTestCase2<PodplayerCardActivity>
{
    protected Solo solo_;
    static final
    private int INIT_SLEEP = 5000;
    static final
    private int UI_SLEEP = 1000;

    final static
    private String TAG = "podtest";

    public TestPodplayerCardActivity() {
        super("com.mamewo.podplayer0", PodplayerCardActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Config config = new Config();
        Context context = getInstrumentation().getTargetContext();
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString("view_mode", String.valueOf(VIEW_CARD));
        editor.commit();

        solo_ = new Solo(getInstrumentation(), config, getActivity());
    }

    @Override
    public void tearDown() throws Exception {
        try {
            getActivity().finish();
            solo_.finishOpenedActivities();
            System.gc();
            solo_.finalize();
        }
        catch(Throwable e) {
            Log.i(TAG, "tearDown error", e);
        }
        solo_ = null;
        super.tearDown();
    }

    public void testStart() throws Exception {
        Assert.assertTrue(solo_.waitForActivity("PodplayerCardActivity", INIT_SLEEP));
        solo_.sleep(5000);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "start");
    }

    public void testPlay() throws Exception {
        Assert.assertTrue(solo_.waitForActivity("PodplayerCardActivity", INIT_SLEEP));
        solo_.sleep(5000);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "play");
        solo_.clickInRecyclerView(2);
        solo_.sleep(500);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "play");
        //stop
        solo_.clickInRecyclerView(2);
        solo_.sleep(500);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "play");
        //TODO: add assert
    }

    public void testLongClick() throws Exception {
        Assert.assertTrue(solo_.waitForActivity("PodplayerCardActivity", INIT_SLEEP));
        solo_.sleep(5000);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "long_click");
        solo_.clickLongInRecycleView(2);
        solo_.sleep(500);
        Assert.assertTrue("dialog is displyed", solo_.waitForDialogToOpen(2000));
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "long_click");
    }

    public void testFilter() {
        Assert.assertTrue(solo_.waitForActivity("PodplayerCardActivity", INIT_SLEEP));
        solo_.pressSpinnerItem(0, 3);
        solo_.sleep(500);
        solo_.clickInRecyclerView(0);
        solo_.sleep(5000);
        //TODO: assert
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "filter");
        //stop
        solo_.clickInRecyclerView(0);
        solo_.sleep(500);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "filter");
    }

    //TODO: reload
}
