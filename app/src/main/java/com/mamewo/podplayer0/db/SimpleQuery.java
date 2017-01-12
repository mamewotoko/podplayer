package com.mamewo.podplayer0.db;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;

import static com.mamewo.podplayer0.Const.*;

public class SimpleQuery {
    private String title_;
    private boolean skipListened_;
    //Const.java 
    private int order_;
    
    public SimpleQuery(String title, boolean skipListened, int order){
        title_ = title;
        skipListened_ = skipListened;
        order_ = order;
    }

    public SimpleQuery(String title, boolean skipListened){
        this(title, skipListened, APPEARANCE_ORDER);
    }
    
    public RealmResults<PodcastRealm> getPodcastList(){
        Realm realm = Realm.getDefaultInstance();
        RealmQuery<PodcastRealm> query = null;
        return realm.where(PodcastRealm.class).equalTo("enabled", true).findAll();
    }

    public RealmResults<EpisodeRealm> getEpisodeList(RealmResults<PodcastRealm> podcastList){
        Realm realm = Realm.getDefaultInstance();
        RealmQuery<EpisodeRealm> episodeQuery = realm.where(EpisodeRealm.class);
        if(podcastList.size() > 0){
            Long[] podcastIdList = new Long[podcastList.size()];
            
            for(int i = 0; i < podcastList.size(); i++){
                podcastIdList[i] = podcastList.get(i).getId();
            }
            episodeQuery.in("podcast.id", podcastIdList);
        }
        if(skipListened_){
            episodeQuery.isNull("listened");
        }
        RealmResults<EpisodeRealm> results;
        if(order_ == APPEARANCE_ORDER || order_ == REVERSE_APPEARANCE_ORDER){
            results = episodeQuery.findAll();
            // if(order_ == REVERSE_APPEARANCE_ORDER){
            //     for(EpisodeRealm episode: results){
            //         realm.beginTransaction();
            //         episode.setPodcastIndex(episode.getPodcast().getId());
            //         realm.commitTransaction();
            //     }
            //     String[] keys = { "podcastIndex", "occurIndex" };
            //     Sort[] order = { Sort.ASCENDING, Sort.DESCENDING };
            //     results = results.sort(keys, order);
            // }
        }
        else if(order_ == DATE_OLDER_FIRST_ORDER){
            results = episodeQuery.findAllSorted("pubdate", Sort.ASCENDING);
        }
        else {
            results = episodeQuery.findAllSorted("pubdate", Sort.DESCENDING);
        }
        return results;
    }

    public RealmResults<EpisodeRealm> getEpisodeList(long podcastId){
        Realm realm = Realm.getDefaultInstance();
        RealmQuery<EpisodeRealm> episodeQuery = realm.where(EpisodeRealm.class);
        episodeQuery.equalTo("podcast.id", podcastId);
        if(skipListened_){
            episodeQuery.isNull("listened");
        }
        return episodeQuery.findAll();
    }
}
