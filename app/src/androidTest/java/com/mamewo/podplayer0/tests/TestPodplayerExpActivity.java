package com.mamewo.podplayer0.tests;

import com.robotium.solo.Solo;
import com.robotium.solo.Solo.Config;
import com.robotium.solo.Solo.Config.ScreenshotFileType;
import android.os.Environment;
import android.content.res.Resources;
import junit.framework.Assert;

import java.io.File;

import com.jraska.falcon.FalconSpoon;

import com.mamewo.podplayer0.PodplayerExpActivity;
import com.mamewo.podplayer0.R;

import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.View;

public class TestPodplayerExpActivity
    extends ActivityInstrumentationTestCase2<PodplayerExpActivity>
{
    protected Solo solo_;
    protected Resources res_;

    static final
    private int INIT_SLEEP = 5000;
    static final
    private int UI_SLEEP = 2000;
    final static
    private String TAG = "podtest";

    public void selectPreference(String targetTitle) {
        solo_.sleep(UI_SLEEP);
        Log.d(TAG, "current activity: "+solo_.getCurrentActivity().getTitle().toString());
        solo_.clickOnText(targetTitle);
    }
    
    public TestPodplayerExpActivity() {
        super("com.mamewo.podplayer0", PodplayerExpActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        Config config = new Config();
        config.screenshotFileType = ScreenshotFileType.PNG;
        config.screenshotSavePath = new File(Environment.getExternalStorageDirectory(), "Robotium-Screenshots").getPath();
        config.shouldScroll = false;
        res_ = getInstrumentation().getTargetContext().getResources();
        solo_ = new Solo(getInstrumentation(), config, getActivity());
    }

    @Override
    public void tearDown() throws Exception {
        try {
            solo_.finalize();
            solo_ = null;
        }
        catch(Throwable e) {
            Log.i(TAG, "tearDown error", e);
        }
        if (! getActivity().isFinishing()) {
            getActivity().finish();
        }
        solo_ = null;
        super.tearDown();
    }

    public void testPlay() throws Exception {
        Log.d(TAG, "testPlay starts");
        solo_.sleep(5000);
        View playButton = solo_.getView(R.id.play_button);
        Log.d(TAG, "testPlay: click play button");
        solo_.clickOnView(playButton);
        solo_.sleep(5000);
        //assertTrue(((ToggleButton)playButton).isChecked());
        //pause for next test
        solo_.clickOnView(playButton);
        solo_.sleep(1000);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "play");
    }

    public void testFinish() {
        solo_.sleep(500);
        solo_.clickOnMenuItem(res_.getString(R.string.exit_menu));
        solo_.sleep(500);
    }

    public void testAbortReload() throws Exception {
        solo_.sleep(5000);
        View reloadButton = solo_.getView(R.id.reload_button);
        //TODO: check image source
        solo_.clickOnView(reloadButton);
        //TODO: check image source
        solo_.sleep(500);
        //TODO: this does not work...
        solo_.scrollUpList(0);
        solo_.sleep(10000);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "abort_reload");
    }

    public void testExpandCollapse() throws Exception {
        solo_.sleep(2500);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "expand_1");
        View collapseButton = solo_.getView(R.id.collapse_button);
        solo_.clickOnView(collapseButton);
        solo_.sleep(500);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "expand_2");
        View expandButton = solo_.getView(R.id.expand_button);
        solo_.clickOnView(expandButton);
        solo_.sleep(500);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "expand_3");
    }
    //TODO: expand/collapse prefrence test
    
    //-----------------------
    public void testMainScreenshot() throws Exception {
        solo_.sleep(500);
        View playButton = solo_.getView(R.id.play_button);
        solo_.clickOnView(playButton);
        solo_.sendKey(Solo.MENU);
        solo_.sleep(200);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "main");
        solo_.sendKey(Solo.MENU);
        solo_.sleep(200);
        solo_.clickOnView(playButton);
        solo_.sleep(2000);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "main2");
    }

    public void testClearCache() throws Exception {
        Assert.assertTrue(solo_.waitForActivity("PodplayerExpActivity", INIT_SLEEP));
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "clear_cache");
        solo_.scrollDown();
        solo_.clickOnMenuItem(res_.getString(R.string.preference_menu));
        solo_.sleep(UI_SLEEP);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "clear_cache");
        selectPreference(res_.getString(R.string.clear_response_cache_title));
        solo_.sleep(UI_SLEEP);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "clear_cache");
        solo_.goBack();
        solo_.sleep(UI_SLEEP);
    }
}
