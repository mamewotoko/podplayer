package com.mamewo.podplayer0.tests;

import java.util.List;
import java.io.File;

import com.robotium.solo.Solo;
import com.robotium.solo.Solo.Config;
import android.os.Environment;
import android.content.res.Resources;

import com.squareup.spoon.Spoon;

import com.mamewo.podplayer0.PodcastListPreference;
import com.mamewo.podplayer0.PodplayerActivity;
import com.mamewo.podplayer0.R;

import junit.framework.Assert;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class TestBugActivity
	extends ActivityInstrumentationTestCase2<PodplayerActivity>
{
	protected Solo solo_;
	protected Resources res_;
		
	final static
	private String TAG = "podtest";

	public TestBugActivity() {
		super("com.mamewo.podplayer0", PodplayerActivity.class);
	}

	@Override
	public void setUp() throws Exception {
		//solo_ = new SciroccoSolo(getInstrumentation(), getActivity(), "com.mamewo.podtest");
		Config config = new Config();
		// config.screenshotFileType = ScreenshotFileType.PNG;
		// config.screenshotSavePath = new File(Environment.getExternalStorageDirectory(), "Robotium-Screenshots").getPath();
		// Log.d(TAG, "screenshotpath:"+config.screenshotSavePath.toString());
		// config.shouldScroll = false;
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

	public boolean selectPreference(String targetTitle) {
		TextView view = null;
		solo_.waitForActivity("PodplayerPrefrence", 3000);
		do {
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
  
    
	public void testIssue1() throws Exception {
		solo_.sleep(10000);
		//View playButton = solo_.getView(R.id.play_button);
		//solo_.clickOnView(playButton);
		// solo_.sendKey(Solo.MENU);
		// solo_.sleep(1000);
		// solo_.sendKey(Solo.MENU);
		// solo_.sleep(300);
		// solo_.clickOnView(playButton);
		// solo_.sleep(300);
		//assertFalse(((ToggleButton)playButton).isChecked());
		Spoon.screenshot(solo_.getCurrentActivity(), "main0");
        //TODO: scroll down?

		solo_.clickOnMenuItem(res_.getString(R.string.preference_menu));
        selectPreference(res_.getString(R.string.pref_podcastlist_title));
        //TODO: uncheck
		solo_.sleep(300);
		Spoon.screenshot(solo_.getCurrentActivity(), "podcastlist_editor0");
		solo_.clickInList(1);
		solo_.sleep(300);
		Spoon.screenshot(solo_.getCurrentActivity(), "podcastlist_editor1");
        solo_.goBack();
        solo_.goBack();
		Spoon.screenshot(solo_.getCurrentActivity(), "podcastlist_editor2");
        //

		View selector = solo_.getView(R.id.podcast_selector);
        solo_.clickOnView(selector);
		Spoon.screenshot(solo_.getCurrentActivity(), "spinner");
	}
}
