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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.input.BOMInputStream;

import okhttp3.Request;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.mamewo.lib.podcast_parser.PodcastInfo;

import static com.mamewo.podplayer0.Const.*;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
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

import okhttp3.OkHttpClient;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

public class PodcastListPreference
	extends AppCompatActivity
    implements OnClickListener,
    OnItemClickListener,
    OnItemLongClickListener,
    DialogInterface.OnClickListener,
    OnCancelListener
{
    private Button addButton_;
    private EditText urlEdit_;
    private CheckTask task_;
    private Dialog dialog_;
    private PodcastInfoAdapter adapter_;
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
    private String PODCAST_SITE_URL = "http://mamewo.ddo.jp/podcast/podcast.html";
    private OkHttpClient client_;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.podlist_editor);
        setTitle(R.string.app_podcastlist_title);
        addButton_ = (Button) findViewById(R.id.add_podcast_button);
        addButton_.setOnClickListener(this);
        urlEdit_ = (EditText) findViewById(R.id.url_edit);
        List<PodcastInfo> list = loadSetting(this);
        adapter_ = new PodcastInfoAdapter(this, list);
        podcastListView_ = (ListView) findViewById(R.id.podlist);
        podcastListView_.setAdapter(adapter_);
        podcastListView_.setOnItemLongClickListener(this);
        podcastListView_.setOnItemClickListener(this);
        bundle_ = null;
        client_ = new OkHttpClient();
       
        //show back button ono toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
    
    static
    private List<PodcastInfo> defaultPodcastInfoList(Context context) {
        String[] allTitles = context.getResources().getStringArray(R.array.pref_podcastlist_keys);
        String[] allURLs = context.getResources().getStringArray(R.array.pref_podcastlist_urls);
        List<PodcastInfo> list = new ArrayList<PodcastInfo>();
        for (int i = 0; i < allTitles.length; i++) {
            String title = allTitles[i];
            try {
                URL url = new URL(allURLs[i]);
                //TODO: get config and fetch icon
                PodcastInfo info = new PodcastInfo(title, url, null, true);
                list.add(info);
            }
            catch (MalformedURLException e) {
                Log.d(TAG, "malformed", e);
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
        Log.d(TAG, "onStop.isChanged?; " + isChanged_);
        if (isChanged_) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
            boolean prevValue = pref.getBoolean("podcastlist2", true);
            pref.edit().putBoolean("podcastlist2", !prevValue).apply();
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
            String[] urlStrList = urlEdit_.getText().toString().split("\n");

            //check url
            //String[] urlStrList = urlStr.split("\n");
            int malformed = 0;
            List<URL> urlList = new ArrayList<URL>();
            for(String urlStr: urlStrList){
                if(urlStr.isEmpty()){
                    continue;
                }
                //TODO: use hash
                boolean duplicate = false;
                for(int i = 0; i < adapter_.getCount(); i++){
                    if(urlStr.equals(adapter_.getItem(i).url_.toString())){
                        //TODO: show toast
                        Log.d(TAG, "duplicate: " + urlStr);
                        duplicate = true;
                        continue;
                    }
                }
                if(duplicate){
                    continue;
                }
                try {
                    URL url = new URL(urlStr);
                    urlList.add(url);
                }
                catch (MalformedURLException e) {
                    malformed += 1;
                }
            }
            if(malformed > 0){
                showMessage(getString(R.string.error_malformed_url));
            }
            if(urlList.size() == 0){
                return;
            }
            //URL[] urlList = urlList;
            showDialog(CHECKING_DIALOG);
            task_ = new CheckTask();
            task_.execute(urlList.toArray(new URL[urlList.size()]));
        }
        else if (view.getId() == R.id.checkbox) {
            CheckBox checkbox = (CheckBox) view;
            onCheckboxClicked(checkbox);
        }
    }

    private void onCheckboxClicked(CheckBox checkbox) {
        Log.d(TAG, "checkbox is clicked: " + checkbox.isChecked());
        //umm...
        isChanged_ = true;
        PodcastInfo info = (PodcastInfo) checkbox.getTag();
        info.enabled_ = !info.enabled_;
        checkbox.setChecked(info.enabled_);
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
                .setTitle("xxx")
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
            PodcastInfo info = adapter_.getItem(pos);
            dialog.setTitle(info.title_);
            //TODO: disable up/down?
            break;
        default:
            break;
        }
    }
    
    //check that podcast XML is valid
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
                String iconURL = null;
                String title = null;
                Response response = null;
                try {
                    ///XXX
                    Request request = new Request.Builder()
                        .url(url)
                        .build();
                    response = client_.newCall(request).execute();
                    if(response.code() == 401){
                        //TODO: queue auth request and retry
                        Log.i(TAG, "auth required: "+url);
                        continue;
                    }
                    if(!response.isSuccessful()){
                        Log.i(TAG, "http error: "+response.message()+", "+url.toString());
                        continue;
                    }
                    is = response.body().byteStream();
                    is = new BOMInputStream(is, false);
                    
                    XmlPullParser parser = factory.newPullParser();
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
                                if(null != iconURL){
                                    iconURL = parser.getAttributeValue(null, "href");
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
                        parser.next();
                    }
                    if (numItems > 0 && null != title) {
                        //Log.d(TAG, "publish: " + title);
                        publishProgress(new PodcastInfo(title, url, iconURL, true));
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
                    if(null != response){
                        response.close();
                    }
                }
            }
            return result;
        }
        
        @Override
        protected void onProgressUpdate(PodcastInfo... values){
            PodcastInfo info = values[0];
            adapter_.add(info);
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
            String urlStr = info.url_.toString();
            if (null == title) {
                title = urlStr;
            }
            else {
                TextView urlView = (TextView) view.findViewById(R.id.podcast_url);
                urlView.setText(urlStr);
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
        for (int i = 0; i < adapter_.getCount(); i++) {
            PodcastInfo info = adapter_.getItem(i);
            JSONObject jsonValue = (new JSONObject())
                .accumulate("title", info.getTitle())
                .accumulate("url", info.getURL().toString())
                //.accumulate("icon_url", info.getIconURL())
                .accumulate("enabled", info.getEnabled());
            array.put(jsonValue);
        }
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
        //TODO: move to podcastinfo
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
        JSONTokener tokener = new JSONTokener(json);
        JSONArray jsonArray = (JSONArray) tokener.nextValue();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject value = jsonArray.getJSONObject(i);
            //TODO: check key existance
            String title  = value.getString("title");
            URL url = new URL(value.getString("url"));
            String iconURL = null;
            if(value.has("icon_url")){
                iconURL = value.getString("icon_url");
            }
            boolean enabled = value.getBoolean("enabled");
			PodcastInfo info = new PodcastInfo(title, url, iconURL, enabled);
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
        PodcastInfo info = adapter_.getItem(pos);
        Log.d(TAG, "DialogInterface: " + which + " pos: " + pos + " " + info.title_);
        switch(which) {
        case REMOVE_OPERATION:
            Log.d(TAG, "onClick REMOVE: " + pos + " " + info.title_);
            adapter_.remove(info);
            adapter_.notifyDataSetChanged();
            isChanged_ = true;
            break;
        case UP_OPERATION:
            Log.d(TAG, "dialog.onClick UP");
            if(pos == 0){
                break;
            }
            adapter_.remove(info);
            adapter_.insert(info, pos - 1);
            adapter_.notifyDataSetChanged();
            isChanged_ = true;
            break;
        case DOWN_OPERATION:
            Log.d(TAG, "dialog.onClick DOWN");
            if(pos == adapter_.getCount() - 1){
                break;
            }
            adapter_.remove(info);
            adapter_.insert(info, pos + 1);
            adapter_.notifyDataSetChanged();
            isChanged_ = true;
            break;
        default:
            break;
        }
        bundle_ = null;
    }

    @Override
    public void onItemClick(AdapterView<?> adapter, View parent, int pos, long id) {
        CheckBox checkbox = (CheckBox) parent.findViewById(R.id.checkbox);
        onCheckboxClicked(checkbox);
    }
}
