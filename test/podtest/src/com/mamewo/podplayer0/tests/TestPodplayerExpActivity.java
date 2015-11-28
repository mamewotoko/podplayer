package com.mamewo.podplayer0.tests;

//import com.jayway.android.robotium.solo.Solo;
import com.robotium.solo.Solo;
import com.robotium.solo.Solo.Config;
import com.robotium.solo.Solo.Config.ScreenshotFileType;
import android.os.Environment;
import java.io.File;

import com.mamewo.podplayer0.PodplayerExpActivity;
import com.mamewo.podplayer0.R;

import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.View;
import android.widget.ToggleButton;
//import asia.sonix.scirocco.SciroccoSolo;

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
public class TestPodplayerExpActivity
	extends ActivityInstrumentationTestCase2<PodplayerExpActivity>
{
	//protected SciroccoSolo solo_;
	protected Solo solo_;

	final static
	private String TAG = "podtest";

	public TestPodplayerExpActivity() {
		super("com.mamewo.podplayer0", PodplayerExpActivity.class);
	}

	@Override
	public void setUp() throws Exception {
		//solo_ = new SciroccoSolo(getInstrumentation(), getActivity(), "com.mamewo.podtest");
		Config config = new Config();
		config.screenshotFileType = ScreenshotFileType.PNG;
		config.screenshotSavePath = new File(Environment.getExternalStorageDirectory(), "Robotium-Screenshots").getPath();
		config.shouldScroll = false;

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
		//solo_.takeScreenShot();
		assertTrue(((ToggleButton)playButton).isChecked());
		//pause for next test
		solo_.clickOnView(playButton);
		solo_.sleep(1000);
	}

	public void testFinish() {
		solo_.sleep(500);
		//TODO: use resource
		solo_.clickOnMenuItem("Exit");
	}

	public void testAbortReload() throws Exception {
		solo_.sleep(5000);
//		solo_.takeScreenShot();
		View reloadButton = solo_.getView(R.id.reload_button);
		//TODO: check image source
		solo_.clickOnView(reloadButton);
		//TODO: check image source
		solo_.sleep(500);
//		solo_.takeScreenShot();
		//TODO: this does not work...
		solo_.scrollUpList(0);
		solo_.sleep(10000);
	}

	public void testExpandAll() throws Exception {
		solo_.sleep(2500);
		View expandButton = solo_.getView(R.id.expand_button);
		solo_.clickOnView(expandButton);
		solo_.sleep(500);
//		solo_.takeScreenShot();
		View collapseButton = solo_.getView(R.id.collapse_button);
		solo_.clickOnView(collapseButton);
		solo_.sleep(500);
//		solo_.takeScreenShot();
	}
	//TODO: expand/collapse prefrence test
	
	//-----------------------
	
	public void testMainScreenshot() throws Exception {
		solo_.sleep(500);
		View playButton = solo_.getView(R.id.play_button);
		solo_.clickOnView(playButton);
		solo_.sendKey(Solo.MENU);
		solo_.sleep(200);
		//solo_.takeScreenShot();
		solo_.sendKey(Solo.MENU);
		solo_.sleep(200);
		solo_.clickOnView(playButton);
	}
}
