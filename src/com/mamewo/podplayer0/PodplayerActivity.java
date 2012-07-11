package com.mamewo.podplayer0;

/**
 * @author Takashi Masuyama <mamewotoko@gmail.com>
 * http://www002.upp.so-net.ne.jp/mamewo/
 */

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import android.app.Activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.mamewo.podplayer0.PlayerService.PodInfo;

public class PodplayerActivity
	extends Activity
	implements OnClickListener, Runnable, 
	ServiceConnection, OnItemClickListener,
	PlayerService.PlayerStateListener
{
	private URL[] podcastURLlist_;
	private Button loadButton_;
	private ToggleButton playButton_;
	private ImageButton nextButton_;
	private Handler handler_;
	private ListView listview_;
	private ProgressBar loadingIcon_;
	private ArrayAdapter<PodInfo> adapter_;
	private static String TAG = "podplayer";
	private Thread worker_;
	//TODO: wait until player_ is not null (service is connected)
	private PlayerService player_ = null;
	private boolean abortFlag_;
	private boolean finishServiceOnExit = false;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		worker_ = null;
		abortFlag_ = false;
		setContentView(R.layout.main);
		loadButton_ = (Button) findViewById(R.id.load_button);
		loadButton_.setOnClickListener(this);
		playButton_ = (ToggleButton) findViewById(R.id.play_button);
		playButton_.setOnClickListener(this);
		playButton_.setEnabled(false);
		nextButton_ = (ImageButton) findViewById(R.id.next_button);
		nextButton_.setOnClickListener(this);
		loadingIcon_ = (ProgressBar) findViewById(R.id.loading_icon);
		loadingIcon_.setOnClickListener(this);
		listview_ = (ListView) findViewById(R.id.listView1);
		listview_.setOnItemClickListener(this);
		adapter_ = new EpisodeAdapter(this);
		listview_.setAdapter(adapter_);
		handler_ = new Handler();
		try {
			podcastURLlist_ = 
					new URL[]{ new URL("http://www.nhk.or.jp/rj/podcast/rss/english.xml"),
							   new URL("http://www.discovery.com/radio/xml/news.xml"),
							   new URL("http://feeds.voanews.com/ps/getRSS?client=Standard&PID=_veJ_N_q3IUpwj2Z5GBO2DYqWDEodojd&startIndex=1&endIndex=500") };
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		Intent intent = new Intent(this, PlayerService.class);
		startService(intent);
		boolean result = bindService(intent, this, Context.BIND_AUTO_CREATE);
		Log.d(TAG, "bindService: " + result);
		updatePodcast();
	}
	
	@Override
	public void onStart(){
		super.onStart();
		updateUI();
	}
	
	@Override
	public void onDestroy(){
		Log.d(TAG, "onDestroy");
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
		if (null != player_) {
			playButton_.setChecked(player_.isPlaying());
		}
	}

	private void updatePodcast(){
		if(null != worker_ && worker_.getState() != Thread.State.TERMINATED){
			showMessage(this, "Other thread running");
		}
		else {
			adapter_.clear();
			worker_ = new Thread(this, "xmlparse");
			worker_.start();
		}
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
		if (v == loadButton_) {
			updatePodcast();
		}
		else if (v == playButton_) {
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
	
	@Override
	public void run() {
		XmlPullParserFactory factory;
		abortFlag_ = false;
		try {
			factory = XmlPullParserFactory.newInstance();
		}
		catch (XmlPullParserException e1) {
			e1.printStackTrace();
			return;
		}

		handler_.post(new Runnable() {
			@Override
			public void run() {
				loadingIcon_.setVisibility(View.VISIBLE);
			}
		});

		for(URL url: podcastURLlist_) {
			if(abortFlag_){
				break;
			}
			Log.d(TAG, "get URL: " + url);
			InputStream is = null;
			try {
				is = url.openConnection().getInputStream();
				//pull parser
				XmlPullParser parser = factory.newPullParser();
				//use reader or give correct encoding
				parser.setInput(is, "UTF-8");
				int eventType = parser.getEventType();
				String title = null;
				String podcastURL = null;
				String pubdate = "";
				TagName tagName = TagName.NONE;
				while(eventType != XmlPullParser.END_DOCUMENT && !abortFlag_) {
					//Log.d(TAG, "eventType: " + eventType);
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
								final PodInfo info = new PodInfo(podcastURL, title, pubdate);
								handler_.post(new Runnable() {
									@Override
									public void run() {
										//Log.d(TAG, "add: " + info.url_);
										adapter_.add(info);
									}
								});
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
				notifyStopLoading();
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (XmlPullParserException e) {
				notifyStopLoading();
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			finally {
				if(null != is) {
					try {
						is.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		notifyStopLoading();
	}
	
	private void notifyStopLoading(){
		handler_.post(new Runnable() {
			@Override
			public void run() {
				loadingIcon_.setVisibility(View.INVISIBLE);
			}
		});
	}
	
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
		updatePlaylist();
		player_.playNth(pos);
		playButton_.setChecked(player_.isPlaying());
	}

	@Override
	public void onStartMusic(PodInfo info) {
		loadingIcon_.setVisibility(View.INVISIBLE);
		playButton_.setChecked(true);
	}

	@Override
	public void onStartLoadingMusic(PodInfo info) {
		loadingIcon_.setVisibility(View.VISIBLE);
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
			return view;
		}
	}

	@Override
	public void onStopMusic() {
		Log.d(TAG, "onStopMusic");
		updateUI();
	}
}
