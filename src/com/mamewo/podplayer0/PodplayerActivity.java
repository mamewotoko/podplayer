package com.mamewo.podplayer0;

/**
 * @author Takashi Masuyama <mamewotoko@gmail.com>
 * http://www002.upp.so-net.ne.jp/mamewo/
 */

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import android.app.Activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.mamewo.podplayer0.PlayerService.PodInfo;
import com.markupartist.android.widget.PullToRefreshListView;

public class PodplayerActivity
	extends Activity
	implements OnClickListener,
	ServiceConnection, OnItemClickListener,
	PlayerService.PlayerStateListener,
	OnSharedPreferenceChangeListener,
	PullToRefreshListView.OnRefreshListener,
	PullToRefreshListView.OnCancelListener
{
	private List<URL> podcastURLlist_;
	private ToggleButton playButton_;
	private ImageButton nextButton_;
	private PullToRefreshListView episodeList_;
	private ArrayAdapter<PodInfo> adapter_;
	//TODO: wait until player_ is not null (service is connected)
	private PlayerService player_ = null;
	private boolean finishServiceOnExit = false;
	private PodInfo currentPodInfo_;
	private GetEpisodeTask loadTask_;
	static final
	private int NET_READ_TIMEOUT_MILLIS = 30 * 1000;

	final static
	private String TAG = "podplayer";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		loadTask_ = null;
		playButton_ = (ToggleButton) findViewById(R.id.play_button);
		playButton_.setOnClickListener(this);
		playButton_.setEnabled(false);
		nextButton_ = (ImageButton) findViewById(R.id.next_button);
		nextButton_.setOnClickListener(this);
		episodeList_ = (PullToRefreshListView) findViewById(R.id.episode_list);
		episodeList_.setOnItemClickListener(this);
		episodeList_.setOnRefreshListener(this);
		episodeList_.setOnCancelListener(this);
		adapter_ = new EpisodeAdapter(this);
		episodeList_.setAdapter(adapter_);
		
		Intent intent = new Intent(this, PlayerService.class);
		startService(intent);
		boolean result = bindService(intent, this, Context.BIND_AUTO_CREATE);
		Log.d(TAG, "bindService: " + result);
		SharedPreferences pref=
				PreferenceManager.getDefaultSharedPreferences(this);
		pref.registerOnSharedPreferenceChangeListener(this);
		syncPreference(pref, "all");
	}
	
	@Override
	public void onStart(){
		super.onStart();
		updateUI();
		SharedPreferences pref =
				PreferenceManager.getDefaultSharedPreferences(this);
		boolean doLoad = pref.getBoolean("load_on_start", true);
		if(doLoad && adapter_.getCount() == 0){
			//umm....
			episodeList_.prepareForRefresh();
			updatePodcast();
		}
	}
	
	@Override
	public void onDestroy(){
		SharedPreferences pref=
				PreferenceManager.getDefaultSharedPreferences(this);
		pref.unregisterOnSharedPreferenceChangeListener(this);
		boolean playing = player_.isPlaying();
		if(finishServiceOnExit && playing) {
			player_.stopMusic();
		}
		unbindService(this);
		if (finishServiceOnExit || ! playing) {
			Intent intent = new Intent(this, PlayerService.class);
			stopService(intent);
		}
		super.onDestroy();
	}

	private void updateUI() {
		if(null == player_) {
			return;
		}
		adapter_.notifyDataSetChanged();
		playButton_.setChecked(player_.isPlaying());
	}

	private void updatePodcast(){
		if(null != loadTask_ && loadTask_.getStatus() == AsyncTask.Status.RUNNING){
			Log.d(TAG, "Already loading");
			return;
		}
		adapter_.clear();
		loadTask_ = new GetEpisodeTask();
		loadTask_.execute();
	}
	
	private void updatePlaylist() {
		List<PodInfo> playlist = new ArrayList<PodInfo>();
		for (int i = 0; i < adapter_.getCount(); i++) {
			playlist.add(adapter_.getItem(i));
		}
		player_.setPlaylist(playlist);
	}
	
	@Override
	public void onClick(View v) {
		//add option to load onStart
		if (v == playButton_) {
			if(player_.isPlaying()) {
				player_.stopMusic();
			}
			else {
				updatePlaylist();
				player_.playNth(0);
			}
			playButton_.setChecked(player_.isPlaying());
		}
		else if (v == nextButton_) {
			if(player_.isPlaying()) {
				player_.playNext();
			}
		}
	}

	enum TagName {
		TITLE, PUBDATE, NONE
	};
	
	public static void showMessage(Context c, String message) {
		Toast.makeText(c, message, Toast.LENGTH_LONG).show();
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder binder) {
		player_ = ((PlayerService.LocalBinder)binder).getService();
		player_.setOnStartMusicListener(this);
		playButton_.setEnabled(true);
		updateUI();
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		player_.clearOnStartMusicListener();
		player_ = null;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
		//refresh header is added....
		if(currentPodInfo_ == adapter_.getItem(pos-1)) {
			player_.pauseMusic();
		}
		else {
			updatePlaylist();
			player_.playNth(pos-1);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mainmenu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean handled = false;
		switch(item.getItemId()) {
		case R.id.exit_menu:
			finishServiceOnExit = true;
			finish();
			handled = true;
			break;
		case R.id.pref_menu:
			startActivity(new Intent(this, PodplayerPreference.class));
			handled = true;
			break;
		}
		return handled;
	}
	
	public class EpisodeAdapter
		extends ArrayAdapter<PodInfo> {

		public EpisodeAdapter(Context context) {
			super(context, R.layout.epsode_item);
		}
		
		//TODO: optimize
		@Override
		public View getView (int position, View convertView, ViewGroup parent) {
			View view;
			if (null == convertView) {
				view = View.inflate(PodplayerActivity.this, R.layout.epsode_item, null);
			}
			else {
				view = convertView;
			}
			PodInfo info = getItem(position);
			TextView titleView = (TextView)view.findViewById(R.id.episode_title);
			TextView timeView = (TextView)view.findViewById(R.id.episode_time);
			titleView.setText(info.title_);
			timeView.setText(info.pubdate_);
			ImageView icon = (ImageView)view.findViewById(R.id.play_icon);
			if(currentPodInfo_ == info) {
				icon.setVisibility(View.VISIBLE);
			}
			else {
				icon.setVisibility(View.GONE);
			}
			return view;
		}
	}

	//UI is updated in following callback methods
	@Override
	public void onStartMusic(PodInfo info) {
		updateUI();
	}

	@Override
	public void onStartLoadingMusic(PodInfo info) {
		currentPodInfo_ = info;
		updateUI();
	}

	@Override
	public void onStopMusic() {
		Log.d(TAG, "onStopMusic");
		currentPodInfo_ = null;
		updateUI();
	}
	// end of callback methods

	private void syncPreference(SharedPreferences pref, String key){
		boolean updateAll = "all".equals(key);
		if(updateAll || "podcastlist".equals(key)) {
			String prefURLString = pref.getString("podcastlist", "");
			Log.d(TAG, "prefURLString: " + prefURLString);
			String[] list = prefURLString.split(MultiListPreference.SEPARATOR);
			podcastURLlist_ = new ArrayList<URL>();
			for (String url: list) {
				try {
					podcastURLlist_.add(new URL(url));
				}
				catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
		Log.d(TAG, "onSharedPreferneceChanged: " + key);
		syncPreference(pref, key);
	}
	
	private class GetEpisodeTask
		extends AsyncTask<Void, PodInfo, Void> {
		
		@Override
		protected Void doInBackground(Void... arg0) {
			XmlPullParserFactory factory;
			try {
				factory = XmlPullParserFactory.newInstance();
			}
			catch (XmlPullParserException e1) {
				Log.i(TAG, "cannot get xml parser", e1);
				return null;
			}

			for(URL url: podcastURLlist_) {
				if(isCancelled()){
					break;
				}
				Log.d(TAG, "get URL: " + url);
				InputStream is = null;
				try {
					Proxy proxy = Proxy.NO_PROXY;
					SharedPreferences pref =
							PreferenceManager.getDefaultSharedPreferences(PodplayerActivity.this);
					String proxySetting = pref.getString("pref_proxy", "");
					Log.d(TAG, "pref_Proxy: " + proxySetting);
					String[] proxyValue = proxySetting.split(":");
					if(proxyValue != null && proxyValue.length == 2) {
						//TODO: resolve host name!?
						proxy = new Proxy(Proxy.Type.HTTP, 
										  new InetSocketAddress(proxyValue[0], Integer.valueOf(proxyValue[1])));
					}
					URLConnection conn = url.openConnection(proxy);
					conn.setReadTimeout(NET_READ_TIMEOUT_MILLIS);
					is = conn.getInputStream();
					XmlPullParser parser = factory.newPullParser();
					//TODO: use reader or give correct encoding
					parser.setInput(is, "UTF-8");
					String title = null;
					String podcastURL = null;
					String pubdate = "";
					TagName tagName = TagName.NONE;
					int eventType;
					while((eventType = parser.getEventType()) != XmlPullParser.END_DOCUMENT && !isCancelled()) {
						if(eventType == XmlPullParser.START_TAG) {
							if("title".equalsIgnoreCase(parser.getName())) {
								tagName = TagName.TITLE;
							}
							else if("pubdate".equalsIgnoreCase(parser.getName())) {
								tagName = TagName.PUBDATE;
							}
							else if("enclosure".equalsIgnoreCase(parser.getName())) {
								podcastURL = parser.getAttributeValue(null, "url");
							}
						}
						else if(eventType == XmlPullParser.TEXT) {
							if(tagName == TagName.TITLE) {
								title = parser.getText();
							}
							else if(tagName == TagName.PUBDATE) {
								//TODO: convert time zone
								pubdate = parser.getText();
							}
						}
						else if(eventType == XmlPullParser.END_TAG) {
							if("item".equalsIgnoreCase(parser.getName())) {
								if(podcastURL != null) {
									if(title == null) {
										title = podcastURL;
									}
									PodInfo info = new PodInfo(podcastURL, title, pubdate);
									publishProgress(info);
								}
								podcastURL = null;
								title = null;
							}
							else if ("title".equalsIgnoreCase(parser.getName())
									|| "pubdate".equalsIgnoreCase(parser.getName())) {
								tagName = TagName.NONE;
							}
						}
						eventType = parser.next();
					}
				}
				catch (IOException e) {
					Log.i(TAG, "IOException", e);
				}
				catch (XmlPullParserException e) {
					Log.i(TAG, "XmlPullParserException", e);
				}
				finally {
					if(null != is) {
						try {
							is.close();
						}
						catch (IOException e) {
							Log.i(TAG, "input stream cannot be close", e);
						}
					}
				}
			}
			return null;
		}
		
		@Override
		protected void onProgressUpdate(PodInfo... values){
			for (int i = 0; i < values.length; i++) {
				adapter_.add(values[i]);
			}
		}

		@Override
		protected void onPostExecute(Void result) {
			if(adapter_.isEmpty()) {
				episodeList_.setLastUpdated("");
			}
			else {
				DateFormat df = DateFormat.getDateTimeInstance();
				String dateStr = df.format(new Date());
				episodeList_.setLastUpdated("Last updated: " + dateStr);
			}
			episodeList_.onRefreshComplete();
			loadTask_ = null;
			//TODO: Sync playlist
			updatePlaylist();
		}
	}

	@Override
	public void onRefresh() {
		updatePodcast();
	}

	@Override
	public void onCancel() {
		if (null != loadTask_) {
			loadTask_.cancel(true);
			loadTask_ = null;
		}
	}
}
