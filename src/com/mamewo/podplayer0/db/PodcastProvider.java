package com.mamewo.podplayer0.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;

import com.mamewo.podplayer0.db.Podcast.EpisodeColumns;
import com.mamewo.podplayer0.db.Podcast.PlayHistoryColumns;
import com.mamewo.podplayer0.db.Podcast.PodcastColumns;
import com.mamewo.podplayer0.R;

import android.net.Uri;
import android.util.Log;
import android.content.ContentProvider;
import android.content.Context;
import android.content.ContentValues;
import android.content.UriMatcher;

import java.util.Map;
import java.util.HashMap;

public class PodcastProvider extends ContentProvider {
	private static final String TAG = "podplayer";
	
	private static final String DATABASE_NAME = "podcast.db";
	private static final int DATABASE_VERSION = 1;
	private static UriMatcher uriMatcher_ = null;

	private static final int PODCAST = 1;
	private static final int EPISODE = 2;
	
	static {
		uriMatcher_ = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher_.addURI(Podcast.AUTHORITY, PodcastColumns.PATH, PODCAST);
		uriMatcher_.addURI(Podcast.AUTHORITY, EpisodeColumns.PATH, EPISODE);
	}
	
	private class DatabaseHelper
		extends SQLiteOpenHelper
	{
		Context context_;

		public DatabaseHelper(Context context){
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			context_ = context;
		}
		
		@Override
		public void onCreate(SQLiteDatabase db){
			db.execSQL("CREATE TABLE " + Podcast.PODCAST_TABLE_NAME + " ("
					   + PodcastColumns._ID + " INTEGER PRIMARY KEY,"
					   + PodcastColumns.TITLE + " TEXT,"
					   + PodcastColumns.URL + " TEXT,"
					   + PodcastColumns.ICON_URL + " TEXT,"
					   + PodcastColumns.ENABLED + " BOOLEAN,"
					   + PodcastColumns.ORD + " INTEGER);");
			db.execSQL("CREATE TABLE " + Podcast.EPISODE_TABLE_NAME + "("
						+ EpisodeColumns._ID + " INTEGER PRIMARY KEY,"
						+ EpisodeColumns.URL + " TEXT,"
						+ EpisodeColumns.TITLE + " TEXT,"
						+ EpisodeColumns.PUBDATE + " TEXT,"
						+ EpisodeColumns.LINK_URL + " TEXT);");
			db.execSQL("CREATE TABLE " + Podcast.PLAY_HISTORY_TABLE_NAME + "("
						+ PlayHistoryColumns._ID + " INTEGER PRIMARY KEY,"
						+ PlayHistoryColumns.EPISODE_ID + " INTEGER,"
						+ PlayHistoryColumns.PLAYED_DATE + " TEXT)");
			//TODO: migrate podcast data from JSON
			String[] allTitles = context_.getResources().getStringArray(R.array.pref_podcastlist_keys);
			String[] allURLs = context_.getResources().getStringArray(R.array.pref_podcastlist_urls);
			for(int i = 0; i < allTitles.length; i++){
				String title = allTitles[i];
				String url = allURLs[i];
				ContentValues values = new ContentValues();
				values.put(PodcastColumns.TITLE, title);
				values.put(PodcastColumns.URL, url);
				values.put(PodcastColumns.ICON_URL, "");
				values.put(PodcastColumns.ENABLED, Integer.valueOf(1));
				values.put(PodcastColumns.ORD, Integer.valueOf(i));
				//TODO: check result of insert
				db.insert(Podcast.PODCAST_TABLE_NAME, null, values);
			}
			//TODO: cache icon data
// 			Log.i(TAG, "db insert: " + result);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}
	}

	private DatabaseHelper helper_;
	
	@Override
	public boolean onCreate() {
		helper_ = new DatabaseHelper(getContext());
		return true;
	}

	@Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
		Log.d(TAG, "query: uri " + uri.toString());
		Cursor c = null;
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();

		switch(uriMatcher_.match(uri)){
		case PODCAST:
			builder.setTables(Podcast.PODCAST_TABLE_NAME);
			Map<String, String> map = new HashMap<String, String>();
			map.put(PodcastColumns._ID, PodcastColumns._ID);
			map.put(PodcastColumns.TITLE, PodcastColumns.TITLE);
			map.put(PodcastColumns.URL, PodcastColumns.URL);
			map.put(PodcastColumns.ENABLED, PodcastColumns.ENABLED);
			map.put(PodcastColumns.ICON_URL, PodcastColumns.ICON_URL);
			builder.setProjectionMap(map);
			SQLiteDatabase db = helper_.getReadableDatabase();
			c = builder.query(db, projection, selection, selectionArgs, null, null, null);
			break;
		case EPISODE:
			//fall through
		default:
			Log.d(TAG, "query: not handled uri " + uri);
			break;
		}
		return c;
	}

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
		return 0;
	}

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
		return 0;
	}

    @Override
    public Uri insert(Uri uri, ContentValues values) {
		switch(uriMatcher_.match(uri)){
		case PODCAST:
			SQLiteDatabase db = helper_.getWritableDatabase();
			long id = db.insert(Podcast.PODCAST_TABLE_NAME, null, values);
			break;
		default:
			Log.d(TAG, "insert: not handled: " + uri);
			break;
		}

		return null;
	}

    @Override
    public String getType(Uri uri) {
		return PodcastColumns.CONTENT_TYPE;
	}
}
