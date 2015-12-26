package com.mamewo.podplayer0;

import android.provider.BaseColumns;
import android.net.Uri;

public class EpisodeColumns
	implements BaseColumns
{
	private EpisodeColumns() { }

	public static
	final Uri CONTENT_URI = Uri.parse("content://"+EpisodeProvider.AUTHORITY+"/episode");
	public static
	final Uri EPISODE_URI = Uri.parse("content://"+EpisodeProvider.AUTHORITY+"/episode/");
	
	public static
	final String CONTENT_TYPE = "vnd.android.cursor.dir/episode";
	public static
	final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/episode";
			
	public static
		final String TITLE = "title";
	public static
		final String URL = "url";
	public static
		final String PUBDATE = "pubdate";
	public static
		final String PODCAST = "podcast_url";
	public static
		final String LISTENED = "listened";

	public static
	final String[] LIST = new String[]{
		_ID,
		TITLE,
		URL,
		PUBDATE,
		PODCAST,
		LISTENED
	};

	//in LIST
	public static
	final int ID_INDEX = 0;
	public static
	final int TITLE_INDEX = 1;
	public static
	final int URL_INDEX = 2;
	public static
	final int PUBDATE_INDEX = 3;
	public static
	final int PODCAST_INDEX = 4;
	public static
	final int LISTENED_INDEX = 5;
}
