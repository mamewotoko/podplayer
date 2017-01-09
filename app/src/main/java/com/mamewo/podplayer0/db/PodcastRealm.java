package com.mamewo.podplayer0.db;

import java.io.Serializable;
import java.net.URL;
import java.net.MalformedURLException;
import android.util.Log;
import com.mamewo.lib.podcast_parser.Podcast;
import com.mamewo.lib.podcast_parser.PodcastBuilder;

import io.realm.RealmObject;
import io.realm.RealmModel;
import io.realm.annotations.RealmClass;

import static com.mamewo.podplayer0.Const.*;

@RealmClass
public class PodcastRealm
    implements RealmModel,
               Podcast,
               Serializable
{
    private static final long serialVersionUID = 76131894671950703L;
    private String title_;
    private String url_;
    private boolean enabled_;
    private String iconURL_;

    //TODO: hold in secure area?
    private String username_;
    private String password_;
    private int status_;

    // @Ignore
    // private URL parsedURL_;
    
    public PodcastRealm(){
        title_ = null;
        url_ = null;
        enabled_ = true;
        iconURL_ = null;
        username_ = null;
        password_ = null;
        status_ = Podcast.UNKNOWN;
    }
    
    // public PodcastInfo(String title, URL url, String iconURL, boolean enabled, String username, String password, Status status) {
    //     title_ = title;
    //     if(null == title_){
    //         title_ = "";
    //     }
    //     url_ = url;
    //     iconURL_ = iconURL;
    //     enabled_ = enabled;

    //     username_ = username;
    //     password_ = password;
    //     lastStatus_ = status;
    // }

    // public PodcastInfo(String title, URL url, String iconURL, boolean enabled) {
    //     this(title, url, iconURL, enabled, null, null, Status.UNKNOWN);
    // }
    
    public String getTitle(){
        return title_;
    }

    public void setTitle(String title){
        title_ = title;
    }
    
    public void setURL(String url) {
        url_ = url;
    }
    
    public String getURL(){
        return url_.toString();
    }

    //XXX
    public URL getParsedURL(){
        try{
            return new URL(url_);
        }
        catch(MalformedURLException e){
            Log.d(TAG, "parse url", e);
            return null;
        }
    }
    
    public URL getURLWithAuthInfo(){
        try{
            if(null != username_ && null != password_){
                return new URL(addUserInfo(url_));
            }
            return new URL(url_);
        }
        catch (MalformedURLException e){
            return null;
        }
    }

    public boolean getEnabled(){
        return enabled_;
    }

    public void setEnabled(boolean enabled){
        enabled_ = enabled;
    }
    
    public String getIconURL(){
        return iconURL_;
    }

    public String getIconURLWithAuthInfo(){
        return addUserInfo(iconURL_);
    }

    public void setIconURL(String url){
        iconURL_ = url;
    }

    public String getUsername(){
        return username_;
    }

    public void setUsername(String username){
        username_ = username;
    }

    public String getPassword(){
        return password_;
    }

    public void setPassword(String password){
        password_ = password;
    }

    public String addUserInfo(String url){
        Log.d(TAG, "addUserInfo: " + url + " " + username_ + " " + password_);
        if(null == url || null == username_ || null == password_){
            return url;
        }
        int pos = url.indexOf("://");
        return url.substring(0, pos) + "://" + username_ +":"+password_+"@"+url.substring(pos+3);
    }

    public int getStatus(){
        return status_;
    }

    public void setStatus(int status){
        status_ = status;
    }

    static
    public class PodcastRealmBuilder
        implements PodcastBuilder<PodcastRealm>
    {
        private PodcastRealm info_;

        public PodcastRealmBuilder(){
            info_ = new PodcastRealm();
        }

        @Override
        public void setTitle(String title){
            info_.setTitle(title);
        }

        @Override
        public void setURL(String url){
            info_.setURL(url);
        }

        @Override
        public void setIconURL(String iconURL){
            info_.setIconURL(iconURL);
        }

        @Override
        public void setEnabled(boolean enabled){
            info_.setEnabled(enabled);
        }
        
        @Override
        public void setUsername(String username){
            info_.setUsername(username);
        }

        @Override
        public void setPassword(String password){
            info_.setPassword(password);
        }

        @Override
        public void setStatus(int status){
            info_.setStatus(status);
        }

        @Override
        public PodcastRealm build(){
            return info_;
        }
    }
}
