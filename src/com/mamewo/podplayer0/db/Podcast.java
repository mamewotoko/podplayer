package com.mamewo.podplayer0.db;

import android.net.Uri;
import android.provider.BaseColumns;

public class Podcast {
	public static final String AUTHORITY = "com.mamewo.podplayer0.provider.Podcast";

	public static final class PodcastColumns
		implements BaseColumns
	{
		public static final String PATH = "podcasts";
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + PATH);
		public static final String TABLE_NAME = "podcast";

		private PodcastColumns(){}
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/xml+rss";

		//TEXT
		public static final String TITLE = "title";
		//TEXT
		public static final String URL = "url";
		//TEXT
		public static final String ICON_URL = "icon_url";
		//BOOLEAN
		public static final String ENABLED = "enabled";
		//INTEGER
		public static final String ORD = "ord";
	}

	public static final class EpisodeColumns
		implements BaseColumns
	{
		public static final String PATH = "episode";
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + PATH);
		public static final String TABLE_NAME = "episode";

		private EpisodeColumns(){}
		//INTEGER
		public static final String PODCAST_ID = "podcast_id";
		//TEXT
		public static final String TITLE = "title";
		//TEXT
		public static final String URL = "url";
		//TEXT
		public static final String PUBDATE = "pubdate";
		//TEXT
		public static final String LINK_URL = "link_url";
	}

	public static final class PlayHistoryColumns
		implements BaseColumns
	{
		private PlayHistoryColumns() {}
		public static final String PATH = "play_history";
		public static final String TABLE_NAME = "play_history";
		
		//int
		public static final String EPISODE_ID = "episode_id";
		//String
		public static final String PLAYED_DATE = "played_date";
	}
}
