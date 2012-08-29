package com.mamewo.podplayer0;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.CheckBox;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;

public class PodcastListEditorActivity
	extends Activity
	implements OnClickListener,
	OnCancelListener
{
	final static
	private String TAG = "podcast";
	
	private Button checkButton_;
	private EditText urlEdit_;
	private CheckTask task_;
	private ProgressDialog dialog_;
	private PodcastInfoAdapter adapter_;
	private ListView podcastListView_;
	static final
	private String CONFIG_FILENAME = "podcast.json";
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.podlist_editor);
		checkButton_ = (Button) findViewById(R.id.check_url_button);
		checkButton_.setOnClickListener(this);
		urlEdit_ = (EditText) findViewById(R.id.url_edit);
		dialog_ = new ProgressDialog(this);
		dialog_.setOnCancelListener(this);
		dialog_.setCancelable(true);
		dialog_.setCanceledOnTouchOutside(true);
		dialog_.setTitle("Checking URL...");
		dialog_.setMessage("Checking that given url refers to a podcast");
		adapter_ = new PodcastInfoAdapter(this);
		File configFile = getFileStreamPath(CONFIG_FILENAME);
		if (configFile.exists()) {
			try {
				loadSetting();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else {
			String[] allTitles = getResources().getStringArray(R.array.pref_podcastlist_keys);
			String[] allURLs = getResources().getStringArray(R.array.pref_podcastlist_urls);

			for (int i = 0; i < allTitles.length; i++) {
				String title = allTitles[i];
				URL url = null;
				try {
					url = new URL(allURLs[i]);
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
				//TODO: get config
				PodcastInfo info = new PodcastInfo(title, url, null, true);
				adapter_.add(info);
			}
		}
		podcastListView_ = (ListView) findViewById(R.id.podlist);
		podcastListView_.setAdapter(adapter_);
	}

	@Override
	public void onStop() {
		super.onStop();
		try {
			saveSetting();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void onClick(View view) {
		if (view == checkButton_) {
			String urlStr = urlEdit_.getText().toString();
			//check url
			URL url = null;
			try {
				url = new URL(urlStr);
			}
			catch (MalformedURLException e) {
				showMessage("Malformed URL");
				return;
			}
			URL[] urlList = new URL[] { url };
			dialog_.show();
			task_ = new CheckTask();
			task_.execute(urlList);
		}
	}

	public class CheckTask
		extends AsyncTask<URL, PodcastInfo, Void>
	{
		@Override
		protected Void doInBackground(URL... urllist) {
			XmlPullParserFactory factory;
			try {
				factory = XmlPullParserFactory.newInstance();
			}
			catch (XmlPullParserException e1) {
				Log.i(TAG, "cannot get xml parser", e1);
				return null;
			}
			for(int i = 0; i < urllist.length; i++) {
				URL url = urllist[i];
				if(isCancelled()){
					break;
				}
				Log.d(TAG, "get URL: " + url);
				InputStream is = null;
				int numItems = 0;
				BitmapDrawable bitmap = null;
				String title = null;
				try {
					URLConnection conn = url.openConnection();
					conn.setReadTimeout(60 * 1000);
					is = conn.getInputStream();
					XmlPullParser parser = factory.newPullParser();
					//TODO: use reader or give correct encoding
					parser.setInput(is, "UTF-8");
					boolean inTitle = false;
					int eventType;
					while((eventType = parser.getEventType()) != XmlPullParser.END_DOCUMENT && !isCancelled()) {
						if(eventType == XmlPullParser.START_TAG) {
							String currentName = parser.getName();
							if("enclosure".equalsIgnoreCase(currentName)) {
								numItems++;
							}
							else if("itunes:image".equalsIgnoreCase(currentName)) {
								if (null == bitmap) {
									URL iconURL = new URL(parser.getAttributeValue(null, "href"));
									bitmap = BaseGetPodcastTask.downloadIcon(PodcastListEditorActivity.this, iconURL, 60);
								}
							}
							else {
								inTitle = "title".equalsIgnoreCase(currentName);
							}
						}
						else if (eventType == XmlPullParser.TEXT) {
							if (null == title && inTitle) {
								title = parser.getText();
								Log.d(TAG, "Title: " + title);
							}
						}
						eventType = parser.next();
					}
					if (numItems > 0 && null != title) {
						Log.d(TAG, "publish: " + title);
						publishProgress(new PodcastInfo(title, url, bitmap, true));
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
		protected void onProgressUpdate(PodcastInfo... values){
			PodcastInfo info = values[0];
			adapter_.add(info);
			showMessage("add " + info.title_);
		}
		
		@Override
		protected void onPostExecute(Void result) {
			showMessage("finished");
			task_ = null;
			dialog_.hide();
		}
		
		@Override
		protected void onCancelled() {
			showMessage("cancelled");
			task_ = null;
			dialog_.hide();
		}
	}
	
	public class PodcastInfoAdapter
		extends ArrayAdapter<PodcastInfo>
	{
		public PodcastInfoAdapter(Context context) {
			super(context, R.layout.podcast_select_item);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			if (null == convertView) {
				view = View.inflate(PodcastListEditorActivity.this, R.layout.podcast_select_item, null);
			}
			else {
				view = convertView;
			}
			PodcastInfo info = getItem(position);
			CheckBox check = (CheckBox) view.findViewById(R.id.checkbox);
			Log.d(TAG, "checkbox: " + check);
			//add check
			String title = info.title_;
			if (null == title) {
				title = info.url_.toString();
			}
			check.setText(title);
			check.setChecked(info.enabled_);
			return view;
		}
	}

	private void saveSetting() throws
		JSONException, IOException
	{
		JSONArray array = new JSONArray();
		for (int i = 0; i < adapter_.getCount(); i++) {
			PodcastInfo info = adapter_.getItem(i);
			JSONObject jsonValue = (new JSONObject())
					.accumulate("title", info.title_)
					.accumulate("url", info.url_.toString())
					.accumulate("enabled", info.enabled_);
			array.put(jsonValue);
		}
		String json = array.toString(2);
		Log.d(TAG, "JSON: " + json);
		FileOutputStream fos = openFileOutput(CONFIG_FILENAME, MODE_PRIVATE);
		try{
			fos.write(json.getBytes());
		}
		finally {
			fos.close();
		}
	}
	
	private void loadSetting()
		throws IOException, JSONException
	{
		FileInputStream fis = openFileInput(CONFIG_FILENAME);
		StringBuffer sb = new StringBuffer();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
			String line;
			while (null != (line = reader.readLine())) {
				sb.append(line);
			}
		}
		finally {
			fis.close();
		}
		String json = sb.toString();
		JSONTokener tokener = new JSONTokener(json);
		JSONArray jsonArray = (JSONArray) tokener.nextValue();
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject value = jsonArray.getJSONObject(i);
			String title  = value.getString("title");
			URL url = new URL(value.getString("url"));
			boolean enabled = value.getBoolean("enabled");
			PodcastInfo info = new PodcastInfo(title, url, null, enabled);
			adapter_.add(info);
		}
	}
	
	static
	public class PodcastInfo {
		public String title_;
		public URL url_;
		public BitmapDrawable bitmap_;
		public boolean enabled_;

		public PodcastInfo(String title, URL url, BitmapDrawable bitmap, boolean enabled) {
			title_ = title;
			url_ = url;
			bitmap_ = bitmap;
			enabled_ = enabled;
		}
	}

	public void showMessage(String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		if (null != task_) {
			task_.cancel(true);
		}
	}
}
