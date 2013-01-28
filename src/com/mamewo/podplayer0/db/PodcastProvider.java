package com.mamewo.podplayer0.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;

import com.mamewo.podplayer0.db.Podcast.EpisodeColumns;
import com.mamewo.podplayer0.db.Podcast.PlayHistoryColumns;
import com.mamewo.podplayer0.db.Podcast.PodcastColumns;
import com.mamewo.podplayer0.R;

import android.text.TextUtils;
import android.net.Uri;
import android.util.Log;
import android.content.ContentProvider;
import android.content.Context;
import android.content.ContentValues;
import android.content.ContentUris;
import android.content.UriMatcher;
import android.database.SQLException;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

public class PodcastProvider extends ContentProvider {
	private static final String TAG = "podplayer";
	
	private static final String DATABASE_NAME = "podcast.db";
	private static final int DATABASE_VERSION = 1;
	private static UriMatcher uriMatcher_ = null;

	private static final int PODCAST = 1;
	private static final int PODCAST_ID = 2;
	private static final int EPISODE = 3;
	
	static {
		uriMatcher_ = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher_.addURI(Podcast.AUTHORITY, PodcastColumns.PATH, PODCAST);
		uriMatcher_.addURI(Podcast.AUTHORITY, PodcastColumns.PATH+"/#", PODCAST_ID);
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
			db.execSQL("CREATE TABLE " + PodcastColumns.TABLE_NAME + " ("
					   + PodcastColumns._ID + " INTEGER PRIMARY KEY,"
					   + PodcastColumns.TITLE + " TEXT,"
					   + PodcastColumns.URL + " TEXT,"
					   + PodcastColumns.ICON_URL + " TEXT,"
					   + PodcastColumns.ENABLED + " BOOLEAN,"
					   + PodcastColumns.ORD + " INTEGER);");
			db.execSQL("CREATE TABLE " + EpisodeColumns.TABLE_NAME + "("
					   + EpisodeColumns._ID + " INTEGER PRIMARY KEY,"
					   + EpisodeColumns.PODCAST_ID + " INTEGER,"
					   + EpisodeColumns.URL + " TEXT,"
					   + EpisodeColumns.TITLE + " TEXT,"
					   + EpisodeColumns.PUBDATE + " TEXT,"
					   + EpisodeColumns.LINK_URL + " TEXT);");
			db.execSQL("CREATE TABLE " + PlayHistoryColumns.TABLE_NAME + "("
					   + PlayHistoryColumns._ID + " INTEGER PRIMARY KEY,"
					   + PlayHistoryColumns.EPISODE_ID + " INTEGER,"
					   + PlayHistoryColumns.PLAYED_DATE + " TEXT)");
			//TODO: migrate podcast data from JSON
			String[] allTitles = context_.getResources().getStringArray(R.array.pref_podcastlist_keys);
			String[] allURLs = context_.getResources().getStringArray(R.array.pref_podcastlist_urls);
			List<ContentValues> valuesList = new ArrayList<ContentValues>();
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
				db.insert(PodcastColumns.TABLE_NAME, null, values);
			}
			//TODO: cache icon data
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
		SQLiteDatabase db = null;
		Map<String, String> map = null;
		
		switch(uriMatcher_.match(uri)){
		case PODCAST:
			builder.setTables(PodcastColumns.TABLE_NAME);
			map = new HashMap<String, String>();
			map.put(PodcastColumns._ID, PodcastColumns._ID);
			map.put(PodcastColumns.TITLE, PodcastColumns.TITLE);
			map.put(PodcastColumns.URL, PodcastColumns.URL);
			map.put(PodcastColumns.ENABLED, PodcastColumns.ENABLED);
			map.put(PodcastColumns.ICON_URL, PodcastColumns.ICON_URL);
			builder.setProjectionMap(map);
			db = helper_.getReadableDatabase();
			c = builder.query(db, projection, selection, selectionArgs, null, null, null);
			break;
		case EPISODE:
			builder.setTables(PodcastColumns.TABLE_NAME+","+EpisodeColumns.TABLE_NAME);
			map = new HashMap<String, String>();
			String[] columns = new String[] {
					EpisodeColumns._ID,
					EpisodeColumns.TITLE,
					EpisodeColumns.URL,
					EpisodeColumns.PUBDATE,
					EpisodeColumns.LINK_URL,
					EpisodeColumns.PODCAST_ID
			};
			for(String column: columns){
				map.put(column, EpisodeColumns.TABLE_NAME+"."+column);
			}
// 			map.put(EpisodeColumns.TABLE_NAME+"."+EpisodeColumns._ID, EpisodeColumns._ID);
// 			map.put(EpisodeColumns.TABLE_NAME+"."+EpisodeColumns.TITLE, EpisodeColumns.TITLE);
// 			map.put(EpisodeColumns.TABLE_NAME+"."+EpisodeColumns.URL, EpisodeColumns.URL);
// 			map.put(EpisodeColumns.TABLE_NAME+"."+EpisodeColumns.PUBDATE, EpisodeColumns.PUBDATE);
// 			map.put(EpisodeColumns.TABLE_NAME+"."+EpisodeColumns.LINK_URL, EpisodeColumns.LINK_URL);
// 			map.put(EpisodeColumns.TABLE_NAME+"."+EpisodeColumns.PODCAST_ID, EpisodeColumns.PODCAST_ID);
			builder.setProjectionMap(map);
			builder.appendWhere(EpisodeColumns.PODCAST_ID
								+ "=" + PodcastColumns.TABLE_NAME+"."+PodcastColumns._ID);
			db = helper_.getReadableDatabase();
			c = builder.query(db, projection, selection, selectionArgs, null, null, null);
			//TODO: add last played time
			break;
		default:
			Log.d(TAG, "query: not handled uri " + uri);
			break;
		}
		return c;
	}

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
		SQLiteDatabase db = helper_.getWritableDatabase();
		int count;
		int matchResult = uriMatcher_.match(uri);
		switch(matchResult){
		case PODCAST_ID:
			//?
			String id = uri.getPathSegments().get(1);
			Log.d(TAG, "Provider.update: " + id + " " + values.get("enabled"));
			count = db.update(PodcastColumns.TABLE_NAME, values, PodcastColumns._ID + "=" + id
							  + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
			break;
		default:
			count = 0;
			break;
		}
		//test
		getContext().getContentResolver().notifyChange(PodcastColumns.CONTENT_URI, null);
		return count;
	}

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
		//TODO:
		//PODCAST_ID
		//EPISODE_ID?
		//HISTORY
		return 0;
	}

    @Override
    public Uri insert(Uri uri, ContentValues values) {
		int matchResult = uriMatcher_.match(uri);
		Uri result = null;
		long id;
		SQLiteDatabase db = helper_.getWritableDatabase();
		switch(matchResult){
		case PODCAST:
			id = db.insert(PodcastColumns.TABLE_NAME, null, values);
			if(id > 0){
				result = ContentUris.withAppendedId(PodcastColumns.CONTENT_URI, id);
				getContext().getContentResolver().notifyChange(result, null);
			}
			else {
				throw new SQLException("Failed to insert row into " + uri);
			}
			break;
		case EPISODE:
			id = db.insert(EpisodeColumns.TABLE_NAME, null, values);
			if(id > 0){
				result = ContentUris.withAppendedId(EpisodeColumns.CONTENT_URI, id);
				getContext().getContentResolver().notifyChange(result, null);
			}
			else {
				throw new SQLException("Failed to insert row into " + uri);
			}
			break;
		default:
			Log.d(TAG, "insert: not handled: " + uri);
			break;
		}
		return result;
	}

    @Override
    public String getType(Uri uri) {
		//TODO: return appropriate value according to uri
		return PodcastColumns.CONTENT_TYPE;
	}
}
