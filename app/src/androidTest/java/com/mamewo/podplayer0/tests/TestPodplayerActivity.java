package com.mamewo.podplayer0.tests;

import java.util.List;

//import com.jayway.android.robotium.solo.Solo;
import com.robotium.solo.Solo;
import com.robotium.solo.Solo.Config;
import com.robotium.solo.Solo.Config.ScreenshotFileType;
import android.os.Environment;
import java.io.File;

import com.mamewo.podplayer0.PodcastListPreference;
import com.mamewo.podplayer0.PodplayerActivity;
import com.mamewo.podplayer0.R;

import junit.framework.Assert;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;
//import asia.sonix.scirocco.SciroccoSolo;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class com.mamewo.podplayer0.PodplayerActivityTest \
 * com.mamewo.podplayer0.tests/android.test.InstrumentationTestRunner
 */
public class TestPodplayerActivity
	extends ActivityInstrumentationTestCase2<PodplayerActivity>
{
	//protected SciroccoSolo solo_;
	protected Solo solo_;
	final static
	private String TAG = "podtest";

	public TestPodplayerActivity() {
		super("com.mamewo.podplayer0", PodplayerActivity.class);
	}

	public boolean selectPreference(String targetTitle) {
		TextView view = null;
		solo_.waitForActivity("PodplayerPrefrence", 3000);
		do {
			//ArrayList<TextView> list = solo_.getCurrentTextViews(null);
			List<TextView> list = solo_.getCurrentViews(TextView.class, false);
			for (TextView listText : list) {
				if(targetTitle.equals(listText.getText())){
					view = listText;
					break;
				}
			}
		}
		while(null == view && solo_.scrollDownList(0));
		if (view == null) {
			return false;
		}
		solo_.clickOnView(view);
		return true;
	}

	@Override
	public void setUp() throws Exception {
		//solo_ = new SciroccoSolo(getInstrumentation(), getActivity(), "com.mamewo.podtest");
		Config config = new Config();
		config.screenshotFileType = ScreenshotFileType.PNG;
		config.screenshotSavePath = new File(Environment.getExternalStorageDirectory(), "Robotium-Screenshots").getPath();
		Log.d(TAG, "screenshotpath:"+config.screenshotSavePath.toString());
		config.shouldScroll = false;

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

	public void testPlay() throws Exception {
		Log.d(TAG, "testPlay starts");
		solo_.sleep(5000);
		View playButton = solo_.getView(R.id.play_button);
		Log.d(TAG, "testPlay: click play button");
		solo_.clickOnView(playButton);
		solo_.sleep(10000);
		assertTrue(((ToggleButton)playButton).isChecked());
		solo_.clickOnView(playButton);
		solo_.sleep(500);
		solo_.takeScreenshot("testPlay");
	}
	
	public void testPlayItem() throws Exception {
		solo_.sleep(10000);
		solo_.clickInList(2);
		solo_.sleep(10000);
		View playButton = solo_.getView(R.id.play_button);
		assertTrue(((ToggleButton)playButton).isChecked());
		solo_.clickOnView(playButton);
		solo_.sleep(500);
		solo_.takeScreenshot("testPlayItem");
	}

	public void testFilter() {
		solo_.sleep(500);
		solo_.pressSpinnerItem(0, 2);
		solo_.sleep(3000);
		solo_.takeScreenshot("testFilter");
	}

	public void testFinish() {
		solo_.sleep(500);
		//TODO: use resource
		solo_.clickOnMenuItem("Exit");
	}

	public void testSelectPodcast() throws Exception {
		solo_.sleep(1000);
		solo_.clickOnMenuItem("Preference");
		selectPreference("Podcast list");
		solo_.waitForActivity(PodcastListPreference.class.getName(), 3000);
		solo_.clickInList(1);
		solo_.sleep(500);
		solo_.clickInList(3);
		// solo_.clickInList(5);
		// solo_.sleep(500);
		solo_.takeScreenshot("testSelectPodcast");
	}

	//TODO: long click
	// remove
	// move up
	// move down
	
	//TODO: add testAddPodcast
	public void testAddPodcast() throws Exception {
		String url = "http://www.tfm.co.jp/podcasts/avanti/podcast.xml";
		solo_.sleep(1000);
		solo_.clickOnMenuItem("Preference");
		selectPreference("Podcast list");
		solo_.waitForActivity(PodcastListPreference.class.getName(), 3000);
		solo_.enterText(0, url);
		solo_.sleep(500);
		View addButton = solo_.getView(R.id.add_podcast_button);
		solo_.clickOnView(addButton);
		//TOOD: add assert
		solo_.sleep(5000);
		solo_.takeScreenshot("testAddPodcast");
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
		solo_.takeScreenshot("testAbortReload");
	}

	public void testGestureScoreUp() throws Exception {
		solo_.sleep(500);
		solo_.clickOnMenuItem("Preference");
		selectPreference("Threshold of score");
		solo_.sleep(500);
		EditText edit = solo_.getEditText(0);
		String beforeString = edit.getText().toString();
		View plusButton = solo_.getView(R.id.double_plus_button);
		solo_.clickOnView(plusButton);
		solo_.sleep(100);
		String afterString = edit.getText().toString();
		double diff = Double.valueOf(afterString) - Double.valueOf(beforeString) - 0.1;
		Log.d(TAG, "diff:  " + diff);
		assertTrue(Math.abs(diff) < 0.0001);
		solo_.clickOnButton("OK");
		//TODO: check summary and pref value
		solo_.takeScreenshot("testGestureScoreUp");
	}

	public void testGestureScoreDown() throws Exception {
		solo_.sleep(500);
		solo_.clickOnMenuItem("Preference");
		selectPreference("Threshold of score");
		solo_.sleep(500);
		EditText edit = solo_.getEditText(0);
		String beforeString = edit.getText().toString();
		View minusButton = solo_.getView(R.id.double_minus_button);
		solo_.clickOnView(minusButton);
		solo_.sleep(100);
		String afterString = edit.getText().toString();
		double diff = Double.valueOf(beforeString) - Double.valueOf(afterString) - 0.1;
		Log.d(TAG, "befere after diff: " + beforeString + " " + afterString + " " + diff);
		assertTrue("scoreminused", Math.abs(diff) < 0.0001);
		solo_.clickOnButton("Cancel");
		//TODO: check summary and pref value
		solo_.takeScreenshot("testGestureScoreDown");
	}

	public void testGestureDialog() throws Exception {
		solo_.sleep(500);
		solo_.clickOnMenuItem("Preference");
		selectPreference("Gesture list");
		solo_.sleep(1000);
		solo_.takeScreenshot("testGestureDialog");
		//TODO: check that gesture list dialog is displayed
	}
	
	public void testLicence() {
		solo_.clickOnMenuItem("Preference");
		selectPreference("License");
		//TODO: screen shot
		solo_.sleep(2000);
		//click ok button
		solo_.clickOnButton(0);
		solo_.sleep(200);
		solo_.takeScreenshot("testLicence");
	}

	public void testVersion() {
		solo_.clickOnMenuItem("Preference");
		solo_.sleep(500);
		Assert.assertTrue(selectPreference("Version"));
		solo_.sleep(500);
		View githubView = solo_.getView(R.id.github_logo);
		solo_.clickOnView(githubView);
		solo_.sleep(5000);
		//browser starts
		solo_.takeScreenshot("testVersion");
	}

	//-----------------------
	public void testMain() throws Exception {
		solo_.sleep(500);
		View playButton = solo_.getView(R.id.play_button);
		solo_.clickOnView(playButton);
		solo_.sendKey(Solo.MENU);
		solo_.sleep(1000);
		solo_.sendKey(Solo.MENU);
		solo_.sleep(300);
		solo_.clickOnView(playButton);
		solo_.sleep(300);
		assertFalse(((ToggleButton)playButton).isChecked());
		solo_.takeScreenshot("testMain");
	}
	
	public void testPreference() throws Exception {
		solo_.sleep(500);
		solo_.clickOnMenuItem("Preference");
		solo_.sleep(1000);
		solo_.scrollDown();
		solo_.sleep(500);
		solo_.takeScreenshot("testPreference");
	}
}
