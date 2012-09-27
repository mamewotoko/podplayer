package com.mamewo.podplayer0.tests;

import junit.framework.Assert;

import com.mamewo.podplayer0.PodcastInfo;
import com.mamewo.podplayer0.PodcastListPreference;
import com.mamewo.podplayer0.R;

import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.widget.ListAdapter;
import asia.sonix.scirocco.SciroccoSolo;

public class PodcastListPreferenceTest
	extends ActivityInstrumentationTestCase2<PodcastListPreference>
{
	private SciroccoSolo solo_;
	static final
	private String TAG = "podtest";
	
	public PodcastListPreferenceTest() {
		super("com.mamewo.podplayer0", PodcastListPreference.class);
	}

	@Override
	public void setUp() {
		solo_ = new SciroccoSolo(getInstrumentation(), getActivity(), "com.mamewo.podtest");
		solo_.sleep(500);
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
		ListAdapter adapter = solo_.getCurrentListViews().get(0).getAdapter();
		int prevCount = adapter.getCount();
		solo_.getEditText(0).setText("http://www.google.co.jp/");
		solo_.sleep(6000);
		Assert.assertEquals(prevCount, adapter.getCount());
	}

	public void testAddSuccess() {
		ListAdapter adapter = solo_.getCurrentListViews().get(0).getAdapter();
		String url = "http://www.tfm.co.jp/podcasts/avanti/podcast.xml";
		int prevCount = adapter.getCount();
		solo_.enterText(solo_.getEditText(0), url);
		solo_.clickOnView(solo_.getView(R.id.add_podcast_button));
		solo_.waitForDialogToClose(20000);
		Assert.assertEquals(prevCount+1, adapter.getCount());
//		PodcastInfo info = (PodcastInfo)adapter.getItem(adapter.getCount()-1);
//		Log.d(TAG, "add succ: " + info.title_);
//		Assert.assertEquals("check url", url, info.url_);
	}
	
//	public void testDelete() {
//		
//	}
}
