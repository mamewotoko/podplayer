package com.mamewo.podplayer0.db;

import android.net.Uri;
import android.provider.BaseColumns;

public class Podcast {
	static final public String AUTHORITY = "com.mamewo.podplayer0.provider.Podcast";
	
	public static final class PodcastColumns implements BaseColumns {
		private PodcastColumns(){}
		public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY + "/podcasts");
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

	public static final class EpisodeColumns implements BaseColumns {
	}
}
