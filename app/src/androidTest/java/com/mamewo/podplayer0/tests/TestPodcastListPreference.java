package com.mamewo.podplayer0.tests;

import junit.framework.Assert;

import com.robotium.solo.Solo;
import com.robotium.solo.Solo.Config;

import com.mamewo.podplayer0.db.PodcastRealm;
import com.mamewo.podplayer0.PodcastListPreference;
import com.mamewo.podplayer0.R;

import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Button;
import android.view.View;
import android.widget.EditText;
//import android.support.test.filters.SmallTest;

import com.jraska.falcon.FalconSpoon;

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
        String url = "http://www.google.co.jp/";
        int prevCount = adapter.getCount();
        solo_.enterText((EditText)solo_.getView(R.id.url_edit), url);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "add_fail");
        solo_.clickOnView(solo_.getView(R.id.add_podcast_button));
        solo_.waitForDialogToClose(10000);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "add_fail");        
        Assert.assertEquals(prevCount, adapter.getCount());
    }

    public void testAddSuccess() {
        solo_.sleep(1000);
        ListAdapter adapter = solo_.getCurrentViews(ListView.class, false).get(0).getAdapter();
        String url = "http://www.tfm.co.jp/podcasts/avanti/podcast.xml";
        int prevCount = adapter.getCount();
        solo_.enterText((EditText)solo_.getView(R.id.url_edit), url);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "add_success");
        solo_.clickOnView(solo_.getView(R.id.add_podcast_button));
        solo_.waitForDialogToClose(20000);
        Assert.assertEquals(prevCount+1, adapter.getCount());
        PodcastRealm info = (PodcastRealm)adapter.getItem(adapter.getCount()-1);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "add_success");
        Assert.assertEquals("check url", url, info.getURL().toString());
    }

    public void testAddWithBOMSuccess() {
        solo_.sleep(1000);
        ListAdapter adapter = solo_.getCurrentViews(ListView.class, false).get(0).getAdapter();
        String url = "http://www.fmtoyama.co.jp/contents/podcast/podcast_24.xml";
        int prevCount = adapter.getCount();
        solo_.enterText((EditText)solo_.getView(R.id.url_edit), url);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "add_with_bom_success");       
        solo_.clickOnView(solo_.getView(R.id.add_podcast_button));
        solo_.waitForDialogToClose(20000);
        Assert.assertEquals(prevCount+1, adapter.getCount());
        PodcastRealm info = (PodcastRealm)adapter.getItem(adapter.getCount()-1);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "add_with_bom_success");
        Assert.assertEquals("check url", url, info.getURL().toString());
    }

    //@SmallTest
    public void testAddDuplicate() {
        solo_.sleep(1000);
        ListAdapter adapter = solo_.getCurrentViews(ListView.class, false).get(0).getAdapter();
        String url = "http://www.nhk.or.jp/rj/podcast/rss/english.xml";
        int prevCount = adapter.getCount();
        solo_.enterText((EditText)solo_.getView(R.id.url_edit), url);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "add_duplicate");
        solo_.clickOnView(solo_.getView(R.id.add_podcast_button));
        solo_.waitForDialogToClose(20000);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "add_duplicate");
        Assert.assertEquals(prevCount, adapter.getCount());
    }

    public void testExpand() {
        //wait loading icon
        solo_.sleep(5000);
        //ListAdapter adapter = solo_.getCurrentViews(ListView.class, false).get(0).getAdapter();
        ListView list = (ListView)solo_.getView(R.id.podlist);
        View v = list.getChildAt(0);
        View button = v.findViewById(R.id.detail_button);
        solo_.clickOnView(button);
        //TODO: list redraw is slow
        solo_.sleep(7000);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "expand");
        // View upButton = v.findViewById(R.id.move_up);
        // View urllabel = v.findViewById(R.id.podcast_url);
        // Assert.assertEquals(View.VISIBLE, urllabel.getVisibility());
        // Assert.assertEquals(View.VISIBLE, upButton.getVisibility());
    }

    public void testDelete() {
        solo_.sleep(1000);
        
        ListView list = (ListView)solo_.getView(R.id.podlist);
        ListAdapter adapter = list.getAdapter();
        int count = adapter.getCount();
        View v = list.getChildAt(0);
        View button = v.findViewById(R.id.detail_button);
        solo_.clickOnView(button);
        //TODO: list redraw is slow
        solo_.sleep(7000);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "delete");

        View deleteButton = v.findViewById(R.id.delete);
        solo_.clickOnView(deleteButton);

        FalconSpoon.screenshot(solo_.getCurrentActivity(), "delete");
        Assert.assertEquals(count-1, adapter.getCount());
    }

    public void testMoveUp() {
        solo_.sleep(1000);
        
        ListView list = (ListView)solo_.getView(R.id.podlist);
        ListAdapter adapter = list.getAdapter();
        int count = adapter.getCount();
        View v = list.getChildAt(0);
        View button = v.findViewById(R.id.detail_button);
        solo_.clickOnView(button);
        //TODO: list redraw is slow
        solo_.sleep(7000);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "up");

        View upButton = v.findViewById(R.id.move_up);
        solo_.clickOnView(upButton);

        FalconSpoon.screenshot(solo_.getCurrentActivity(), "up");
        Assert.assertEquals(count, adapter.getCount());
    }

    public void testMoveDown() {
        solo_.sleep(1000);
        
        ListView list = (ListView)solo_.getView(R.id.podlist);
        ListAdapter adapter = list.getAdapter();
        int count = adapter.getCount();
        View v = list.getChildAt(0);
        View button = v.findViewById(R.id.detail_button);
        solo_.clickOnView(button);
        //TODO: list redraw is slow
        solo_.sleep(7000);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "down");

        View downButton = v.findViewById(R.id.move_down);
        solo_.clickOnView(downButton);

        FalconSpoon.screenshot(solo_.getCurrentActivity(), "down");
        Assert.assertEquals(count, adapter.getCount());
    }


    public void testAddAuth(){
        solo_.sleep(200);
        String url = "http://mamewo.ddo.jp/podcast/auth/sample_podcast.xml";
        solo_.enterText((EditText)solo_.getView(R.id.url_edit), url);
        FalconSpoon.screenshot(solo_.getCurrentActivity(), "add_auth");
        solo_.clickOnView(solo_.getView(R.id.add_podcast_button));
        solo_.waitForDialogToClose(20000);
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
        //check status button
    }  
}
