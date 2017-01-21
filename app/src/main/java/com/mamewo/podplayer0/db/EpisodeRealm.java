package com.mamewo.podplayer0.db;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import io.realm.RealmModel;
import io.realm.annotations.RealmClass;
import io.realm.annotations.Index;
import io.realm.annotations.Ignore;
    
@RealmClass
public class EpisodeRealm
    implements RealmModel,
               Serializable
{
    static final String DATE_PATTERN = "EEE, dd MMM yyyy HH:mm:ss Z";
    static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DATE_PATTERN, Locale.US);

    private long id;
    private PodcastRealm podcast;
    @Index private String url;
    private String title;
    private String pubdatestr;
    private String link;
    private Date pubdate;
    private int occurIndex;
    private Date listened;
    private long podcastIndex;

    public EpisodeRealm(){
        id = 0;
        podcast = null;
        url = null;
        title = null;
        pubdatestr = null;
        link = null;
        pubdate = null;
        listened = null;      
        podcastIndex = 0;
    }

    public long getId(){
        return id;
    }

    public void setId(long id){
        this.id = id;
    }

    public PodcastRealm getPodcast(){
        return podcast;
    }

    public void setPodcast(PodcastRealm info){
        this.podcast = info;
    }

    public String getURL(){
        return url;
    }

    public void setURL(String url){
        this.url = url;
    }

    public String getTitle(){
        return title;
    }

    public void setTitle(String title){
        this.title = title;
    }

    public String getPubdateStr(DateFormat dateFormat){
        if(null == pubdate){
            return pubdatestr;
        }
        synchronized(dateFormat){
            return dateFormat.format(pubdate);
        }
    }

    public void setPubdateStr(String str){
        this.pubdatestr = str;
        try{
            synchronized(DATE_FORMAT) {
                pubdate = DATE_FORMAT.parse(str);
            }
        }
        catch(ParseException e){
            pubdate = null;
        }
    }

    public Date getPubdate(){
        if(null != pubdate){
            return pubdate;
        }
        try{
            synchronized(DATE_FORMAT) {
                pubdate = DATE_FORMAT.parse(pubdatestr);
            }
        }
        catch(ParseException e){
            pubdate = null;
        }
        return pubdate;
    }
   
    public void setPubdate(Date pubdate){
        this.pubdate = pubdate;
    }

    public String getLink(){
        return link;
    }

    public void setLink(String link){
        this.link = link;
    }

    public int getOccurIndex(){
        return occurIndex;
    }

    public void setOccurIndex(int index){
        this.occurIndex = index;
    }

    public Date getListened(){
        return listened;
    }
    
    public void setListenedDate(Date date){
        this.listened = date;
    }

    public long getPodcastIndex(){
        return podcastIndex;
    }

    public void setPodcastIndex(long index){
        this.podcastIndex = index;
    }
}
