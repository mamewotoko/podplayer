package com.mamewo.podplayer0.tests;

import junit.framework.Assert;

import com.robotium.solo.Solo;
import com.robotium.solo.Solo.Config;
import com.robotium.solo.Solo.Config.ScreenshotFileType;
import android.os.Environment;
import java.io.File;

import com.mamewo.lib.podcast_parser.PodcastInfo;
import com.mamewo.podplayer0.PodcastListPreference;
import com.mamewo.podplayer0.R;

import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.squareup.spoon.Spoon;

public class TestPodcastListPreference
	extends ActivityInstrumentationTestCase2<PodcastListPreference>
{
	private Solo solo_;
	static final
	private String TAG = "podtest";
	
	public TestPodcastListPreference() {
		super("com.mamewo.podplayer0", PodcastListPreference.class);
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
		} catch (Throwable e) {
			Log.i(TAG, "tearDown error", e);
		}
		if (! getActivity().isFinishing()) {
			getActivity().finish();
		}
		super.tearDown();
	}
	
	public void testAddFail() {
		solo_.sleep(1000);
		ListAdapter adapter = solo_.getCurrentViews(ListView.class, false).get(0).getAdapter();
		PodcastInfo prevInfo = (PodcastInfo)adapter.getItem(adapter.getCount()-1);
		String url = "http://www.google.co.jp/";
		int prevCount = adapter.getCount();
		solo_.enterText(solo_.getEditText(0), url);
		Spoon.screenshot(solo_.getCurrentActivity(), "add_fail");
		solo_.clickOnView(solo_.getView(R.id.add_podcast_button));
		solo_.waitForDialogToClose(10000);
		Assert.assertEquals(prevCount, adapter.getCount());
		PodcastInfo info = (PodcastInfo)adapter.getItem(adapter.getCount()-1);
		Spoon.screenshot(solo_.getCurrentActivity(), "add_fail");
		Assert.assertEquals("check url", prevInfo, info);
	}

	public void testAddSuccess() {
		solo_.sleep(1000);
		ListAdapter adapter = solo_.getCurrentViews(ListView.class, false).get(0).getAdapter();
		String url = "http://www.tfm.co.jp/podcasts/avanti/podcast.xml";
		int prevCount = adapter.getCount();
		solo_.enterText(solo_.getEditText(0), url);
		solo_.sleep(500);
		Spoon.screenshot(solo_.getCurrentActivity(), "add_success");       
		solo_.clickOnView(solo_.getView(R.id.add_podcast_button));
		solo_.waitForDialogToClose(20000);
		Assert.assertEquals(prevCount+1, adapter.getCount());
		PodcastInfo info = (PodcastInfo)adapter.getItem(adapter.getCount()-1);
		Log.d(TAG, "add succ: " + info.title_);
		Spoon.screenshot(solo_.getCurrentActivity(), "add_success");
		Assert.assertEquals("check url", url, info.url_.toString());
	}

	public void testAddWithBOMSuccess() {
		solo_.sleep(1000);
		ListAdapter adapter = solo_.getCurrentViews(ListView.class, false).get(0).getAdapter();
		String url = "http://www.fmtoyama.co.jp/contents/podcast/podcast_24.xml";
		int prevCount = adapter.getCount();
		solo_.enterText(solo_.getEditText(0), url);
		solo_.sleep(500);
		Spoon.screenshot(solo_.getCurrentActivity(), "add_with_bom_success");       
		solo_.clickOnView(solo_.getView(R.id.add_podcast_button));
		solo_.waitForDialogToClose(20000);
		Assert.assertEquals(prevCount+1, adapter.getCount());
		PodcastInfo info = (PodcastInfo)adapter.getItem(adapter.getCount()-1);
		Log.d(TAG, "add succ: " + info.title_);
		Spoon.screenshot(solo_.getCurrentActivity(), "add_with_bom_success");
		Assert.assertEquals("check url", url, info.url_.toString());
	}
	
	public void testDelete3() {
		solo_.sleep(1000);
		ListAdapter adapter = solo_.getCurrentViews(ListView.class, false).get(0).getAdapter();
		int count = adapter.getCount();
		solo_.clickLongInList(3);
		solo_.sleep(200);
		Spoon.screenshot(solo_.getCurrentActivity(), "delete3");
		solo_.clickInList(PodcastListPreference.REMOVE_OPERATION+1);
		solo_.sleep(200);
		Spoon.screenshot(solo_.getCurrentActivity(), "delete3");
		Assert.assertEquals(count-1, adapter.getCount());
	}
	
	public void testUp() {
		solo_.sleep(1000);
		ListAdapter adapter = solo_.getCurrentViews(ListView.class, false).get(0).getAdapter();
		PodcastInfo info = (PodcastInfo)adapter.getItem(1);
		solo_.clickLongInList(2);
		solo_.sleep(200);
		Spoon.screenshot(solo_.getCurrentActivity(), "up");
		solo_.clickInList(PodcastListPreference.UP_OPERATION+1);
		solo_.sleep(200);
		Spoon.screenshot(solo_.getCurrentActivity(), "up");
		Assert.assertEquals(info, adapter.getItem(0));
	}
	
	public void testDown() {
		solo_.sleep(1000);
		ListAdapter adapter = solo_.getCurrentViews(ListView.class, false).get(0).getAdapter();
		PodcastInfo info = (PodcastInfo)adapter.getItem(0);
		solo_.clickLongInList(1);
		solo_.sleep(200);
		Spoon.screenshot(solo_.getCurrentActivity(), "down");      
		solo_.clickInList(PodcastListPreference.DOWN_OPERATION+1);
		solo_.sleep(200);
		Spoon.screenshot(solo_.getCurrentActivity(), "down");        
		Assert.assertEquals(info, adapter.getItem(1));
	}
}
