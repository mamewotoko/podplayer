package com.mamewo.podplayer0.db;

import java.io.Serializable;
import java.net.URL;
import java.net.MalformedURLException;
import android.util.Log;
import com.mamewo.podplayer0.parser.Podcast;
import com.mamewo.podplayer0.parser.PodcastBuilder;
import io.realm.RealmModel;
import io.realm.annotations.RealmClass;
import io.realm.annotations.Ignore;
import io.realm.annotations.Index;

import static com.mamewo.podplayer0.Const.*;

@RealmClass
public class PodcastRealm
    implements RealmModel,
               Podcast,
               Serializable
{
    private static final long serialVersionUID = 76131894671950703L;
    private long id;
    private String title;
    @Index private String url;
    private boolean enabled;
    private String iconURL;

    //TODO: hold in secure area?
    private String username;
    private String password;
    private int status;
    private int occurIndex;
    
    @Ignore
    private URL parsedURL_;
    
    public PodcastRealm(){
        id = 0;
        title = null;
        url = null;
        enabled = true;
        iconURL = null;
        username = null;
        password = null;
        status = Podcast.UNKNOWN;
        occurIndex = 0;
    }
    
    public String getTitle(){
        return title;
    }

    public void setTitle(String title){
        this.title = title;
    }
    
    public void setURL(String url) {
        this.url = url;
        try{
            parsedURL_ = new URL(url);
        }
        catch(MalformedURLException e){
            Log.d(TAG, "parse url", e);
            parsedURL_ = null;
        }
    }

    public long getId(){
        return id;
    }

    public void setId(long id){
        this.id = id;
    }
    
    public String getURL(){
        return url;
    }

    public URL getParsedURL(){
        try{
            parsedURL_ = new URL(url);
        }
        catch(MalformedURLException e){
            Log.d(TAG, "parse url", e);
            parsedURL_ = null;
        }
        return parsedURL_;
    }
    
    public URL getURLWithAuthInfo(){
        try{
            if(null != this.username && null != this.password){
                return new URL(addUserInfo(url));
            }
            return parsedURL_;
        }
        catch (MalformedURLException e){
            return null;
        }
    }

    public boolean getEnabled(){
        return enabled;
    }

    public void setEnabled(boolean enabled){
        this.enabled = enabled;
    }
    
    public String getIconURL(){
        return iconURL;
    }

    public String getIconURLWithAuthInfo(){
        return addUserInfo(iconURL);
    }

    public void setIconURL(String url){
        iconURL = url;
    }

    public String getUsername(){
        return username;
    }

    public void setUsername(String username){
        this.username = username;
    }

    public String getPassword(){
        return password;
    }

    public void setPassword(String password){
        this.password = password;
    }

    public String addUserInfo(String url){
        Log.d(TAG, "addUserInfo: " + url + " " + this.username + " " + this.password);
        if(null == url || null == this.username || null == this.password){
            return url;
        }
        int pos = url.indexOf("://");
        return url.substring(0, pos) + "://" + this.username +":"+this.password+"@"+url.substring(pos+3);
    }

    public int getStatus(){
        return status;
    }

    public void setStatus(int status){
        this.status = status;
    }

    public int getOccurIndex(){
        return occurIndex;
    }

    public void setOccurIndex(int index){
        this.occurIndex = index;
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
