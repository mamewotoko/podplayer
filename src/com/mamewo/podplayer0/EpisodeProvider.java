package com.mamewo.podplayer0;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.content.ContentProvider;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

import java.util.List;

import com.mamewo.lib.podcast_parser.EpisodeInfo;
import com.mamewo.lib.podcast_parser.PodcastInfo;

public class EpisodeProvider extends ContentProvider {
	public static
	final String AUTHORITY = "com.mamewo.podplayer0.provider.Episode";

	private static
	final String TABLE_NAME = "episode";

	private static
	final UriMatcher uriMatcher_ = new UriMatcher(UriMatcher.NO_MATCH);

	private static
		final int EPISODE = 1;
	private static
		final int EPISODE_ID = 2;

	private DBHelper dbhelper_;

    @Override
    public boolean onCreate() {
		dbhelper_ = new DBHelper(getContext());
		return true;
	}

    @Override
    public Cursor query(Uri uri,
						String[] projection,
						String selection,
						String[] selectionArgs,
						String sortOrder)
	{
		return dbhelper_.load();
	}

    @Override
    public String getType(Uri uri) {
		int match = uriMatcher_.match(uri);
		switch(match){
		case EPISODE:
			return "vnd.android.cursor.dir/episode";
		case EPISODE_ID:
			return "vnd.android.cursor.item/episode";
		default:
			return null;
		}
	}

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
		if(uriMatcher_.match(uri) != EPISODE){
			throw new IllegalArgumentException("Unknown URI"+uri);
		}
		if(initialValues == null){
			throw new IllegalArgumentException("null initialValues");
		}
		SQLiteDatabase db = dbhelper_.getWritableDatabase();
		//db.insert();
		return null;
	}

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		return -1;
	}

	@Override
	public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
		return -1;
	}

	static {
		uriMatcher_.addURI(AUTHORITY, "episode", EPISODE);
		uriMatcher_.addURI(AUTHORITY, "episode/#", EPISODE_ID);
	}

	static
	public class DBHelper
		extends SQLiteOpenHelper
	{
		private final Context context_;
		private SQLiteDatabase database_;
		private static
			final String DATABASE_NAME = "podcast_db";
		private static
			final int DATABASE_VERSION = 1;
		public static
			final String TITLE_COLUMN = "title";
		public static
			final String URL_COLUMN = "url";
		public static
			final String DATE_COLUMN = "date";
		public static
			final String PODCAST_COLUMN = "podcast_url";
		public static
			final String LISTENED_COLUMN = "listened";

		public static
			final String[] COLUMNS = new String[]{
			TITLE_COLUMN,
			URL_COLUMN,
			DATE_COLUMN,
			PODCAST_COLUMN,
			LISTENED_COLUMN
		};

		private static
			final String CREATE_SQL = "CREATE TABLE USING fts3 (" 
			+ TITLE_COLUMN + "," + URL_COLUMN + "," + DATE_COLUMN + ","
			+ PODCAST_COLUMN + "," + LISTENED_COLUMN+");";

		public DBHelper(Context context){
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			context_ = context;
		}

		public Cursor load(){
			SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
			builder.setTables(TABLE_NAME);
			Cursor cursor = builder.query(getReadableDatabase(), COLUMNS, null, null, null, null, null);
			if(cursor == null){
				return null;
			}
			if(!cursor.moveToFirst()){
				return null;
			}
			return cursor;
		}
	
		@Override
		public void onCreate(SQLiteDatabase db) {
			database_ = db;
			database_.execSQL(CREATE_SQL);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
			onCreate(db);
		}

		public void write(final List<PodcastInfo> podcastList,
						  final List<EpisodeInfo> episodeList)
		{
			for(EpisodeInfo info: episodeList){
				ContentValues v = new ContentValues();
				v.put(TITLE_COLUMN, info.title_);
				v.put(URL_COLUMN, info.url_);
				v.put(DATE_COLUMN, info.pubdate_);
				v.put(PODCAST_COLUMN, podcastList.get(info.index_).url_.toString());
				v.put(LISTENED_COLUMN, "false");
				database_.insert(TABLE_NAME, null, v);
				//check return value
			}
		}
	}
}
