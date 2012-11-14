package com.mamewo.podplayer0.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import com.mamewo.podplayer0.db.Podcast.PodcastColumns;
import android.net.Uri;
import android.util.Log;
import android.content.ContentProvider;
import android.content.Context;
import android.content.ContentValues;

import java.util.Map;
import java.util.HashMap;

public class PodcastProvider extends ContentProvider {
	private static final String TAG = "podplayer";
	
	private static final String DATABASE_NAME = "podcast.db";
	private static final int DATABASE_VERSION = 1;
	private static final String PODCAST_TABLE_NAME = "podcast";

	private static final String EPISODE_TABLE_NAME = "episode";
	
	private static class DatabaseHelper extends SQLiteOpenHelper {
		public DatabaseHelper(Context context){
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db){
			db.execSQL("CREATE TABLE " + PODCAST_TABLE_NAME + " ("
					   + PodcastColumns._ID + " INTEGER PRIMARY KEY,"
					   + PodcastColumns.TITLE + " TEXT,"
					   + PodcastColumns.URL + " TEXT,"
					   + PodcastColumns.ICON_URL + " TEXT,"
					   + PodcastColumns.ENABLED + " BOOLEAN,"
					   + PodcastColumns.ORD + " INTEGER);");
			//TODO: migrate podcast data from json
			ContentValues values = new ContentValues();
			values.put(PodcastColumns.TITLE, "test");
			values.put(PodcastColumns.URL, "http://podcast.1242.com/ningendoc/index.xml");
			values.put(PodcastColumns.ICON_URL, "http://podcast.1242.com/image/ningendoc.jpg");
			values.put(PodcastColumns.ENABLED, Integer.valueOf(1));
			values.put(PodcastColumns.ORD, Integer.valueOf(1));
			long result = db.insert(PODCAST_TABLE_NAME, null, values);
			Log.i(TAG, "db insert: " + result);
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
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		builder.setTables(PODCAST_TABLE_NAME);
		Map map = new HashMap();
		map.put(PodcastColumns._ID, PodcastColumns._ID);
		map.put(PodcastColumns.TITLE, PodcastColumns.TITLE);
		map.put(PodcastColumns.URL, PodcastColumns.URL);
		map.put(PodcastColumns.ICON_URL, PodcastColumns.ICON_URL);
		builder.setProjectionMap(map);
		SQLiteDatabase db = helper_.getReadableDatabase();
		Cursor c = builder.query(db, projection, selection, selectionArgs, null, null, null);
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
    public Uri insert(Uri uri, ContentValues initialValues) {
		return null;
	}

    @Override
    public String getType(Uri uri) {
		return PodcastColumns.CONTENT_TYPE;
	}
}
