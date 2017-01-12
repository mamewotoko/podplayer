package com.mamewo.podplayer0.db;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.RealmChangeListener;
import io.realm.Sort;
import android.util.Log;
import android.support.v4.util.LongSparseArray;

import static com.mamewo.podplayer0.Const.*;

public class SimpleQuery {
    private String title_;
    private boolean skipListened_;
    //Const.java 
    private int order_;
    private LongSparseArray<RealmResults<EpisodeRealm>> podcastId2Episode_;
    
    private RealmResults<PodcastRealm> podcastList_;
    private RealmResults<EpisodeRealm> episodeList_;
    private DataChangeListener listener_;
    
    public interface DataChangeListener {
        public void notifyPodcastListChanged(RealmResults<PodcastRealm> result);
        public void notifyEpisodeListAllChanged(RealmResults<EpisodeRealm> result);
        public void notifyEpisodeListGroupChanged(long podcastId, RealmResults<EpisodeRealm> result);
    }
    private RealmChangeListener<RealmResults<PodcastRealm>> internalPodcastListener_;
    private RealmChangeListener<RealmResults<EpisodeRealm>> internalEpisodeListener_;
    private LongSparseArray<RealmChangeListener<RealmResults<EpisodeRealm>>> podcastId2InternalEpisodeListener_;

    public SimpleQuery(String title, boolean skipListened, int order, DataChangeListener listener){
        title_ = title;
        skipListened_ = skipListened;
        order_ = order;
        episodeList_ = null;
        listener_ = listener;
        internalPodcastListener_ = null;
        internalPodcastListener_ = null;
        podcastId2InternalEpisodeListener_ = new LongSparseArray<RealmChangeListener<RealmResults<EpisodeRealm>>>();
        podcastId2Episode_  = new LongSparseArray<RealmResults<EpisodeRealm>>();
    }

    public SimpleQuery(String title, boolean skipListened, int order){
        this(title, skipListened, order, null);
    }
    
    public SimpleQuery(String title, boolean skipListened){
        this(title, skipListened, APPEARANCE_ORDER, null);
    }
    
    public RealmResults<PodcastRealm> getPodcastList(){
        if(null != podcastList_){
            return podcastList_;
        }
        Realm realm = Realm.getDefaultInstance();
        RealmQuery<PodcastRealm> query = null;
        podcastList_ = realm.where(PodcastRealm.class).equalTo("enabled", true).findAll();
        if(null != listener_){
            internalPodcastListener_ = new RealmChangeListener<RealmResults<PodcastRealm>>(){
                @Override
                public void onChange(RealmResults<PodcastRealm> results){
                    listener_.notifyPodcastListChanged(results);
                }
            };
        }
        return podcastList_;
    }

    public RealmResults<EpisodeRealm> getEpisodeList(){
        if(null != episodeList_){
            return episodeList_;
        }
        Realm realm = Realm.getDefaultInstance();
        RealmQuery<EpisodeRealm> episodeQuery = realm.where(EpisodeRealm.class);
        if(podcastList_.size() > 0){
            Long[] podcastIdList = new Long[podcastList_.size()];
            
            for(int i = 0; i < podcastList_.size(); i++){
                podcastIdList[i] = podcastList_.get(i).getId();
            }
            episodeQuery.in("podcast.id", podcastIdList);
        }
        if(skipListened_){
            episodeQuery.isNull("listened");
        }
        RealmResults<EpisodeRealm> results;
        if(order_ == APPEARANCE_ORDER || order_ == REVERSE_APPEARANCE_ORDER){
            results = episodeQuery.findAll();
            if(order_ == REVERSE_APPEARANCE_ORDER){
                realm.beginTransaction();
                for(EpisodeRealm episode: results){
                    long index = episode.getPodcast().getId()*100000 + 10000 - episode.getOccurIndex();
                    episode.setPodcastIndex(index);
                }
                realm.commitTransaction();
                // String[] keys = { "podcastIndex", "occurIndex" };
                // Sort[] order = { Sort.ASCENDING, Sort.DESCENDING };
                results = results.sort("podcastIndex", Sort.ASCENDING);
            }
        }
        else if(order_ == DATE_OLDER_FIRST_ORDER){
            results = episodeQuery.findAllSorted("pubdate", Sort.ASCENDING);
        }
        else {
            results = episodeQuery.findAllSorted("pubdate", Sort.DESCENDING);
        }
        episodeList_ = results;
        if(listener_ != null){
            RealmChangeListener<RealmResults<EpisodeRealm>> l = new RealmChangeListener<RealmResults<EpisodeRealm>>(){
                @Override
                public void onChange(RealmResults<EpisodeRealm> results){
                    listener_.notifyEpisodeListAllChanged(results);
                }
                };
            internalEpisodeListener_ = l;
        }
        return episodeList_;
    }

    public RealmResults<EpisodeRealm> getEpisodeList(final long podcastId){
        if(null != podcastId2Episode_.get(podcastId)){
            return podcastId2Episode_.get(podcastId);
        }
        Realm realm = Realm.getDefaultInstance();
        RealmQuery<EpisodeRealm> episodeQuery = realm.where(EpisodeRealm.class);
        episodeQuery.equalTo("podcast.id", podcastId);
        if(skipListened_){
            episodeQuery.isNull("listened");
        }
        //TODO: sort
        RealmResults<EpisodeRealm> episodeList = episodeQuery.findAll();
        podcastId2Episode_.put(podcastId, episodeList);
        if(podcastId2InternalEpisodeListener_.get(podcastId) != null){
            RealmChangeListener<RealmResults<EpisodeRealm>> listener = new RealmChangeListener<RealmResults<EpisodeRealm>>(){
                @Override
                public void onChange(RealmResults<EpisodeRealm> results){
                    listener_.notifyEpisodeListGroupChanged(podcastId, results);
                }
            };
            podcastId2InternalEpisodeListener_.put(podcastId, listener);
        }
        return episodeList_;
    }
}
