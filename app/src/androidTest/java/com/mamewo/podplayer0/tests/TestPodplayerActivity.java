package com.mamewo.podplayer0.tests;

import com.robotium.solo.Solo;
import com.robotium.solo.Solo.Config;
import com.robotium.solo.Illustration;
import android.content.res.Resources;

import com.jraska.falcon.FalconSpoon;

import com.mamewo.podplayer0.PodplayerActivity;
import com.mamewo.podplayer0.R;
import android.view.MotionEvent;

import junit.framework.Assert;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.content.Context;

public class TestPodplayerActivity
    extends ActivityInstrumentationTestCase2<PodplayerActivity>
{
    protected Solo solo_;
    protected Resources res_;
    static final
    private int INIT_SLEEP = 5000;
    static final
    private int UI_SLEEP = 1000;
    
    final static
    private String TAG = "podtest";

    public TestPodplayerActivity() {
        super("com.mamewo.podplayer0", PodplayerActivity.class);
    }

    public void selectPreference(String targetTitle) {
        solo_.sleep(UI_SLEEP);
        Log.d(TAG, "current activity: "+solo_.getCurrentActivity().getTitle().toString());
        solo_.clickOnText(targetTitle);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Config config = new Config();
        Context context = getInstrumentation().getTargetContext();
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        //editor.putBoolean("use_expandable_ui", false);
        editor.putBoolean("use_expandable_ui", true);
        editor.commit();

        solo_ = new Solo(getInstrumentation(), config, getActivity());
        res_ = getInstrumentation().getTargetContext().getResources();
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

    ///////////////////////
    public void testPlay() throws Exception {
        Assert.assertTrue(solo_.waitForActivity("PodplayerActivity", INIT_SLEEP));
        View playButton = solo_.getView(R.id.play_button);
        solo_.clickOnView(playButton);
        solo_.sleep(10000);
        //assertTrue(((ToggleButton)playButton).isChecked());
        solo_.clickOnView(playButton);
        solo_.sleep(500);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "play");
    }
    
    public void testPlayItem() throws Exception {
        Assert.assertTrue(solo_.waitForActivity("PodplayerActivity", INIT_SLEEP));
        solo_.sleep(10000);
        solo_.clickInList(2);
        solo_.sleep(10000);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "play_item_before");
        View playButton = solo_.getView(R.id.play_button);
        //assertTrue(((ToggleButton)playButton).isChecked());
        solo_.clickOnView(playButton);
        solo_.sleep(500);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "play_item");
    }

    //cannot take screen shot of dropdown
    // https://github.com/square/spoon/issues/4
    // public void testOpenFilter() {
    //     solo_.sleep(2000);
    //     solo_.clickOnView(solo_.getView(R.id.podcast_selector));
    //     FalconSpoon.screenshot(solo_.getCurrentActivity(), "open_filter0");
    //     solo_.sleep(3000);
    //     FalconSpoon.screenshot(solo_.getCurrentActivity(), "open_filter");
    //     //TODO: assert text of spinner item is drawn with white
    // }
    
    public void testFilter() {
        Assert.assertTrue(solo_.waitForActivity("PodplayerActivity", INIT_SLEEP));
        solo_.pressSpinnerItem(0, 2);
        solo_.sleep(3000);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "filter");
    }

    public void testFinish() {
        Assert.assertTrue(solo_.waitForActivity("PodplayerActivity", INIT_SLEEP));
        //TODO: use resource
        solo_.clickOnMenuItem(res_.getString(R.string.exit_menu));
    }

    public void testSelectPodcast() throws Exception {
        Assert.assertTrue(solo_.waitForActivity("PodplayerActivity", INIT_SLEEP));
        solo_.clickOnMenuItem(res_.getString(R.string.preference_menu));
        //false...
        //Assert.assertTrue(solo_.waitForActivity("PodplayerPrefrence", UI_SLEEP));
        // solo_.sleep(5000);
        // solo_.clickOnText(res_.getString(R.string.pref_podcastlist_title));
        selectPreference(res_.getString(R.string.pref_podcastlist_title));
        //solo_.waitForActivity(PodcastListPreference.class.getName(), UI_SLEEP);
        solo_.sleep(UI_SLEEP);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "select_podcast");       
        solo_.clickInList(3);
        solo_.sleep(500);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "select_podcast");
    }

    //TODO: long click
    // remove
    // move up
    // move down
    
    public void testAddPodcast() throws Exception {
        String url = "http://www.bbc.co.uk/programmes/p02nrvk3/episodes/downloads.rss";
        Assert.assertTrue(solo_.waitForActivity("PodplayerActivity", INIT_SLEEP));
        solo_.clickOnMenuItem(res_.getString(R.string.preference_menu));
        //false...
        //solo_.waitForActivity("PodplayerPrefrence", UI_SLEEP);
        selectPreference(res_.getString(R.string.pref_podcastlist_title));
        solo_.sleep(UI_SLEEP);
        //podcast list
        solo_.enterText(0, url);
        solo_.sleep(500);
        View addButton = solo_.getView(R.id.add_podcast_button);
        solo_.clickOnView(addButton);
        solo_.sleep(10000);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "add_podcast");
    }

    public void testAddAuthPodcast() throws Exception {
        String url = "http://mamewo.ddo.jp/podcast/auth/sample_podcast.xml";
        Assert.assertTrue(solo_.waitForActivity("PodplayerActivity", INIT_SLEEP));
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "add_auth");
        
        solo_.clickOnMenuItem(res_.getString(R.string.preference_menu));
        solo_.sleep(UI_SLEEP);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "add_auth");
       
        selectPreference(res_.getString(R.string.pref_podcastlist_title));
        solo_.sleep(UI_SLEEP);

        solo_.enterText((EditText)solo_.getView(R.id.url_edit), url);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "add_auth");
        solo_.clickOnView(solo_.getView(R.id.add_podcast_button));
        solo_.waitForDialogToClose(40000);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "add_auth");

        //enter username and password
        ListView list = (ListView)solo_.getView(R.id.podlist);
        ListAdapter adapter = list.getAdapter();
        int count = adapter.getCount();

        View v = list.getChildAt(count-1);

        View expand = v.findViewById(R.id.detail_button);
        solo_.clickOnView(expand);

        EditText usernameEdit = (EditText)v.findViewById(R.id.username);
        EditText passwordEdit = (EditText)v.findViewById(R.id.password);
        View loginButton = v.findViewById(R.id.auth_info);
        //TODO: assert usernameEdit, xxx are visible
        solo_.enterText(usernameEdit, "tak");
        solo_.enterText(passwordEdit, "takashi");
        solo_.clickOnView(loginButton);

        solo_.waitForDialogToClose(20000);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "add_auth");
        solo_.goBack();
        solo_.sleep(UI_SLEEP);
        solo_.goBack();
        solo_.sleep(40000);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "add_auth");

        //View collapseButton = solo_.getView(R.id.collapse_button);
        //solo_.clickOnView(collapseButton);
        solo_.scrollListToBottom((ListView)solo_.getView(R.id.list));
        solo_.sleep(UI_SLEEP);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "add_auth");
    }

    public void testDeletePodcastAndBack() throws Exception {
        Assert.assertTrue(solo_.waitForActivity("PodplayerActivity", INIT_SLEEP));
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "delete_and_back");
        
        solo_.clickOnMenuItem(res_.getString(R.string.preference_menu));
        solo_.sleep(UI_SLEEP);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "delete_and_back");
       
        selectPreference(res_.getString(R.string.pref_podcastlist_title));
        solo_.sleep(40000);

        ListView list = (ListView)solo_.getView(R.id.podlist);
        ListAdapter adapter = list.getAdapter();
        int count = adapter.getCount();

        View v = list.getChildAt(count-1);
        View expand = v.findViewById(R.id.detail_button);
        solo_.clickOnView(expand);
        solo_.sleep(UI_SLEEP);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "delete_and_back");

        View deleteButton = v.findViewById(R.id.delete);
        solo_.clickOnView(deleteButton);
        solo_.sleep(UI_SLEEP);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "delete_and_back");
        solo_.goBack();
        solo_.sleep(UI_SLEEP);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "delete_and_back");

        solo_.goBack();
        //crash
        solo_.sleep(5000);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "delete_and_back");
    }

    public void testAbortReload() {
        solo_.sleep(500);
        View cancelView = solo_.getView(R.id.cancel_image);
        Assert.assertEquals(cancelView.getVisibility(), View.VISIBLE);
        solo_.clickOnView(cancelView);
        solo_.sleep(500);
        //TODO: this does not work...
        solo_.scrollUpList(0);
        solo_.sleep(10000);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "abort_reload");
    }

    // public void testGestureScoreUp() throws Exception {
    //     solo_.sleep(500);
    //     solo_.clickOnMenuItem(res_.getString(R.string.preference_menu));
    //     solo_.waitForActivity(PodcastListPreference.class.getName(), 3000);
        
    //     selectPreference(res_.getString(R.string.pref_threshold_of_gesture_score));
    //     solo_.sleep(500);
    //     EditText edit = solo_.getEditText(0);
    //     String beforeString = edit.getText().toString();
    //     View plusButton = solo_.getView(R.id.double_plus_button);
    //     solo_.clickOnView(plusButton);
    //     solo_.sleep(100);
    //     String afterString = edit.getText().toString();
    //     double diff = Double.valueOf(afterString) - Double.valueOf(beforeString) - 0.1;
    //     Log.d(TAG, "diff:  " + diff);
    //     assertTrue(Math.abs(diff) < 0.0001);
    //     solo_.clickOnButton("OK");
    //     //TODO: check summary and pref value
    //     FalconSpoon.screenshot(solo_.getCurrentActivity(), "gesture_score_up");
    // }

    // public void testGestureScoreDown() throws Exception {
    //     solo_.sleep(500);
    //     solo_.clickOnMenuItem(res_.getString(R.string.preference_menu));
    //     selectPreference(res_.getString(R.string.pref_threshold_of_gesture_score));
    //     solo_.sleep(500);
    //     EditText edit = solo_.getEditText(0);
    //     String beforeString = edit.getText().toString();
    //     View minusButton = solo_.getView(R.id.double_minus_button);
    //     solo_.clickOnView(minusButton);
    //     solo_.sleep(100);
    //     String afterString = edit.getText().toString();
    //     double diff = Double.valueOf(beforeString) - Double.valueOf(afterString) - 0.1;
    //     Log.d(TAG, "befere after diff: " + beforeString + " " + afterString + " " + diff);
    //     assertTrue("scoreminused", Math.abs(diff) < 0.0001);
    //     solo_.clickOnButton(res_.getString(android.R.string.cancel));
    //     //TODO: check summary and pref value
    //     FalconSpoon.screenshot(solo_.getCurrentActivity(), "gesture_score_down");
    // }
   
    public void testGestureDialog() throws Exception {
        Assert.assertTrue(solo_.waitForActivity("PodplayerActivity", INIT_SLEEP));
        solo_.clickOnMenuItem(res_.getString(R.string.preference_menu));
        solo_.sleep(UI_SLEEP);
        selectPreference(res_.getString(R.string.pref_gesture_list));
        solo_.sleep(UI_SLEEP);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "gesture_dialog");
        //TODO: check that gesture list dialog is displayed
    }

    public void testClearCache() throws Exception {
        Assert.assertTrue(solo_.waitForActivity("PodplayerActivity", INIT_SLEEP));
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "clear_cache");
        solo_.scrollDown();       
        solo_.clickOnMenuItem(res_.getString(R.string.preference_menu));
        solo_.sleep(UI_SLEEP);
        selectPreference(res_.getString(R.string.clear_response_cache_title));
        solo_.sleep(UI_SLEEP);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "clear_cache");
        solo_.goBack();
        solo_.sleep(UI_SLEEP);
    }

    public void testImageDisableEnable() throws Exception {
        Assert.assertTrue(solo_.waitForActivity("PodplayerActivity", INIT_SLEEP));
        solo_.clickOnMenuItem(res_.getString(R.string.preference_menu));
        solo_.sleep(UI_SLEEP);
        solo_.scrollDown();
       
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "image_disable_enable");
        selectPreference(res_.getString(R.string.pref_show_podcast_icon));
        solo_.sleep(UI_SLEEP);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "image_disable_enable");
        solo_.goBack();
        //TODO: reload
        solo_.sleep(UI_SLEEP);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "image_disable_enable");
        //
        // int[] pos = new int[2];
        // View v = solo_.getCurrentActivity().findViewById(R.id.list);
        // v.getLocationOnScreen(pos);
        // solo_.drag(pos[0]+10, pos[1]+10, pos[0]+10, pos[1]+30, 4);
        
        solo_.clickOnMenuItem(res_.getString(R.string.preference_menu));
        solo_.sleep(UI_SLEEP);
        solo_.scrollDown();
        selectPreference(res_.getString(R.string.pref_show_podcast_icon));
        solo_.sleep(UI_SLEEP);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "image_disable_enable");
        solo_.goBack();
        solo_.sleep(UI_SLEEP);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "image_disable_enable");

        // v = solo_.getCurrentActivity().findViewById(R.id.list);
        // v.getLocationOnScreen(pos);
        // solo_.drag(pos[0]+10, pos[1]+10, pos[0]+10, pos[1]+30, 4);
        // FalconSpoon.screenshot(solo_.getCurrentActivity(), "image_disable_enable");
    }
    
    public void testLicence() {
        Assert.assertTrue(solo_.waitForActivity("PodplayerActivity", INIT_SLEEP));
        solo_.clickOnMenuItem(res_.getString(R.string.preference_menu));
        solo_.sleep(UI_SLEEP);
        solo_.scrollDown();
        solo_.scrollDown();

        selectPreference(res_.getString(R.string.pref_license));
        solo_.sleep(UI_SLEEP);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "license");
    }

    public void testVersion() {
        Assert.assertTrue(solo_.waitForActivity("PodplayerActivity", INIT_SLEEP));
        solo_.clickOnMenuItem(res_.getString(R.string.preference_menu));
        solo_.sleep(500);
        solo_.scrollDown();
        solo_.scrollDown();
        
        selectPreference(res_.getString(R.string.pref_version));
        solo_.sleep(500);
        View githubView = solo_.getView(R.id.github_logo);
        solo_.clickOnView(githubView);
        solo_.sleep(5000);
        //browser starts
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "version");
    }

    public void testMain() throws Exception {
        solo_.sleep(500);
        View playButton = solo_.getView(R.id.play_button);
        solo_.clickOnView(playButton);
        solo_.sendKey(Solo.MENU);
        solo_.sleep(1000);
        solo_.sendKey(Solo.MENU);
        solo_.sleep(300);
        solo_.clickOnView(playButton);
        solo_.sleep(1000);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "main");
    }
    
    public void testPreference() throws Exception {
        Assert.assertTrue(solo_.waitForActivity("PodplayerActivity", INIT_SLEEP));
        solo_.clickOnMenuItem(res_.getString(R.string.preference_menu));
        solo_.sleep(500);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "preference0");
        solo_.scrollDown();
        solo_.sleep(500);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "preference1");
        solo_.scrollDown();
        solo_.sleep(500);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "preference2");
    }

    public void testPlayGesture() throws Exception {
        Assert.assertTrue(solo_.waitForActivity("PodplayerActivity", INIT_SLEEP));
        int px1 = 100;
        int py1 = 300;

        int px2 = 400;
        int py2 = py1+100;

        int px3 = px1;
        int py3 = py2+100;

        Illustration.Builder builder = solo_.createIllustrationBuilder();
        builder.addPoint(px1, py1, MotionEvent.TOOL_TYPE_FINGER);
        builder.addPoint(px2, py2, MotionEvent.TOOL_TYPE_FINGER);
        builder.addPoint(px3, py3, MotionEvent.TOOL_TYPE_FINGER);
        builder.addPoint(px1, py1-50, MotionEvent.TOOL_TYPE_FINGER);
        Illustration illustration = builder.build();
        solo_.sleep(10000);
        solo_.illustrate(illustration);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "play_gesture");
        solo_.sleep(10000);
        //TODO: add assert that playing podcast
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "play_gesture");

        int qx1 = px1;
        int qy1 = py1;

        int qx2 = qx1;
        int qy2 = qy1+200;

        int qx3 = qx1+200;
        int qy3 = qy1;

        int qx4 = qx1+200;
        int qy4 = qy1+200+50;

        builder = solo_.createIllustrationBuilder();
        builder.addPoint(qx1, qy1, MotionEvent.TOOL_TYPE_FINGER);
        builder.addPoint(qx2, qy2, MotionEvent.TOOL_TYPE_FINGER);
        builder.addPoint(qx3, qy3, MotionEvent.TOOL_TYPE_FINGER);
        builder.addPoint(qx4, qy4, MotionEvent.TOOL_TYPE_FINGER);

        illustration = builder.build();
        solo_.illustrate(illustration);
        //cannot capture gesture yellow line
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "play_gesture");
        solo_.sleep(UI_SLEEP);
        //TODO: add assert that pausing 
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "play_gesture");
    }

    public void testNextBackGesture() throws Exception {
        Assert.assertTrue(solo_.waitForActivity("PodplayerActivity", INIT_SLEEP));
        int px1 = 300;
        int py1 = 300;

        int px2 = px1+100;
        int py2 = py1+200;

        int px3 = 100;
        int py3 = py2;

        Illustration.Builder builder = solo_.createIllustrationBuilder();
        builder.addPoint(px1, py1, MotionEvent.TOOL_TYPE_FINGER);
        builder.addPoint(px2, py2, MotionEvent.TOOL_TYPE_FINGER);
        builder.addPoint(px3, py3, MotionEvent.TOOL_TYPE_FINGER);
        Illustration illustration = builder.build();
        solo_.sleep(10000);
        solo_.illustrate(illustration);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "next_gesture");
        solo_.sleep(10000);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "next_gesture");

        int qx1 = 300;
        int qy1 = 300;

        int qx2 = qx1-100;
        int qy2 = qy1+200;

        int qx3 = 500;
        int qy3 = qy2;

        builder = solo_.createIllustrationBuilder();
        builder.addPoint(qx1, qy1, MotionEvent.TOOL_TYPE_FINGER);
        builder.addPoint(qx2, qy2, MotionEvent.TOOL_TYPE_FINGER);
        builder.addPoint(qx3, qy3, MotionEvent.TOOL_TYPE_FINGER);
        illustration = builder.build();
        solo_.illustrate(illustration);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "next_gesture");
        solo_.sleep(10000);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "next_gesture");
    }

    public void testPullToRefresh() throws Exception {
        Assert.assertTrue(solo_.waitForActivity("PodplayerActivity", INIT_SLEEP));
        int px1 = 300;
        int py1 = 300;

        int px2 = px1;
        int py2 = py1 + 500;

        Illustration.Builder builder = solo_.createIllustrationBuilder();
        builder.addPoint(px1, py1, MotionEvent.TOOL_TYPE_FINGER);
        builder.addPoint(px2, py2, MotionEvent.TOOL_TYPE_FINGER);

        Illustration illustration = builder.build();
        solo_.sleep(10000);
        solo_.illustrate(illustration);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "pull_to_refresh");
        solo_.sleep(10000);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "pull_to_refresh");
    }   
}
