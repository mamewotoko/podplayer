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
import java.util.Map;
import java.util.HashMap;
import android.graphics.PorterDuff;
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
import com.mamewo.lib.podcast_parser.Util;
import com.mamewo.lib.podcast_parser.PodcastInfo.Status.*;

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
import android.view.ViewParent;

import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;

import okhttp3.OkHttpClient;
import com.bumptech.glide.Glide;

import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

public class PodcastListPreference
	extends AppCompatActivity
    implements OnClickListener,
    OnItemClickListener,
    OnCancelListener
{
    private Button addButton_;
    private EditText urlEdit_;
    private CheckTask task_;
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
    final static
    private String PODCAST_SITE_URL = "http://mamewo.ddo.jp/podcast/podcast.html";
    private Dialog dialog_;
    private OkHttpClient client_;
    private Map<String, Option> optionMap_;

    private class Option {
        public boolean expand_;
        public Option(){
            expand_ = false;
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.podlist_editor);
        setTitle(R.string.app_podcastlist_title);
        addButton_ = (Button) findViewById(R.id.add_podcast_button);
        addButton_.setOnClickListener(this);
        urlEdit_ = (EditText) findViewById(R.id.url_edit);
        dialog_ = null;
        optionMap_ = new HashMap<String, Option>();
        podcastListView_ = (ListView) findViewById(R.id.podlist);
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
        List<PodcastInfo> list = loadSetting(this);
        adapter_ = new PodcastInfoAdapter(this, list);
        podcastListView_.setAdapter(adapter_);
    }
    
    @Override
    public void onStop() {
        super.onStop();
        try {
            List<PodcastInfo> lst = new ArrayList<PodcastInfo>();
            for(int i = 0; i < adapter_.getCount(); i++){
                lst.add(adapter_.getItem(i));
            }
            Log.d(TAG, "onStop: saveSetting");
            saveSetting(this, lst);
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
                    if(urlStr.equals(adapter_.getItem(i).getURL().toString())){
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
            showDialog(CHECKING_DIALOG);
            task_ = new CheckTask();
            SimpleRequest[] reqlist = new SimpleRequest[urlList.size()];
            for(int i = 0; i < urlList.size(); i++){
                reqlist[i] = new SimpleRequest(urlList.get(i), null, null);
            }
            task_.execute(reqlist);
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
        info.setEnabled(!info.getEnabled());
        checkbox.setChecked(info.getEnabled());
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
            break;
        default:
            break;
        }
        dialog_ = dialog;
        return dialog;
    }
    
    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        bundle_ = args;
        switch(id){
        case CHECKING_DIALOG:
            dialog_ = dialog;
            break;
        default:
            break;
        }
    }
    
    //check that podcast XML is valid
    public class CheckTask
    //extends AsyncTask<URL, PodcastInfo, Boolean>
        extends AsyncTask<SimpleRequest, PodcastInfo, Boolean>
    {
        private boolean addItem_;
        
        public CheckTask(boolean addItem){
            addItem_ = addItem;
        }

        public CheckTask(){
            this(true);
        }

        @Override
        protected Boolean doInBackground(SimpleRequest... reqlist) {
            XmlPullParserFactory factory;
            try {
                factory = XmlPullParserFactory.newInstance();
            }
            catch (XmlPullParserException e1) {
                Log.i(TAG, "cannot get xml parser", e1);
                return false;
            }
            boolean result = false;
            for(int i = 0; i < reqlist.length; i++) {
                if(isCancelled()){
                    break;
                }
                SimpleRequest req = reqlist[i];
                URL url = req.getURL();
                String username = req.getUsername();
                String password = req.getPassword();

                Log.d(TAG, "get URL: " + url);
                InputStream is = null;
                int numItems = 0;
                String iconURL = null;
                String title = null;
                Response response = null;
                try {
                    ///XXX
                    Request.Builder builder = new Request.Builder();
                    builder.url(url);
                    if(null != username && null != password){
                        builder.addHeader("Authorization", Util.makeHTTPAuthorizationHeader(username, password));
                    }
                    Request request = builder.build();
                    //TODO: cancel?
                    try{
                        response = client_.newCall(request).execute();
                    }
                    catch(IOException e){
                        //showMessage(getString(R.string.network_error));
                        Log.d(TAG, "network error", e);
                        continue;
                    }
                    if(response.code() == 401){
                        //TODO: show messge
                        publishProgress(new PodcastInfo(title, url, iconURL, true, username, password, PodcastInfo.Status.AUTH_REQUIRED_LOCKED));
                        Log.i(TAG, "auth required: "+url);
                        //showMessage(getString(R.string.auth_required));
                        continue;
                    }
                    if(!response.isSuccessful()){
                        Log.i(TAG, "http error: "+response.message()+", "+url.toString());
                        publishProgress(new PodcastInfo(title, url, iconURL, true, username, password, PodcastInfo.Status.ERROR));
                        continue;
                    }
                    //TODO: check content-type
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
                        PodcastInfo.Status status;
                        if(null != username && null != password){
                            status = PodcastInfo.Status.AUTH_REQUIRED_UNLOCKED;
                        }
                        else {
                            status = PodcastInfo.Status.PUBLIC;
                        }
                        publishProgress(new PodcastInfo(title, url, iconURL, true, username, password, status));
                        result = true;
                    }
                }
                catch (IOException e) {
                    Log.i(TAG, "IOException", e);
                    publishProgress(new PodcastInfo(title, url, iconURL, true, username, password, PodcastInfo.Status.ERROR));
                    //continue
                }
                catch (XmlPullParserException e) {
                    Log.i(TAG, "XmlPullParserException", e);
                    publishProgress(new PodcastInfo(title, url, iconURL, true, username, password, PodcastInfo.Status.ERROR));
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
                MessageFormat.format(getString(R.string.podcast_added), info.getTitle());
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
            String iconURL = info.getIconURL();
            Log.d(TAG, "getView: icon: " + iconURL);
            ImageView icon = (ImageView)view.findViewById(R.id.podcast_icon);
            if(null != iconURL){
                Glide.with(getApplicationContext())
                    .load(iconURL)
                    .into(icon);
                icon.setVisibility(View.VISIBLE);
            }
            else{
                //set dummy image?
                icon.setVisibility(View.GONE);
            }
            CheckBox check = (CheckBox) view.findViewById(R.id.checkbox);
            check.setOnClickListener(PodcastListPreference.this);
            check.setTag(info);
            
            TextView urlView = (TextView) view.findViewById(R.id.podcast_url);
            //add check
            String title = info.getTitle();
            String urlStr = info.getURL().toString();
            TextView label = (TextView) view.findViewById(R.id.podcast_title_label);
            if (null == title) {
                title = urlStr;
            }
            label.setText(title);
            
            check.setChecked(info.getEnabled());

            ImageButton detailButton = (ImageButton)view.findViewById(R.id.detail_button);
            detailButton.setTag(info);
            detailButton.setOnClickListener(new DetailButtonListener());
            
            Option opt = optionMap_.get(urlStr);
            View v = view.findViewById(R.id.podcast_detail_view);
            View authView = view.findViewById(R.id.podcast_auth_view);
            
            if(null != opt && opt.expand_){
                urlView.setText(urlStr);
                urlView.setVisibility(View.VISIBLE);
                
                ImageButton upButton = (ImageButton) view.findViewById(R.id.move_up);
                ImageButton downButton = (ImageButton) view.findViewById(R.id.move_down);
                ImageButton deleteButton = (ImageButton) view.findViewById(R.id.delete);
                deleteButton.setTag(info);
                deleteButton.setOnClickListener(new RemoveButtonListener());
                upButton.setTag(info);
                upButton.setOnClickListener(new MoveupButtonListener());
                downButton.setTag(info);
                downButton.setOnClickListener(new MovedownButtonListener());

                Button enterPasswordButton = (Button)view.findViewById(R.id.auth_info);
                enterPasswordButton.setTag(info);
                enterPasswordButton.setOnClickListener(new EnterPasswordButtonListener());

                int imageId = R.drawable.ic_lock_outline_white_24dp;
                Context context = PodcastListPreference.this;
                int imageColor = ContextCompat.getColor(context, R.color.green);
                ImageButton statusIcon = (ImageButton)view.findViewById(R.id.status_icon);
                Log.d(TAG, "status: " + info.getTitle() + " " + info.getStatus());
                switch(info.getStatus()){
                case UNKNOWN:
                    //TODO: change icon
                    authView.setVisibility(View.GONE);
                    imageId = R.drawable.ic_error_outline_white_24dp;
                    imageColor = ContextCompat.getColor(context, R.color.white);
                    break;
                case PUBLIC:
                    authView.setVisibility(View.GONE);
                    imageId = R.drawable.ic_public_white_24dp;
                    imageColor = ContextCompat.getColor(context, R.color.green);
                    break;
                case AUTH_REQUIRED_LOCKED:
                    authView.setVisibility(View.VISIBLE);
                    imageId = R.drawable.ic_lock_outline_white_24dp;
                    imageColor = ContextCompat.getColor(context, R.color.yellow);
                    break;
                case AUTH_REQUIRED_UNLOCKED:
                    authView.setVisibility(View.VISIBLE);
                    imageId = R.drawable.ic_lock_open_white_24dp;
                    imageColor = ContextCompat.getColor(context, R.color.green);
                    break;
                case ERROR:
                    authView.setVisibility(View.GONE);
                    imageId = R.drawable.ic_error_outline_white_24dp;
                    imageColor = ContextCompat.getColor(context, R.color.pink);
                    break;
                default:
                    break;
                }
                statusIcon.setImageResource(imageId);
                statusIcon.getDrawable().setColorFilter(imageColor, PorterDuff.Mode.SRC_IN);
                detailButton.setImageResource(R.drawable.ic_expand_less_white_24dp);
                v.setVisibility(View.VISIBLE);
            }
            else {
                urlView.setVisibility(View.GONE);
                authView.setVisibility(View.GONE);
                //TODO: remove listener?
                detailButton.setImageResource(R.drawable.ic_expand_more_white_24dp);
                v.setVisibility(View.GONE);
            }
            return view;
        }
    }

    static
    public void saveSetting(Context context, List<PodcastInfo> lst) throws
        JSONException, IOException
    {
        JSONArray array = new JSONArray();
        //for (int i = 0; i < adapter_.getCount(); i++) {
        //PodcastInfo info = adapter_.getItem(i);
        for(PodcastInfo info: lst){
            JSONObject jsonValue = (new JSONObject())
                .accumulate("title", info.getTitle())
                .accumulate("url", info.getURL().toString())
                .accumulate("icon_url", info.getIconURL())
                .accumulate("enabled", info.getEnabled())
                .accumulate("username", info.getUsername())
                .accumulate("password", info.getPassword())
                .accumulate("status", info.getStatus());
            array.put(jsonValue);
        }
        String json = array.toString();
        Log.d(TAG, "saveSetting JSON: " + json);
        FileOutputStream fos = context.getApplicationContext().openFileOutput(CONFIG_FILENAME, MODE_PRIVATE);
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
        FileInputStream fis = context.getApplicationContext().openFileInput(CONFIG_FILENAME);
        StringBuffer sb = new StringBuffer();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            String line;
            while (null != (line = reader.readLine())) {
                Log.d(TAG, "loadSettingFromJSONFile: JSON "+line);
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
            String title  = "xxx";
            if(value.has("title")){
                title = value.getString("title");
            }
            URL url = new URL(value.getString("url"));
            String iconURL = null;
            PodcastInfo.Status status = PodcastInfo.Status.UNKNOWN;
            if(value.has("icon_url")){
                iconURL = value.getString("icon_url");
            }
            if(value.has("status")){
                try{
                    status = PodcastInfo.Status.valueOf(value.getString("status"));
                }
                catch(Exception e){
                    Log.d(TAG, "read status failed: ", e);
                }
            }
            String username  = null;
            if(value.has("username")){
                username = value.getString("username");
            }
            String password = null;
            if(value.has("password")){
                password  = value.getString("password");
            }
            boolean enabled = value.getBoolean("enabled");
			PodcastInfo info = new PodcastInfo(title, url, iconURL, enabled, username, password, status);
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
    public void onItemClick(AdapterView<?> adapter, View parent, int pos, long id) {
        CheckBox checkbox = (CheckBox) parent.findViewById(R.id.checkbox);
        onCheckboxClicked(checkbox);
    }

    private class RemoveButtonListener
        implements View.OnClickListener
    {
        @Override
        public void onClick(View v){
            PodcastInfo info = (PodcastInfo)v.getTag();
            adapter_.remove(info);
            adapter_.notifyDataSetChanged();
        }
    }

    private class MoveupButtonListener
        implements View.OnClickListener
    {
        @Override
        public void onClick(View v){
            PodcastInfo info = (PodcastInfo)v.getTag();
            int pos = adapter_.getPosition(info);
            adapter_.remove(info);
            if(pos > 0){
                adapter_.insert(info, pos-1);
            }
            else {
                adapter_.add(info);
            }
            adapter_.notifyDataSetChanged();
        }
    }

    private class MovedownButtonListener
        implements View.OnClickListener
    {
        @Override
        public void onClick(View v){
            PodcastInfo info = (PodcastInfo)v.getTag();
            int pos = adapter_.getPosition(info);
            int len = adapter_.getCount();
            adapter_.remove(info);
            if(pos < len-1){
                adapter_.insert(info, pos+1);
            }
            else {
                adapter_.insert(info, 0);
            }
            adapter_.notifyDataSetChanged();
        }
    }

    private class DetailButtonListener
        implements View.OnClickListener
    {
        @Override
        public void onClick(View v){
            PodcastInfo info = (PodcastInfo)v.getTag();
            Option opt = optionMap_.get(info.getURL().toString());
            if(opt == null){
                opt = new Option();
                optionMap_.put(info.getURL().toString(), opt);
            }
            opt.expand_ = !opt.expand_;
            adapter_.notifyDataSetChanged();
        }
    }

    private class EnterPasswordButtonListener
        implements View.OnClickListener
    {
        @Override
        public void onClick(View v){
            PodcastInfo info = (PodcastInfo)v.getTag();
            ViewParent parent = v.getParent();
            if(null == parent){
                Log.d(TAG, "parent is null");
                return;
            }
            if(parent instanceof LinearLayout){
                LinearLayout layout = (LinearLayout)parent;
                EditText usernameView = (EditText)layout.findViewById(R.id.username);
                EditText passwordView = (EditText)layout.findViewById(R.id.password);
                info.setUsername(usernameView.getText().toString());
                info.setPassword(passwordView.getText().toString());
                Log.d(TAG, "url, user, pass: " + info.getURL() + " " + info.getUsername() + " " + info.getPassword());
                //TODO: start check
                //TODO check task
                showDialog(CHECKING_DIALOG);
                task_ = new CheckTask();
                task_.execute(new SimpleRequest(info.getURL(), info.getUsername(), info.getPassword()));
            }
        }
    }

    private class SimpleRequest
    {
        private URL url_;
        private String username_;
        private String password_;
        
        public SimpleRequest(URL url, String username, String password){
            url_ = url;
            username_ = username;
            password_ = password;
        }

        public URL getURL(){
            return url_;
        }

        public String getUsername(){
            return username_;
        }

        public String getPassword(){
            return password_;
        }
    }
}
