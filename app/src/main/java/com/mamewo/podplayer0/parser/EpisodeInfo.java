package com.mamewo.podplayer0.parser;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.text.DateFormat;
import android.util.Log;

public class EpisodeInfo
    implements Serializable
{
    //parse pattern
    static final String DATE_PATTERN = "EEE, dd MMM yyyy HH:mm:ss Z";
    static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DATE_PATTERN, Locale.US);

    static final
    String TAG = "podparser";
    static final
    private DateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm Z");
        
    private static final long serialVersionUID = 1L;
    public Podcast podcast_;
    final private String url_;
    final private String title_;
    final private String pubdate_;
    final private String link_;
    final public int index_;

    private Date pubdateobj_;
    
    public EpisodeInfo(Podcast podcast, String url, String title, String pubdate, String link, int index) {
        podcast_ = podcast;
        url_ = url;
        title_ = title;
        pubdate_ = pubdate;
        try {
            synchronized(DATE_FORMAT) {
                pubdateobj_ = DATE_FORMAT.parse(pubdate);
            }
        }
        catch (ParseException e) {
            pubdateobj_ = null;
            Log.d(TAG, "parse error: " + pubdate);
        }
        link_ = link;
        index_ = index;
    }

    public String getPubdateString(){
        return getPubdateString(DEFAULT_DATE_FORMAT);
    }

    public String getPubdateString(DateFormat format){
        if(null == pubdateobj_){
            return pubdate_;
        }
        synchronized(format){
            return format.format(pubdateobj_);
        }
    }

    public boolean equalEpisode(EpisodeInfo other){
        return getURL().equals(other.getURL());
    }

    public Podcast getPodcast(){
        return podcast_;
    }
    
    public String getTitle(){
        return title_;
    }
    
    public String getURL(){
        return url_;
    }

    public String getUsername(){
        return podcast_.getUsername();
    }

    public String getPassword(){
        return podcast_.getPassword();
    }

    public String getURLWithAuthInfo(){
        if(null == podcast_.getUsername() || null == podcast_.getPassword()){
            return url_;
        }
        return podcast_.addUserInfo(url_);
    }

    public String getLink(){
        return link_;
    }

    public int getIndex(){
        return index_;
    }
    
    // static
    // public class PubdateComparator
    //     implements Serializable,
    //                Comparator<EpisodeInfo>                   
    // {
    //     @Override
    //     public int compare(EpisodeInfo o1, EpisodeInfo o2){
    //         if(null == o1.pubdateobj_ && null == o1.pubdateobj_){
    //             //use occurence order or compare date string
    //             return 0;
    //         }
    //         if(null == o1.pubdateobj_){
    //             return -1;
    //         }
    //         if(null == o2.pubdateobj_){
    //             return 1;
    //         }
    //         return o1.pubdateobj_.compareTo(o2.pubdateobj_);
    //     }
    // }
    
    // static
    // public OccurenceOrderComparator
    //     extends Compator<EpisodeInfo>
    //     implements Serializable
    // {
    //     @Override
    //     public int compare(EpisodeInfo o1, EpisodeInfo o2){
    //         if(o1.index_ != o2.index_){
    //             return o1.index_ - o2.index_;
    //         }
    //         //
    //     }
    // }
}
