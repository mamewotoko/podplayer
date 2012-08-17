package com.mamewo.podplayer0;

import java.util.ArrayList;

import com.jayway.android.robotium.solo.Solo;

import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.View;
import asia.sonix.scirocco.SciroccoSolo;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class com.mamewo.podplayer0.PodplayerExpActivityTest \
 * com.mamewo.podplayer0.tests/android.test.InstrumentationTestRunner
 */
public class PodplayerExpActivityTest
extends ActivityInstrumentationTestCase2<PodplayerExpActivity>
{
	protected SciroccoSolo solo_;
	final static
	private String TAG = "podtest";

	public PodplayerExpActivityTest() {
		super("com.mamewo.podplayer0", PodplayerExpActivity.class);
	}

	@Override
	public void setUp() throws Exception {
		solo_ = new SciroccoSolo(getInstrumentation(), getActivity(), "com.mamewo.podtest");
	}

	@Override
	public void tearDown() throws Exception {
		try {
			solo_.finalize();
		}
		catch(Throwable e) {
			Log.i(TAG, "tearDown error", e);
		}
		getActivity().finish();
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

	public void testFinish() {
		solo_.sleep(500);
		//TODO: use resource
		solo_.clickOnMenuItem("Exit");
	}

	public void testAbortReload() throws Exception {
		solo_.sleep(500);
		solo_.takeScreenShot();
		View reloadButton = solo_.getView(R.id.reload_button);
		//TODO: check image source
		solo_.clickOnView(reloadButton);
		//TODO: check image source
		solo_.sleep(500);
		solo_.takeScreenShot();
		//TODO: this does not work...
		solo_.scrollUpList(0);
		solo_.sleep(10000);
	}

	public void testExpandAll() throws Exception {
		solo_.sleep(2500);
		View expandButton = solo_.getView(R.id.expand_button);
		solo_.clickOnView(expandButton);
		solo_.sleep(500);
		solo_.takeScreenShot();
		View collapseButton = solo_.getView(R.id.collapse_button);
		solo_.clickOnView(collapseButton);
		solo_.sleep(500);
		solo_.takeScreenShot();
	}
	//TODO: expand/collapse prefrence test
	
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
}
