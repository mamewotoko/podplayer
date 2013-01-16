package com.mamewo.podplayer0;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.database.Cursor;

import com.mamewo.podplayer0.db.Podcast;
import com.mamewo.podplayer0.db.Podcast.PodcastColumns;
import com.mamewo.podplayer0.db.PodcastProvider;
import android.widget.SimpleCursorAdapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
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
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CheckBox;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;

public class PodcastListPreference
	extends Activity
	implements OnClickListener,
	OnItemClickListener,
	OnItemLongClickListener,
			   DialogInterface.OnClickListener,
			   OnCancelListener
{
	final static
	private String TAG = "podplayer";
	
	private Button addButton_;
	private EditText urlEdit_;
	private CheckTask task_;
	private Dialog dialog_;
	//private PodcastInfoAdapter adapter_;
	public SimpleCursorAdapter adapter_;
	private ListView podcastListView_;
	private Bundle bundle_;
	private boolean isChanged_ = false;
	static final
	private String CONFIG_FILENAME = "podcast.json";
	static final
	private int CHECKING_DIALOG = 0;
	static final
	private int DIALOG_REMOVE_PODCAST = 1;
	//position on dialog
	static final
	public int REMOVE_OPERATION = 0;
	static final
	public int UP_OPERATION = 1;
	static final
	public int DOWN_OPERATION = 2;
	final static
	private String PODCAST_SITE_URL = "http://www002.upp.so-net.ne.jp/mamewo/podcast.html";
	final static
		private String[] PROJECTION = new String[] { PodcastColumns._ID, PodcastColumns.TITLE };

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.podlist_editor);
		setTitle(R.string.app_podcastlist_title);
		addButton_ = (Button) findViewById(R.id.add_podcast_button);
		addButton_.setOnClickListener(this);
		urlEdit_ = (EditText) findViewById(R.id.url_edit);
		List<PodcastInfo> list = loadSetting(this);
		podcastListView_ = (ListView) findViewById(R.id.podlist);
		
		//adapter_ = new PodcastInfoAdapter(this, list);
		// podcastListView_.setAdapter(adapter_);
		// podcastListView_.setOnItemLongClickListener(this);
		// podcastListView_.setOnItemClickListener(this);
		//TODO: 
		Cursor cursor = managedQuery(Podcast.PodcastColumns.AUTHORITY_URI,
									 PROJECTION, null, null, null);
		adapter_ = new SimpleCursorAdapter(this, R.layout.podcast_select_item, cursor,
										   new String[] { Podcast.PodcastColumns.TITLE },
										   new int[] { R.id.podcast_title_label });
		podcastListView_.setAdapter(adapter_);
		bundle_ = null;
	}
	
	static
	private List<PodcastInfo> defaultPodcastInfoList(Context context) {
		String[] allTitles = context.getResources().getStringArray(R.array.pref_podcastlist_keys);
		String[] allURLs = context.getResources().getStringArray(R.array.pref_podcastlist_urls);
		List<PodcastInfo> list = new ArrayList<PodcastInfo>();
		for (int i = 0; i < allTitles.length; i++) {
			String title = allTitles[i];
			URL url = null;
			try {
				url = new URL(allURLs[i]);
				//TODO: get config and fetch icon
				PodcastInfo info = new PodcastInfo(title, url, null, true);
				list.add(info);
			}
			catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		return list;
	}

	@Override
	public void onStart() {
		super.onStart();
		isChanged_ = false;
	}
	
	@Override
	public void onStop() {
		super.onStop();
		try {
			saveSetting();
		}
		catch (JSONException e) {
			Log.d(TAG, "failed to save podcast list setting");
		}
		catch (IOException e) {
			Log.d(TAG, "failed to save podcast list setting");
		}
		//Ummm..: to call preference listener
		if (isChanged_) {
			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
			boolean prevValue = pref.getBoolean("podcastlist2", true);
			pref.edit().putBoolean("podcastlist2", !prevValue).commit();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.podcastlistmenu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean handled = false;
		switch(item.getItemId()) {
		case R.id.podcast_page_menu:
			Intent i =
				new Intent(Intent.ACTION_VIEW, Uri.parse(PODCAST_SITE_URL));
			startActivity(new Intent(i));
			handled = true;
			break;
		default:
			break;
		}
		return handled;
	}
	
	@Override
	public void onClick(View view) {
		if (view == addButton_) {
			String urlStr = urlEdit_.getText().toString();
			//check url
			URL url = null;
			try {
				url = new URL(urlStr);
			}
			catch (MalformedURLException e) {
				showMessage(getString(R.string.error_malformed_url));
				return;
			}
			URL[] urlList = new URL[] { url };
			showDialog(CHECKING_DIALOG);
			task_ = new CheckTask();
			task_.execute(urlList);
		}
		else if (view.getId() == R.id.checkbox) {
			//umm...
			CheckBox checkbox = (CheckBox) view;
			Log.d(TAG, "checkbox is clicked: " + checkbox.isChecked());
			PodcastInfo info = (PodcastInfo) checkbox.getTag();
			info.enabled_ = !info.enabled_;
			checkbox.setChecked(info.enabled_);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id, Bundle bundle) {
		Log.d(TAG, "onCreateDialog(bundle): " + id);
		Dialog dialog = null;
		switch(id){
		case CHECKING_DIALOG:
			ProgressDialog progressDialog = new ProgressDialog(this);
			progressDialog.setOnCancelListener(this);
			progressDialog.setCancelable(true);
			progressDialog.setCanceledOnTouchOutside(true);
			progressDialog.setTitle(R.string.dialog_checking_podcast_url);
			progressDialog.setMessage(getString(R.string.dialog_checking_podcast_url_body));
			dialog = progressDialog;
			dialog_ = progressDialog;
			break;
		case DIALOG_REMOVE_PODCAST:
			List<String> items = new ArrayList<String>();
			items.add(getString(R.string.remove_operation));
			items.add(getString(R.string.up_operation));
			items.add(getString(R.string.down_operation));
			ListAdapter adapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_item, items);
			dialog = new AlertDialog.Builder(this)
			.setCancelable(true)
			.setAdapter(adapter, this)
			.create();
			break;
		default:
			break;
		}
		return dialog;
	}
	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
		bundle_ = args;
		switch(id){
		case CHECKING_DIALOG:
			dialog_ = dialog;
			break;
		case DIALOG_REMOVE_PODCAST:
			Log.d(TAG, "onPrepareDialog(bundle): " + args.getInt("position"));
			int pos = args.getInt("position");
			//PodcastInfo info = adapter_.getItem(pos);
			//dialog.setTitle(info.title_);
			//TODO: disable up/down?
			break;
		default:
			break;
		}
	}
	
	public class CheckTask
		extends AsyncTask<URL, PodcastInfo, Boolean>
	{
		@Override
		protected Boolean doInBackground(URL... urllist) {
			XmlPullParserFactory factory;
			try {
				factory = XmlPullParserFactory.newInstance();
			}
			catch (XmlPullParserException e1) {
				Log.i(TAG, "cannot get xml parser", e1);
				return false;
			}
			boolean result = false;
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
									bitmap = BaseGetPodcastTask.downloadIcon(PodcastListPreference.this, iconURL, 60);
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
						result = true;
					}
				}
				catch (IOException e) {
					Log.i(TAG, "IOException", e);
					//continue
				}
				catch (XmlPullParserException e) {
					Log.i(TAG, "XmlPullParserException", e);
					//continue
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
			return result;
		}
		
		@Override
		protected void onProgressUpdate(PodcastInfo... values){
			PodcastInfo info = values[0];
			//adapter_.add(info);
			String msg =
					MessageFormat.format(getString(R.string.podcast_added), info.title_);
			showMessage(msg);
			urlEdit_.setText("");
			urlEdit_.clearFocus();
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			task_ = null;
			dialog_.hide();
			dialog_ = null;
			if (!result.booleanValue()) {
				showMessage(getString(R.string.msg_add_podcast_failed));
			}
		}
		
		@Override
		protected void onCancelled() {
			showMessage(getString(R.string.msg_add_podcast_cancelled));
			task_ = null;
			dialog_.hide();
			dialog_ = null;
		}
	}
	
	public class PodcastInfoAdapter
		extends ArrayAdapter<PodcastInfo>
	{
		public PodcastInfoAdapter(Context context) {
			super(context, R.layout.podcast_select_item);
		}

		public PodcastInfoAdapter(Context context, List<PodcastInfo> list) {
			super(context, R.layout.podcast_select_item, list);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			if (null == convertView) {
				view = View.inflate(PodcastListPreference.this, R.layout.podcast_select_item, null);
			}
			else {
				view = convertView;
			}
			PodcastInfo info = getItem(position);
			CheckBox check = (CheckBox) view.findViewById(R.id.checkbox);
			check.setOnClickListener(PodcastListPreference.this);
			check.setTag(info);
			TextView label = (TextView) view.findViewById(R.id.podcast_title_label);
			//add check
			String title = info.title_;
			if (null == title) {
				title = info.url_.toString();
			}
			label.setText(title);
			check.setChecked(info.enabled_);
			return view;
		}
	}

	private void saveSetting() throws
		JSONException, IOException
	{
		JSONArray array = new JSONArray();
		// for (int i = 0; i < adapter_.getCount(); i++) {
		// 	PodcastInfo info = adapter_.getItem(i);
		// 	JSONObject jsonValue = (new JSONObject())
		// 			.accumulate("title", info.title_)
		// 			.accumulate("url", info.url_.toString())
		// 			.accumulate("enabled", info.enabled_);
		// 	array.put(jsonValue);
		//}
		String json = array.toString();
		//Log.d(TAG, "JSON: " + json);
		FileOutputStream fos = openFileOutput(CONFIG_FILENAME, MODE_PRIVATE);
		try{
			fos.write(json.getBytes());
		}
		finally {
			fos.close();
		}
	}
	
	static
	public List<PodcastInfo> loadSetting(Context context) {
		List<PodcastInfo> list;
		File configFile = context.getFileStreamPath(CONFIG_FILENAME);
		if (configFile.exists()) {
			try {
				list = loadSettingFromJSONFile(context);
			}
			catch (IOException e) {
				Log.d(TAG, "IOException", e);
				list = defaultPodcastInfoList(context);
			}
			catch (JSONException e) {
				Log.d(TAG, "JSONException", e);
				list = defaultPodcastInfoList(context);
			}
		}
		else {
			list = defaultPodcastInfoList(context);
		}
		return list;
	}
	
	static
	private List<PodcastInfo> loadSettingFromJSONFile(Context context)
			throws IOException, JSONException
	{
		FileInputStream fis = context.openFileInput(CONFIG_FILENAME);
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
		List<PodcastInfo> list = new ArrayList<PodcastInfo>();
		//Log.d(TAG, "JSON size: " + json.length());
		JSONTokener tokener = new JSONTokener(json);
		JSONArray jsonArray = (JSONArray) tokener.nextValue();
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject value = jsonArray.getJSONObject(i);
			String title  = value.getString("title");
			URL url = new URL(value.getString("url"));
			boolean enabled = value.getBoolean("enabled");
			PodcastInfo info = new PodcastInfo(title, url, null, enabled);
			list.add(info);
		}
		return list;
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

	@Override
	public boolean onItemLongClick(AdapterView<?> adapter, View view, int pos,
			long id) {
		Log.d(TAG, "onLongItemClick: " + pos);
		Bundle bundle = new Bundle();
		bundle.putInt("position", pos);
		showDialog(DIALOG_REMOVE_PODCAST, bundle);
		return true;
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		if (null == bundle_) {
			Log.d(TAG, "onClick bundle is null!");
			return;
		}
		int pos = bundle_.getInt("position");
		//PodcastInfo info = adapter_.getItem(pos);
		//Log.d(TAG, "DialogInterface: " + which + " pos: " + pos + " " + info.title_);
		switch(which) {
		case REMOVE_OPERATION:
			//Log.d(TAG, "onClick REMOVE: " + pos + " " + info.title_);
			//adapter_.remove(info);
			//adapter_.notifyDataSetChanged();
			isChanged_ = true;
			break;
		case UP_OPERATION:
			Log.d(TAG, "dialog.onClick UP");
			if(pos == 0){
				break;
			}
			//adapter_.remove(info);
			// adapter_.insert(info, pos - 1);
			//adapter_.notifyDataSetChanged();
			isChanged_ = true;
			break;
		case DOWN_OPERATION:
			Log.d(TAG, "dialog.onClick DOWN");
			// if(pos == adapter_.getCount() - 1){
			// 	break;
			// }
			// adapter_.remove(info);
			// adapter_.insert(info, pos + 1);
			// adapter_.notifyDataSetChanged();
			// isChanged_ = true;
			break;
		default:
			break;
		}
		bundle_ = null;
	}

	@Override
	public void onItemClick(AdapterView<?> adapter, View parent, int pos, long id) {
		CheckBox checkbox = (CheckBox) parent.findViewById(R.id.checkbox);
		PodcastInfo info = (PodcastInfo) adapter.getItemAtPosition(pos);
		info.enabled_ = !info.enabled_;
		checkbox.setChecked(info.enabled_);
		//TODO: check total state
		isChanged_ = true;
	}
}
