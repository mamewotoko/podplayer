package com.mamewo.podplayer0;

import android.provider.BaseColumns;
import android.net.Uri;

public class EpisodeColumns
	implements BaseColumns
{
	private EpisodeColumns() { }

	public static
	final Uri CONTENT_URI = Uri.parse("conent://"+EpisodeProvider.AUTHORITY+"/episodes");
	public static
	final String CONTENT_TYPE = "vnd.android.cursor.dir/episode";
	public static
	final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/episode";
			
	public static
		final String TITLE = "title";
	public static
		final String URL = "url";
	public static
		final String DATE = "date";
	public static
		final String PODCAST = "podcast_url";
	public static
		final String LISTENED = "listened";

	public static
	final String[] LIST = new String[]{
		TITLE,
		URL,
		DATE,
		PODCAST,
		LISTENED
	};
}
