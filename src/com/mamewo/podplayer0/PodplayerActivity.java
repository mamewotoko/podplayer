package com.mamewo.podplayer0;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

public class PodplayerActivity
	extends Activity
	implements OnClickListener, Runnable, ServiceConnection
{
	private URL[] podcastURLlist_;
	private Button loadButton_;
	private Button playButton_;
	private Button stopButton_;
	private Handler handler_;
	private ListView listview_;
	private ArrayAdapter<URL> adapter_;
	private static String TAG = "podcast";
	private Thread worker_;
	private PlayerService player_ = null;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		worker_ = null;
		setContentView(R.layout.main);
		loadButton_ = (Button) findViewById(R.id.load_button);
		loadButton_.setOnClickListener(this);
		playButton_ = (Button) findViewById(R.id.play_button);
		playButton_.setOnClickListener(this);
		stopButton_ = (Button) findViewById(R.id.stop_button);
		stopButton_.setOnClickListener(this);
		listview_ = (ListView) findViewById(R.id.listView1);
		adapter_ =
			new ArrayAdapter<URL>(this, android.R.layout.simple_list_item_1);
		listview_.setAdapter(adapter_);
		handler_ = new Handler();
		try {
			podcastURLlist_ = 
					new URL[]{ new URL("http://www.nhk.or.jp/rj/podcast/rss/english.xml") };
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		Intent intent = new Intent(this, PlayerService.class);
		boolean result = bindService(intent, this, Context.BIND_AUTO_CREATE);
		Log.d(TAG, "bindService: " + result);
	}

	@Override
	public void onClick(View v) {
		//add option to load onStart
		if (v == loadButton_) {
			if(null != worker_ && worker_.getState() != Thread.State.TERMINATED){
				showMessage(this, "Other thread running");
			}
			else {
				//adapter_.clear();
				worker_ = new Thread(this, "xmlparse");
				worker_.start();
			}
		}
		else if (v == playButton_) {
			ArrayList<URL> playlist = new ArrayList<URL>();
			for (int i = 0; i < adapter_.getCount(); i++) {
				playlist.add(adapter_.getItem(i));
			}
			player_.playMusic(playlist);
		}
		else if (v == stopButton_) {
			player_.stopMusic();
		}
	}

	@Override
	public void run() {
		Log.d(TAG, "run");
		XmlPullParserFactory factory;
		try {
			factory = XmlPullParserFactory.newInstance();
		}
		catch (XmlPullParserException e1) {
			e1.printStackTrace();
			return;
		}

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
				while(eventType != XmlPullParser.END_DOCUMENT) {
					//Log.d(TAG, "eventType: " + eventType);
					if(eventType == XmlPullParser.START_TAG) {
						if("enclosure".equalsIgnoreCase(parser.getName())) {
							final String podcastURL = parser.getAttributeValue(null, "url");
							final URL purl = new URL(podcastURL);
							//add to UI
							handler_.post(new Runnable() {
								@Override
								public void run() {
									Log.d(TAG, "add: " + podcastURL);
									adapter_.add(purl);
								}
							});
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
			}
		}
	}
	
	public static void showMessage(Context c, String message) {
		Toast.makeText(c, message, Toast.LENGTH_LONG).show();
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder binder) {
		player_ = ((PlayerService.LocalBinder)binder).getService();
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		player_ = null;
	}
}
