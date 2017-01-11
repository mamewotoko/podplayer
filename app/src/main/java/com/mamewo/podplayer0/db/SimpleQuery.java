package com.mamewo.podplayer0.db;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;

public class SimpleQuery {
    private String title_;
    private boolean skipListened_;
    
    public SimpleQuery(String title, boolean skipListened){
        title_ = title;
        skipListened_ = skipListened;
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
        return episodeQuery.findAll();
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
