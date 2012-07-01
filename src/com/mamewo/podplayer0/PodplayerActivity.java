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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
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
	private static String TAG = "podcast";
	private Thread worker_;
	private PlayerService player_ = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		worker_ = null;
		setContentView(R.layout.main);
		loadButton_ = (Button) findViewById(R.id.load_button);
		loadButton_.setOnClickListener(this);
		playButton_ = (ToggleButton) findViewById(R.id.play_button);
		playButton_.setOnClickListener(this);
		nextButton_ = (ImageButton) findViewById(R.id.next_button);
		nextButton_.setOnClickListener(this);
		loadingIcon_ = (ProgressBar) findViewById(R.id.loading_icon);
		loadingIcon_.setOnClickListener(this);
		listview_ = (ListView) findViewById(R.id.listView1);
		listview_.setOnItemClickListener(this);
		adapter_ =
			new ArrayAdapter<PodInfo>(this, android.R.layout.simple_list_item_1);
		listview_.setAdapter(adapter_);
		handler_ = new Handler();
		try {
			podcastURLlist_ = 
					new URL[]{ new URL("http://www.nhk.or.jp/rj/podcast/rss/english.xml"),
							   new URL("http://feeds.voanews.com/ps/getRSS?client=Standard&PID=_veJ_N_q3IUpwj2Z5GBO2DYqWDEodojd&startIndex=1&endIndex=500") };
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		Intent intent = new Intent(this, PlayerService.class);
		boolean result = bindService(intent, this, Context.BIND_AUTO_CREATE);
		Log.d(TAG, "bindService: " + result);
	}
	
	@Override
	public void onStart(){
		super.onStart();
		updatePodcast();
	}
	
	@Override
	public void onDestroy(){
		player_ = null;
		super.onDestroy();
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

	@Override
	public void run() {
		XmlPullParserFactory factory;
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
				boolean inTitle = false;
				while(eventType != XmlPullParser.END_DOCUMENT) {
					//Log.d(TAG, "eventType: " + eventType);
					if(eventType == XmlPullParser.START_TAG) {
						if("title".equalsIgnoreCase(parser.getName())) {
							inTitle = true;
						}
						else if("enclosure".equalsIgnoreCase(parser.getName())) {
							podcastURL = parser.getAttributeValue(null, "url");
						}
					}
					else if(eventType == XmlPullParser.TEXT) {
						if(inTitle) {
							title = parser.getText();
						}
					}
					else if(eventType == XmlPullParser.END_TAG) {
						if("item".equalsIgnoreCase(parser.getName())) {
							if(podcastURL != null) {
								if(title == null) {
									title = podcastURL;
								}
								final PodInfo info = new PodInfo(podcastURL, title);
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
						else if ("title".equalsIgnoreCase(parser.getName())) {
							inTitle = false;
						}
					}
					eventType = parser.next();
				}
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (XmlPullParserException e) {
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
				handler_.post(new Runnable() {
					@Override
					public void run() {
						loadingIcon_.setVisibility(View.INVISIBLE);
					}
				});
			}
		}
	}
	
		
	public static void showMessage(Context c, String message) {
		Toast.makeText(c, message, Toast.LENGTH_LONG).show();
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder binder) {
		player_ = ((PlayerService.LocalBinder)binder).getService();
		player_.setOnStartMusicListener(this);
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
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
}
