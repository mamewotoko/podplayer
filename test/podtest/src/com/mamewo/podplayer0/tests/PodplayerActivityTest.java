package com.mamewo.podplayer0.tests;

import java.util.ArrayList;

import com.jayway.android.robotium.solo.Solo;
import com.mamewo.podplayer0.PodcastListPreference;
import com.mamewo.podplayer0.PodplayerActivity;
import com.mamewo.podplayer0.R;

import junit.framework.Assert;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import asia.sonix.scirocco.SciroccoSolo;

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
public class PodplayerActivityTest
	extends ActivityInstrumentationTestCase2<PodplayerActivity>
{
	protected SciroccoSolo solo_;
	final static
	private String TAG = "podtest";

	public PodplayerActivityTest() {
		super("com.mamewo.podplayer0", PodplayerActivity.class);
	}

	public boolean selectPreference(String targetTitle) {
		TextView view = null;
		solo_.waitForActivity("PodplayerPrefrence", 3000);
		do {
			ArrayList<TextView> list = solo_.getCurrentTextViews(null);
			for (TextView listText : list) {
				//Log.i(TAG, "listtext: " + listText.getText());
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
		solo_ = new SciroccoSolo(getInstrumentation(), getActivity(), "com.mamewo.podtest");
	}

	@Override
	public void tearDown() throws Exception {
		try {
			solo_.takeScreenShot();
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

	public void testPlay() throws Exception {
		solo_.sleep(500);
		View playButton = solo_.getView(R.id.play_button);
		solo_.clickOnView(playButton);
		solo_.sleep(500);
		solo_.takeScreenShot();
		solo_.sleep(10000);
	}

	public void testFilter() {
		solo_.sleep(500);
		solo_.pressSpinnerItem(0, 2);
		solo_.sleep(3000);
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
		solo_.takeScreenShot();
		solo_.clickInList(1);
		solo_.clickInList(3);
		solo_.clickInList(5);
		solo_.sleep(500);
		solo_.takeScreenShot();
	}
	
	//TODO: add testAddPodcast
	public void testAddPodcast() throws Exception {
		String url = "http://www.tfm.co.jp/podcasts/avanti/podcast.xml";
		solo_.sleep(1000);
		solo_.clickOnMenuItem("Preference");
		selectPreference("Podcast list");
		solo_.waitForActivity(PodcastListPreference.class.getName(), 3000);
		solo_.enterText(0, url);
		View addButton = solo_.getView(R.id.add_podcast_button);
		solo_.clickOnView(addButton);
		//TOOD: add assert
		solo_.sleep(5000);
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
		String afterString = edit.getText().toString();
		double diff = Double.valueOf(afterString) - Double.valueOf(beforeString) - 0.1;
		assertTrue(diff >= 0.0 && diff < 0.0001);
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
		String afterString = edit.getText().toString();
		double diff = Double.valueOf(beforeString) - Double.valueOf(afterString) - 0.1;
		assertTrue(diff >= 0.0 && diff < 0.0001);
	}

	public void testGestureDialog() throws Exception {
		solo_.sleep(500);
		solo_.clickOnMenuItem("Preference");
		selectPreference("Gesture list");
		solo_.sleep(1000);
		solo_.takeScreenShot();
		//TODO: check that gesture list dialog is displayed
	}
	
	public void testLicence() {
		solo_.clickOnMenuItem("Preference");
		selectPreference("License");
		//screen shot
	}

	public void testVersion() {
		solo_.clickOnMenuItem("Preference");
		solo_.sleep(500);
		Assert.assertTrue(selectPreference("Version"));
		solo_.sleep(500);
		View githubView = solo_.getView(R.id.github_logo);
		solo_.clickOnView(githubView);
		solo_.sleep(10000);
		//browser starts
	}

	//-----------------------
	public void testPlayingScreenshot() throws Exception {
		solo_.sleep(500);
		View playButton = solo_.getView(R.id.play_button);
		solo_.clickOnView(playButton);
		solo_.sleep(1000);
		solo_.takeScreenShot();
	}
	
	public void testMainScreenshot() throws Exception {
		solo_.sleep(500);
		View playButton = solo_.getView(R.id.play_button);
		solo_.clickOnView(playButton);
		solo_.sendKey(Solo.MENU);
		solo_.sleep(500);
		solo_.takeScreenShot();
	}
	
	public void testPreferenceScreenshot() throws Exception {
		solo_.sleep(500);
		solo_.clickOnMenuItem("Preference");
		solo_.sleep(1000);
		solo_.takeScreenShot();
		solo_.scrollDown();
		solo_.sleep(500);
		solo_.takeScreenShot();
	}
}
