package com.mamewo.podplayer0;

import com.mamewo.lib.podcast_parser.EpisodeInfo;
import java.io.Serializable;

public class DBEpisodeInfo
	extends EpisodeInfo
	implements Serializable
{
	private long id_;
	private long listenedTime_;
	
	public DBEpisodeInfo(String url,
						 String title,
						 String pubdate,
						 String link,
						 int podcastIndex,
						 long id,
						 long listenedTime)
	{
		super(url, title, pubdate, link, podcastIndex);
		id_ = id;
		listenedTime_ = listenedTime;
	}

	public long getId(){
		return id_;
	}

	public void setListenedTime(long time){
		listenedTime_ = time;
	}

	public long getListenedTime(){
		return listenedTime_;
	}
	
	@Override
	public boolean equalEpisode(EpisodeInfo info){
		if(info instanceof DBEpisodeInfo){
			DBEpisodeInfo dbinfo = (DBEpisodeInfo)info;
			return id_ == dbinfo.id_;
		}
		return super.equalEpisode(info);
	}
}
