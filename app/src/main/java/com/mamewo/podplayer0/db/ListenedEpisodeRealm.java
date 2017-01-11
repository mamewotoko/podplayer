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
    
@RealmClass
public class ListenedEpisodeRealm
    implements RealmModel,
               Serializable
{
    private Date date;
    private EpisodeRealm episode;
    private String podcastTitle;
    private String episodeTitle;

    public ListenedEpisodeRealm(){
        date = null;
        episode = null;
        podcastTitle = null;
        episodeTitle = null;
    }

    public ListenedEpisodeRealm(Date date, EpisodeRealm episode, String podcastTitle, String episodeTitle){
        this.date = date;
        this.episode = episode;
        this.podcastTitle = podcastTitle;
        this.episodeTitle = episodeTitle;
    }
    
    public Date getDate(){
        return date;
    }

    public void setDate(Date date){
        this.date = date;
    }

    public EpisodeRealm getEpisode(){
        return episode;
    }

    public void setEpisode(EpisodeRealm episode){
        this.episode = episode;
    }

    public String getPodcastTitle(){
        return podcastTitle;
    }
    
    public void setPodcastTitle(String title){
        this.podcastTitle = title;
    }
    
    public String getEpisodeTitle(){
        return episodeTitle;
    }

    public void setEpisodeTitle(String title){
        this.episodeTitle = title;
    }
}
