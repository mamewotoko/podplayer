package com.mamewo.podplayer0;

import java.util.List;
import java.util.ArrayList;

import android.util.Log;
import android.webkit.WebView;
import android.webkit.JavascriptInterface;
import android.os.Bundle;
import android.view.View;
import android.content.Intent;
import android.app.Activity;
import android.widget.EditText;
import android.webkit.WebChromeClient;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import static com.mamewo.podplayer0.Const.*;

public class PodcastSiteActivity
    extends AppCompatActivity
{
    static final
    public String PODCAST_SITE_URLS = "PODCAST_SITE_URLS";
    static final        
    private String PODSITE_URL = "http://mamewo.ddo.jp/podcast/podcast.html";
    private WebView webView_;
    private String urls_;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.podcast_site);

        webView_ = (WebView)findViewById(R.id.web_view);
        urls_ = null;
        webView_.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView_.addJavascriptInterface(new JSInterface(), "WEBVIEW");
        webView_.setWebChromeClient(new WebChromeClient() {
                public void onConsoleMessage(String message, int lineNumber, String sourceID) {
                    Log.d(TAG, message + " -- From line "
                          + lineNumber + " of "
                          + sourceID);
                }
            });
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(null != urls_ && !urls_.isEmpty()){
                        Intent data = new Intent();
                        data.putExtra(PODCAST_SITE_URLS, urls_);
                        if(getParent() == null){
                            setResult(Activity.RESULT_OK, data);
                        }
                        else {
                            getParent().setResult(Activity.RESULT_OK, data);
                        }
                    }
                    finish();
                }
            });
    }

    @Override
    public void onStart() {
        super.onStart();
        webView_.loadUrl(PODSITE_URL);
    }

    private class JSInterface {
        @JavascriptInterface
        public void setURLs(String urls){
            Log.d(TAG, "urls: "+urls);
            urls_ = urls;
        }
    }
}
