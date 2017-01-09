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
import android.graphics.Bitmap;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import io.realm.OrderedRealmCollection;
//import io.realm.RealmBaseAdapter;
import io.realm.RealmChangeListener;
import io.realm.RealmRecyclerViewAdapter;


import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
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

import com.mamewo.podplayer0.parser.PodcastInfo;
import com.mamewo.podplayer0.parser.Podcast;
import com.mamewo.podplayer0.parser.PodcastInfo.PodcastInfoBuilder;
import com.mamewo.podplayer0.parser.PodcastBuilder;
import com.mamewo.podplayer0.parser.Util;

import static com.mamewo.podplayer0.Const.*;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
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

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.LinearLayoutManager;

import com.mamewo.podplayer0.db.PodcastRealm;

public class PodcastListPreference
	extends AppCompatActivity
    implements OnClickListener,
    OnCancelListener
{
    static final
    private int WHITE = 0xFFFFFFFF;
    static final
    private int BLACK = 0xFF000000;
    static final
    private String CONFIG_FILENAME = "podcast.json";
    static final
    private int CHECKING_DIALOG = 0;
    static final
    private int EXPORT_DIALOG = 1;
    static final
    private int SHARE_PODCAST_DIALOG = 2;
    static final
    private int QRCODE_DIALOG = 3;
    static final
    private int SCAN_QRCODE_REQUEST_CODE = 3232;
    static final
    private int PODCAST_SITE_REQUEST_CODE = 3233;
        
    final static
    private String PODCAST_SITE_URL = "http://mamewo.ddo.jp/podcast/podcast.html";
    private Podcast selectedPodcastInfo_;
    private OkHttpClient client_;
    private Map<String, Option> optionMap_;
    private Dialog dialog_;
    private boolean isChanged_ = false;
    private Button addButton_;
    private EditText urlEdit_;
    private CheckTask task_;
    private PodcastAdapter adapter_;
    private RecyclerView podcastView_;
    private LinearLayoutManager layoutManager_;

    private int dialogID_;
    private RealmResults<PodcastRealm> podcastModel_;
    private RealmChangeListener<RealmResults<PodcastRealm>> changeListener_;
        
    static
    private Class<PodcastRealm.PodcastRealmBuilder> podcastBuilderClass_ = PodcastRealm.PodcastRealmBuilder.class;
    
    private class Option {
        public boolean expand_;
        public Option(){
            expand_ = false;
        }
    }

    static
    private PodcastRealm.PodcastRealmBuilder createPodcastBuilder(){
        try{
            return podcastBuilderClass_.newInstance();
        }
        catch(Exception e){
            Log.d(TAG, "builder craete error", e);
            return null;
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
        optionMap_ = new HashMap<String, Option>();
        podcastView_ = (RecyclerView) findViewById(R.id.podlist);
        client_ = new OkHttpClient();
        dialogID_ = -1;
        Realm realm = Realm.getDefaultInstance();
        Log.d(TAG, "json exists: "+configJSONExists());
        if(configJSONExists()){
            try{
                //loadJSONFileIntoDB();
                storeJSONSetting();
                storeDefaultPodcastList();
            }
            catch(JSONException e){
                Log.d(TAG, "load error(JSON)", e);
            }
            catch(IOException e){
                Log.d(TAG, "load error", e);
            }
        }
        podcastModel_ = realm.where(PodcastRealm.class).findAll();
        changeListener_ = new RealmChangeListener<RealmResults<PodcastRealm>>(){
                @Override
                public void onChange(RealmResults<PodcastRealm> results){
                    adapter_.notifyDataSetChanged();
                }
            };
        podcastModel_.addChangeListener(changeListener_);
        layoutManager_ = new LinearLayoutManager(this);
        podcastView_.setLayoutManager(layoutManager_);
        adapter_ = new PodcastAdapter(this, podcastModel_);

        podcastView_.setAdapter(adapter_);

        //restore after rotate screen
        if(null != savedInstanceState){
            String url = (String)savedInstanceState.getCharSequence("selected_url");
            if(null != url){
                for(Podcast info: podcastModel_){
                    if(url.equals(info.getURL().toString())){
                        selectedPodcastInfo_ = info;
                        break;
                    }
                }
            }
        }
        
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

    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        if(null != selectedPodcastInfo_){
            outState.putCharSequence("selected_url", selectedPodcastInfo_.getURL().toString());
        }
    }
    
    @Override
    public void onDestroy(){
        Log.d(TAG, "onDestroy: dialog" + dialogID_);
        //remove on rotating screen
        podcastModel_.removeChangeListener(changeListener_);
        removeDialog(dialogID_);
        super.onDestroy();
    }
    
    public void storeDefaultPodcastList() {
        String[] allTitles = getResources().getStringArray(R.array.pref_podcastlist_keys);
        String[] allURLs = getResources().getStringArray(R.array.pref_podcastlist_urls);
        List<Podcast> list = new ArrayList<Podcast>();

        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        for (int i = 0; i < allTitles.length; i++) {
            String title = allTitles[i];
            String url = allURLs[i];
            //TODO: get config and fetch icon
            RealmResults<PodcastRealm> existing = realm.where(PodcastRealm.class).equalTo("url", url).findAll();
            if(existing.size() > 0){
                continue;
            }
            PodcastRealm info = realm.createObject(PodcastRealm.class);
            info.setTitle(title);
            info.setURL(url);
            info.setEnabled(true);
        }
        realm.commitTransaction();
    }

    @Override
    public void onStart() {
        super.onStart();
        isChanged_ = false;
    }

    private void saveSettingJSON(){
        try {
            List<Podcast> lst = new ArrayList<Podcast>();
            for(int i = 0; i < adapter_.getItemCount(); i++){
                lst.add(adapter_.getItem(i));
            }
            saveSettingJSON(this, lst);
        }
        catch (JSONException e) {
            Log.d(TAG, "failed to save podcast list setting");
        }
        catch (IOException e) {
            Log.d(TAG, "failed to save podcast list setting");
        }
    }

    //tab separated text
    private StringBuffer exportSetting(){
        StringBuffer sb = new StringBuffer();
        for(int i = 0; i < adapter_.getItemCount(); i++){
            Podcast pinfo = adapter_.getItem(i);
            String title = pinfo.getTitle();
            String url = pinfo.getURL().toString();
            sb.append(title);
            sb.append("\t");
            sb.append(url);
        }
        return sb;
    }

 
    @Override
    public void onStop() {
        super.onStop();
        //saveSetting();
        Log.d(TAG, "onStop isChanged_: "+isChanged_);
        if (isChanged_) {
            //Ummm..: to call preference listener
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
        Intent i;
        switch(item.getItemId()) {
        case R.id.podcast_page_menu:
            // i =
            //     new Intent(Intent.ACTION_VIEW, Uri.parse(PODCAST_SITE_URL));
            // startActivity(new Intent(i));
            Intent intent = new Intent(this, PodcastSiteActivity.class);
            startActivityForResult(intent, PODCAST_SITE_REQUEST_CODE);
            handled = true;
            break;
        case R.id.export_podcast_menu:
            showDialog(EXPORT_DIALOG);
            break;
        case R.id.scan_qrcode_menu:
            //i = new Intent("com.google.zxing.client.android.SCAN");
            //startActivityForResult(i, SCAN_QRCODE_REQUEST_CODE);
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setBeepEnabled(false);
            integrator.initiateScan();
            break;
        default:
            break;
        }
        return handled;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        switch(requestCode){
        case SCAN_QRCODE_REQUEST_CODE:
            IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if(scanResult == null){
            //DISPLAY toast message?
                return;
            }
            String url = scanResult.getContents();
            String orig = urlEdit_.getText().toString();
            if(orig.length() > 0){
                orig = orig + "\n" + url;
            }
            else {
                orig = url;
            }
            urlEdit_.setText(orig);
            startCheckURLText();
            break;
        case PODCAST_SITE_REQUEST_CODE:
            Log.d(TAG, "data: "+data);
            if(null == data){
                break;
            }
            String urls = data.getStringExtra(PodcastSiteActivity.PODCAST_SITE_URLS);
            //String urlStr = String.join("\n", urls);
            urlEdit_.setText(urls);
            startCheckURLText();
            break;
        default:
            break;
        }
    }
    
    private class SimpleRequest
    {
        private URL url_;
        private String username_;
        private String password_;
        private PodcastRealm prevInfo_;
        
        public SimpleRequest(URL url, String username, String password, PodcastRealm prevInfo){
            url_ = url;
            username_ = username;
            password_ = password;
            prevInfo_ = prevInfo;
        }

        public SimpleRequest(URL url, String username, String password){
            this(url, username, password, null);
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

        public PodcastRealm getPrevInfo(){
            return prevInfo_;
        }
    }

    public void startCheckURLText(){
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
            for(int i = 0; i < adapter_.getItemCount(); i++){
                if(urlStr.equals(adapter_.getItem(i).getURL().toString())){
                    //TODO: show toast
                    Log.d(TAG, "duplicate: " + urlStr);
                    duplicate = true;
                    continue;
                }
            }
            if(duplicate){
                //TODO: mesage
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
    
    @Override
    public void onClick(View view) {
        if (view == addButton_) {
            startCheckURLText();
        }
        else if (view.getId() == R.id.checkbox) {
            CheckBox checkbox = (CheckBox) view;
            isChanged_ = true;
            PodcastRealm info = (PodcastRealm) checkbox.getTag();
            Realm realm = Realm.getDefaultInstance();
            realm.beginTransaction();
            info.setEnabled(!info.getEnabled());
            realm.commitTransaction();
            checkbox.setChecked(info.getEnabled());
        }
    }

    private Bitmap createQRCode(String content){
        //Map<EncodeHintType, Object> hints = null;
        BitMatrix result;
        Map<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>();
        hints.put(EncodeHintType.MARGIN, 2);

        WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        Point displaySize = new Point();
        int widthScreen = display.getWidth();
        int heightScreen = display.getHeight();
        int smallerDimension = (widthScreen < heightScreen) ? widthScreen : heightScreen;
        smallerDimension = smallerDimension*7/8;
        try{ 
            result = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, smallerDimension, smallerDimension, hints);
        }
        catch(Exception e){
            Log.d(TAG, "QR code error:", e);
            return null;
        }
                                
        int width = result.getWidth();
        int height = result.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }
    
    @Override
    protected Dialog onCreateDialog(int id, Bundle bundle) {
        Log.d(TAG, "onCreateDialog: " + id);
        Dialog dialog;
        AlertDialog.Builder builder = null;
        View view;
        
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
        case EXPORT_DIALOG:
            builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.exported_podcasts)
                .setPositiveButton("OK", null);
            view = LayoutInflater
                .from(this)
                .inflate(R.layout.selectable_textview, null, false);
            builder.setView(view);
            dialog = builder.create();
            break;
        case SHARE_PODCAST_DIALOG:
            builder = new AlertDialog.Builder(this)
                .setTitle(R.string.share_podcast)
                .setItems(SHARE_OPTIONS, new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick(DialogInterface dialog, int which){
                            Podcast info = selectedPodcastInfo_;
                            if(null == info){
                                // when screen is rotated
                                return;
                            }
                            if("Twitter".equals(SHARE_OPTIONS[which])){
                                //
                                Intent i = new Intent();
                                i.setAction(Intent.ACTION_SEND);
                                i.setType("text/plain");
                                i.setPackage("com.twitter.android");
                                i.putExtra(Intent.EXTRA_TEXT, info.getTitle()+" #podplayer "+info.getURL().toString());
                                startActivity(i);
                            }
                            else if("Mail".equals(SHARE_OPTIONS[which])){
                                Intent i = new Intent();
                                i.setAction(Intent.ACTION_SEND);
                                i.setType("message/rfc822");
                                //i.putExtra(Intent.EXTRA_SUBJECT");
                                //TODO: translate
                                i.putExtra(Intent.EXTRA_TEXT, info.getTitle()+"\n"+info.getURL().toString()+"\n"+"-----\npodplayer (Android app): https://play.google.com/store/apps/details?id=com.mamewo.podplayer0");
                                startActivity(i);
                            }
                            else if("QRCode".equals(SHARE_OPTIONS[which])){
                                Bundle b = new Bundle();
                                b.putCharSequence("title", selectedPodcastInfo_.getTitle());
                                b.putCharSequence("url", selectedPodcastInfo_.getURL().toString());
                                
                                showDialog(QRCODE_DIALOG, b);
                            }
                        }
                    })
                .setNegativeButton("Cancel", null); //TODO: put to strings
            dialog = builder.create();
            break;
        case QRCODE_DIALOG:
            view = LayoutInflater
                .from(this)
                .inflate(R.layout.qr_code, null, false);
            builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.qrcode_of_podcast)
                .setView(view)
                .setPositiveButton("OK", null);
            dialog = builder.create();
            break;
        default:
            dialog = null;
            break;
        }
        //dialog_ = dialog;
        return dialog;
    }
    
    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        AlertDialog.Builder builder = null;
        dialogID_ = id;
        
        switch(id){
        case CHECKING_DIALOG:
            dialog_ = dialog;
            break;
        case EXPORT_DIALOG:
            StringBuffer sb = exportSetting();
            TextView text = (TextView)dialog.findViewById(R.id.message);
            text.setText(sb.toString());
            //dialog_ = dialog;
            break;
        case SHARE_PODCAST_DIALOG:
            //list alert
            //dialog_ = dialog;
            break;
        case QRCODE_DIALOG:
            if(null == selectedPodcastInfo_){
                dialog.dismiss();
                return;
            }
            String url = selectedPodcastInfo_.getURL().toString();
            Bitmap bitmap = createQRCode(url);
            
            ImageView qrCode = (ImageView)dialog.findViewById(R.id.qr_code);
            qrCode.setImageBitmap(bitmap);
            TextView titleView = (TextView)dialog.findViewById(R.id.title);
            titleView.setText(selectedPodcastInfo_.getTitle());
            TextView urlView = (TextView)dialog.findViewById(R.id.url);
            urlView.setText(url);
            break;
        default:
            break;
        }
    }
    
    //check that podcast XML is valid
    public class CheckTask
        extends AsyncTask<SimpleRequest, String, Boolean>
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
            Realm realm = Realm.getDefaultInstance();
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

                //Log.d(TAG, "get URL: " + url);
                InputStream is = null;
                int numItems = 0;
                String iconURL = null;
                String title = null;
                Response response = null;
                boolean isRSS = false;                
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
                        //TODO: post showMessage(getString(R.string.network_error));
                        Log.d(TAG, "network error", e);
                        continue;
                    }
                    if(response.code() == 401){
                        //TODO: show messge
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                            Podcast prevInfo = req.getPrevInfo();
                            if(null != prevInfo){
                                //update password
                                prevInfo.setUsername(username);
                                prevInfo.setPassword(null);
                                //update ui
                            }
                            else {
                                //PodcastBuilder<PodcastRealm> b = createPodcastBuilder();
                                realm.beginTransaction();
                                PodcastRealm info = realm.createObject(PodcastRealm.class);
                                info.setTitle(url.toString());
                                info.setURL(url.toString());
                                info.setEnabled(true);
                                info.setUsername(username);
                                info.setPassword(password);
                                info.setStatus(Podcast.AUTH_REQUIRED_LOCKED);
                                realm.commitTransaction();
                                publishProgress(info.getTitle());
                            }
                            result = true;
                            Log.i(TAG, "auth required: "+url.toString());
                        }
                        else {
                            Log.i(TAG, "auth required but not supported for this android: "+url.toString());
                        }
                        //TODO: show supported or not
                        //TODO: post showMessage(getString(R.string.auth_required));
                        continue;
                    }
                    if(!response.isSuccessful()){
                        Log.i(TAG, "http error: "+response.message()+", "+url.toString());
                        //PodcastBuilder<PodcastRealm> b = createPodcastBuilder();
                        realm.beginTransaction();
                        PodcastRealm info = realm.createObject(PodcastRealm.class);
                        info.setTitle(url.toString());
                        info.setURL(url.toString());
                        info.setEnabled(true);
                        info.setUsername(username);
                        info.setPassword(password);
                        info.setStatus(Podcast.ERROR);
                        realm.commitTransaction();

                        publishProgress(info.getTitle());
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
                            if("rss".equalsIgnoreCase(currentName)){
                                isRSS = true;
                            }
                            else if("enclosure".equalsIgnoreCase(currentName)) {
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
                    //TODO: braek if title, iconURL and at least one episode(enclosure) are obtained
                }
                catch (IOException e) {
                    Log.i(TAG, "IOException", e);
                    //publishProgress(new PodcastInfo(title, url, iconURL, true, username, password, PodcastInfo.Status.ERROR));
                    //continue
                }
                catch (XmlPullParserException e) {
                    Log.i(TAG, "XmlPullParserException", e);
                    //TODO: showMessage();
                    //publishProgress(new PodcastInfo(title, url, iconURL, true, username, password, PodcastInfo.Status.ERROR));
                    //continue
                }
                finally {
                    if(null != is) {
                        try {
                            is.close();
                        }
                        catch (IOException e) {
                            Log.i(TAG, "input stream cannot be closed", e);
                        }
                    }
                    if(null != response){
                        response.close();
                    }
                }
                if(!isRSS){
                    //TODO: show message "this is not a podcast"
                    continue;
                }
                if (null != title) {
                    //Log.d(TAG, "publish: " + title);
                    int status;
                    if(null != username && null != password){
                        status = Podcast.AUTH_REQUIRED_UNLOCKED;
                        }
                    else {
                        status = Podcast.PUBLIC;
                    }
                    PodcastRealm prevInfo = req.getPrevInfo();
                    if(null != prevInfo){
                        realm.beginTransaction();

                        prevInfo.setTitle(title);
                        prevInfo.setIconURL(iconURL);
                        prevInfo.setUsername(username);
                        prevInfo.setPassword(password);
                        prevInfo.setStatus(status);
                        realm.commitTransaction();
                    }
                    else {
                        //PodcastBuilder<PodcastRealm> builder = createPodcastBuilder();
                        realm.beginTransaction();
                        PodcastRealm info = realm.createObject(PodcastRealm.class);
                        info.setTitle(title);
                        info.setURL(url.toString());
                        info.setIconURL(iconURL);
                        info.setEnabled(true);
                        info.setUsername(username);
                        info.setPassword(password);
                        info.setStatus(status);

                        realm.commitTransaction();                        
                        publishProgress(info.getTitle());
                    }
                    result = true;
                }
            }
            return result;
        }
        
        @Override
        protected void onProgressUpdate(String... titles){
            // for(int i = 0; i < values.length; i++){
            //     podcastModel_.add(values[i]);
            // }
            for(int i = 0; i < titles.length; i++){
                String title = titles[i];
                if(null != title && title.length() > 0){
                    String msg =
                        MessageFormat.format(getString(R.string.podcast_added), title);
                    showMessage(msg);
                }
            }
            //TODO: check status of PodcastInfo and change message
            urlEdit_.setText("");
            urlEdit_.clearFocus();
            isChanged_ = true;
        }
        
        @Override
        protected void onPostExecute(Boolean result) {
            task_ = null;
            dialog_.hide();
            dialog_ = null;
            if (!result.booleanValue()) {
                showMessage(getString(R.string.msg_add_podcast_failed));
            }
            else {
                isChanged_ = true;
                adapter_.notifyDataSetChanged();
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
    
    public class PodcastAdapter
        extends RealmRecyclerViewAdapter<PodcastRealm, Holder>
    {
        public PodcastAdapter(Context context, OrderedRealmCollection<PodcastRealm> list) {
            super(context, list, true);
        }
        
        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.podcast_select_item, parent, false);
            return new Holder(v);
        }
        
        @Override
        public void onBindViewHolder(Holder holder, int position) {
            PodcastRealm info = getData().get(position);
            holder.info_ = info;
            String iconURL = info.getIconURL();
            //Log.d(TAG, "getView: icon: " + iconURL);
            if(null != iconURL){
                Glide.with(getApplicationContext())
                    .load(iconURL)
                    .into(holder.icon_);
                holder.icon_.setVisibility(View.VISIBLE);
            }
            else{
                //set dummy image?
                holder.icon_.setVisibility(View.GONE);
            }
            
            //TextView urlView = (TextView) view.findViewById(R.id.podcast_url);
            //add check
            String title = info.getTitle();
            String urlStr = info.getURL().toString();

            if (null == title) {
                title = urlStr;
            }
            holder.title_.setText(title);

            holder.checkbox_.setOnClickListener(PodcastListPreference.this);
            holder.checkbox_.setTag(info);
            holder.checkbox_.setChecked(info.getEnabled());

            holder.detailButton_.setTag(info);
            holder.detailButton_.setOnClickListener(new DetailButtonListener());
            
            Option opt = optionMap_.get(urlStr);
            
            if(null != opt && opt.expand_){
                holder.podcastURL_.setText(urlStr);
                holder.podcastURL_.setVisibility(View.VISIBLE);
                
                holder.deleteButton_.setTag(info);
                holder.deleteButton_.setOnClickListener(new RemoveButtonListener());
                holder.moveUpButton_.setTag(info);
                holder.moveUpButton_.setOnClickListener(new MoveupButtonListener());
                holder.moveDownButton_.setTag(info);
                holder.moveDownButton_.setOnClickListener(new MovedownButtonListener());
                holder.shareButton_.setTag(info);
                holder.shareButton_.setOnClickListener(new ShareButtonListener());
                
                holder.loginButton_.setTag(info);
                holder.loginButton_.setOnClickListener(new EnterPasswordButtonListener());

                int imageId = R.drawable.ic_lock_outline_white_24dp;
                Context context = PodcastListPreference.this;
                int imageColor = ContextCompat.getColor(context, R.color.green);
                Log.d(TAG, "status: " + info.getTitle() + " " + info.getStatus());
                switch(info.getStatus()){
                case Podcast.UNKNOWN:
                    //TODO: change icon
                    holder.authView_.setVisibility(View.GONE);
                    imageId = R.drawable.ic_error_outline_white_24dp;
                    imageColor = ContextCompat.getColor(context, R.color.white);
                    break;
                case Podcast.PUBLIC:
                    holder.authView_.setVisibility(View.GONE);
                    imageId = R.drawable.ic_public_white_24dp;
                    imageColor = ContextCompat.getColor(context, R.color.green);
                    break;
                case Podcast.AUTH_REQUIRED_LOCKED:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH){
                        holder.authView_.setVisibility(View.VISIBLE);
                        if(null != info.getUsername()){
                            holder.usernameEdit_.setText(info.getUsername());
                        }
                        else {
                            holder.usernameEdit_.setText("");
                        }
                        if(null != info.getPassword()){
                            holder.passwordEdit_.setText(info.getPassword());
                        }
                        else {
                            holder.passwordEdit_.setText("");
                        }
                    }
                    else {
                        holder.authView_.setVisibility(View.GONE);
                    }
                    imageId = R.drawable.ic_lock_outline_white_24dp;
                    imageColor = ContextCompat.getColor(context, R.color.yellow);
                    break;
                case Podcast.AUTH_REQUIRED_UNLOCKED:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH){
                        holder.authView_.setVisibility(View.VISIBLE);
                        if(null != info.getUsername()){
                            holder.usernameEdit_.setText(info.getUsername());
                        }
                        else {
                            holder.usernameEdit_.setText("");
                        }
                        if(null != info.getPassword()){
                            holder.passwordEdit_.setText(info.getPassword());
                        }
                        else {
                            holder.passwordEdit_.setText("");
                        }
                    }
                    else {
                        holder.authView_.setVisibility(View.GONE);
                    }
                    imageId = R.drawable.ic_lock_open_white_24dp;
                    imageColor = ContextCompat.getColor(context, R.color.green);
                    break;
                case Podcast.ERROR:
                    holder.authView_.setVisibility(View.GONE);
                    imageId = R.drawable.ic_error_outline_white_24dp;
                    imageColor = ContextCompat.getColor(context, R.color.pink);
                    break;
                default:
                    break;
                }
                holder.statusButton_.setImageResource(imageId);
                holder.statusButton_.getDrawable().setColorFilter(imageColor, PorterDuff.Mode.SRC_IN);
                holder.detailButton_.setImageResource(R.drawable.ic_expand_less_white_24dp);
                holder.authView_.setVisibility(View.VISIBLE);
                holder.detailView_.setVisibility(View.VISIBLE);
            }
            else {
                holder.podcastURL_.setVisibility(View.GONE);
                holder.authView_.setVisibility(View.GONE);
                //TODO: remove listener?
                holder.detailButton_.setImageResource(R.drawable.ic_expand_more_white_24dp);
                holder.detailView_.setVisibility(View.GONE);
            }
        }
    }

    static
    public void saveSettingJSON(Context context, List<Podcast> lst) throws
        JSONException, IOException
    {
        JSONArray array = new JSONArray();
        //for (int i = 0; i < adapter_.getCount(); i++) {
        //PodcastInfo info = adapter_.getItem(i);
        for(Podcast info: lst){
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
        //Log.d(TAG, "saveSetting JSON: " + json);
        FileOutputStream fos = context.getApplicationContext().openFileOutput(CONFIG_FILENAME, MODE_PRIVATE);
        try{
            fos.write(json.getBytes());
        }
        finally {
            fos.close();
        }
    }
    
    public boolean configJSONExists(){
        File configFile = getFileStreamPath(CONFIG_FILENAME);
        return configFile.exists();
    }
   
    //List<PodcastRealm>
    // public RealmResults<PodcastRealm> loadPodcastRealm(Context context){
    //     Realm realm = Realm.getDefaultInstance();
    //     RealmResults<PodcastRealm> result = realm.where(PodcastRealm.class).findAll();
    //     return result;
    // }
  
    private void storeJSONSetting()
            throws IOException, JSONException
    {
        Log.d(TAG, "loadJSONFileIntoDB");
        Realm realm = Realm.getDefaultInstance();
        //TODO: move to podcastinfo
        FileInputStream fis = getApplicationContext().openFileInput(CONFIG_FILENAME);
        StringBuffer sb = new StringBuffer();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            String line;
            while (null != (line = reader.readLine())) {
                //Log.d(TAG, "loadSettingFromJSONFile: JSON "+line);
                sb.append(line);
            }
        }
        finally {
            fis.close();
        }
        String json = sb.toString();
        JSONTokener tokener = new JSONTokener(json);
        JSONArray jsonArray = (JSONArray) tokener.nextValue();

        realm.beginTransaction();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject value = jsonArray.getJSONObject(i);
            //TODO: check key existance
            String title  = null;
            if(value.has("title")){
                title = value.getString("title");
            }
            URL url = new URL(value.getString("url"));
            String iconURL = null;
            int status = Podcast.UNKNOWN;
            if(value.has("icon_url")){
                iconURL = value.getString("icon_url");
            }
            if(value.has("status")){
                try{
                    String st = value.getString("status");
                    if("UNKNOWN".equals(st)){
                        status = Podcast.UNKNOWN;
                    }
                    else if("PUBLIC".equals(st)){
                        status = Podcast.PUBLIC;
                    }
                    else if("AUTH_REQUIRED_LOCKED".equals(st)){
                        status = Podcast.AUTH_REQUIRED_LOCKED;
                    }
                    else if("ERROR".equals(st)){
                        status = Podcast.ERROR;
                    }
                }
                catch(Exception e){
                    Log.d(TAG, "read status failed: ", e);
                }
            }
            String username = null;
            if(value.has("username")){
                username = value.getString("username");
            }
            String password = null;
            if(value.has("password")){
                password  = value.getString("password");
            }
            boolean enabled = value.getBoolean("enabled");
            RealmResults<PodcastRealm> existing = realm.where(PodcastRealm.class).equalTo("url", url.toString()).findAll();
            if(existing.size() > 0){
                continue;
            }
			//PodcastInfo info = new PodcastInfo(title, url, iconURL, enabled, username, password, status);
            PodcastRealm info = realm.createObject(PodcastRealm.class);
            info.setTitle(title);
            info.setURL(url.toString());
            info.setIconURL(iconURL);
            info.setEnabled(enabled);
            info.setUsername(username);
            info.setPassword(password);
            info.setStatus(status);
        }
        realm.commitTransaction();
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

    private class RemoveButtonListener
        implements View.OnClickListener
    {
        @Override
        public void onClick(View v){
            Podcast info = (Podcast)v.getTag();
            //adapter_.remove(info);
            isChanged_ = true;
            adapter_.notifyDataSetChanged();
        }
    }

    private class MoveupButtonListener
        implements View.OnClickListener
    {
        @Override
        public void onClick(View v){
            Podcast info = (Podcast)v.getTag();
            //int pos = adapter_.getPosition(info);
            // adapter_.remove(info);
            // if(pos > 0){
            //     adapter_.insert(info, pos-1);
            // }
            // else {
            //     adapter_.add(info);
            // }
            isChanged_ = true;
            adapter_.notifyDataSetChanged();
        }
    }

    private class MovedownButtonListener
        implements View.OnClickListener
    {
        @Override
        public void onClick(View v){
            Podcast info = (Podcast)v.getTag();
            //int pos = adapter_.getPosition(info);
            //int len = adapter_.getCount();
            // adapter_.remove(info);
            // if(pos < len-1){
            //     adapter_.insert(info, pos+1);
            // }
            // else {
            //     adapter_.insert(info, 0);
            // }
            isChanged_ = true;
            adapter_.notifyDataSetChanged();
        }
    }
    
    private class ShareButtonListener
        implements View.OnClickListener
    {
        @Override
        public void onClick(View v){
            selectedPodcastInfo_ = (Podcast)v.getTag();

            Bundle b = new Bundle();
            b.putCharSequence("title", selectedPodcastInfo_.getTitle());
            b.putCharSequence("url", selectedPodcastInfo_.getURL().toString());
            //display dialog
            showDialog(SHARE_PODCAST_DIALOG, b);
        }
    }
    
    private class DetailButtonListener
        implements View.OnClickListener
    {
        @Override
        public void onClick(View v){
            Podcast info = (Podcast)v.getTag();
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
            PodcastRealm info = (PodcastRealm)v.getTag();            
            ViewParent parent = v.getParent();
            if(null == parent){
                //Log.d(TAG, "parent is null");
                return;
            }
            if(parent instanceof LinearLayout){
                LinearLayout layout = (LinearLayout)parent;
                EditText usernameView = (EditText)layout.findViewById(R.id.username);
                EditText passwordView = (EditText)layout.findViewById(R.id.password);
                info.setUsername(usernameView.getText().toString());
                info.setPassword(passwordView.getText().toString());
                //TODO: start check
                //TODO check task
                showDialog(CHECKING_DIALOG);
                task_ = new CheckTask();
                task_.execute(new SimpleRequest(info.getParsedURL(), info.getUsername(), info.getPassword(), info));
            }
        }
    }

    private class Holder
        extends RecyclerView.ViewHolder
        implements View.OnLongClickListener
    {
        public TextView title_;
        public ImageView icon_;
        public CheckBox checkbox_;
        public ImageButton detailButton_;
        public TextView podcastURL_;
        public ImageButton moveDownButton_;
        public ImageButton moveUpButton_;
        public ImageButton shareButton_;
        public ImageButton deleteButton_;
        public ImageButton statusButton_;
        public EditText usernameEdit_;
        public EditText passwordEdit_;
        public Button loginButton_;
        
        public LinearLayout authView_;
        public LinearLayout detailView_;
        public PodcastRealm info_;
        
        public Holder(View view){
            super(view);
            info_ = null;
            icon_ = (ImageView)view.findViewById(R.id.podcast_icon);
            checkbox_ = (CheckBox)view.findViewById(R.id.checkbox);
            title_ = (TextView)view.findViewById(R.id.podcast_title_label);
            detailButton_ = (ImageButton)view.findViewById(R.id.detail_button);
            podcastURL_ = (TextView)view.findViewById(R.id.podcast_url);
            moveDownButton_ = (ImageButton)view.findViewById(R.id.move_down);
            moveUpButton_ = (ImageButton)view.findViewById(R.id.move_up);
            shareButton_ = (ImageButton)view.findViewById(R.id.podcast_share);
            deleteButton_ = (ImageButton)view.findViewById(R.id.delete);
            statusButton_ = (ImageButton)view.findViewById(R.id.status_icon);
            usernameEdit_ = (EditText)view.findViewById(R.id.username);
            passwordEdit_ = (EditText)view.findViewById(R.id.password);
            loginButton_ = (Button)view.findViewById(R.id.auth_info);
            authView_  = (LinearLayout)view.findViewById(R.id.podcast_auth_view);
            detailView_ = (LinearLayout)view.findViewById(R.id.podcast_detail_view);
            view.setOnLongClickListener(this);
        }

        //enable selected item only
        @Override
        public boolean onLongClick(View v) {
            Realm realm = Realm.getDefaultInstance();
            realm.beginTransaction();
            for(PodcastRealm info: podcastModel_){
                info.setEnabled(false);
            }
            info_.setEnabled(true);
            realm.commitTransaction();
            return true;
        }
    }
}
