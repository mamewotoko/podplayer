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
import android.text.TextUtils;
import android.util.Log;
import android.provider.BaseColumns;

import java.util.List;

import static com.mamewo.podplayer0.Const.*;

import com.mamewo.lib.podcast_parser.EpisodeInfo;
import com.mamewo.lib.podcast_parser.PodcastInfo;

public class EpisodeProvider
	extends ContentProvider
{
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
		return dbhelper_.query(projection, selection, selectionArgs, sortOrder);
	}

    @Override
    public String getType(Uri uri) {
		int match = uriMatcher_.match(uri);
		switch(match){
		case EPISODE:
			return EpisodeColumns.CONTENT_TYPE;
		case EPISODE_ID:
			return EpisodeColumns.CONTENT_ITEM_TYPE;
		default:
			return null;
		}
	}

	//unique insert
    @Override
    public Uri insert(Uri uri, ContentValues values) {
		Log.d(TAG, "Provider insert: episode ");

		if(uriMatcher_.match(uri) != EPISODE){
			throw new IllegalArgumentException("Unknown URI"+uri);
		}
		if(values == null){
			throw new IllegalArgumentException("null initialValues");
		}
		SQLiteDatabase db = dbhelper_.getWritableDatabase();

		//duplicate item check
		String[] args = new String[]{ values.getAsString(EpisodeColumns.PODCAST),
									  values.getAsString(EpisodeColumns.TITLE),
									  values.getAsString(EpisodeColumns.PUBDATE)
		};
		Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE_NAME+" WHERE "
									+ EpisodeColumns.PODCAST+ " = ? AND "
									+ EpisodeColumns.TITLE+" = ? AND "
									+ EpisodeColumns.PUBDATE+"= ?", args);	
		long rowId;
		if(cursor.moveToNext()){
			Log.d(TAG, "exists: " + cursor.getString(cursor.getColumnIndex(EpisodeColumns.TITLE)));
			rowId = cursor.getLong(cursor.getColumnIndex(EpisodeColumns._ID));
		}
		else {		
			rowId = db.insert(TABLE_NAME, null, new ContentValues(values));
		}
		if(rowId <= 0){
			throw new SQLException("Failed to insert row into " + uri);
		}
		//TODO: check
		Uri episodeUri = ContentUris.withAppendedId(EpisodeColumns.CONTENT_URI, rowId);
		getContext().getContentResolver().notifyChange(episodeUri, null);
		return episodeUri;
	}

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = dbhelper_.getWritableDatabase();
        int count;
		switch (uriMatcher_.match(uri)) {
        case EPISODE:
            count = db.delete(TABLE_NAME, where, whereArgs);
            break;
        case EPISODE_ID:
			String episodeId = uri.getPathSegments().get(1);
            count = db.delete(TABLE_NAME,
							  EpisodeColumns._ID + "=" + episodeId
							  + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
							  whereArgs);
			break;
		default:
            throw new IllegalArgumentException("Unknown URI " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
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

		private static
		final String CREATE_SQL = "CREATE TABLE "+TABLE_NAME+" ("
			+ EpisodeColumns._ID + " INTEGER PRIMARY KEY,"
			+ EpisodeColumns.TITLE + " TEXT,"
			+ EpisodeColumns.URL + " TEXT,"
			+ EpisodeColumns.PUBDATE + " TEXT,"
			+ EpisodeColumns.PODCAST + " TEXT,"
			+ EpisodeColumns.LISTENED+" INTEGER"
			+ ");";

		public DBHelper(Context context){
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			context_ = context;
		}

		public Cursor query(String[] projection,
							String selection,
							String[] selectionArgs,
							String sortOrder)
		{
			Log.d(TAG, "DBHelper.query: " + selection + " " + selectionArgs);
			SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
			builder.setTables(TABLE_NAME);
			Cursor cursor = builder.query(getReadableDatabase(),
										  EpisodeColumns.LIST,
										  selection,
										  selectionArgs,
										  null,
										  null,
										  null);
			if(cursor == null){
				Log.d(TAG, "DBHelper.load: cursor is null");
				return null;
			}
			if(!cursor.moveToFirst()){
				Log.d(TAG, "DBHelper.load: cursor is empty");
				return null;
			}
			Log.d(TAG, "DBHelper.load: cursor returned");
				
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

		// public void write(final List<PodcastInfo> podcastList,
		// 				  final List<EpisodeInfo> episodeList)
		// {
		// 	for(EpisodeInfo info: episodeList){
		// 		ContentValues v = new ContentValues();
		// 		v.put(EpisodeColumns.TITLE, info.title_);
		// 		v.put(EpisodeColumns.URL, info.url_);
		// 		v.put(EpisodeColumns.DATE, info.pubdate_);
		// 		v.put(EpisodeColumns.PODCAST, podcastList.get(info.index_).url_.toString());
		// 		v.put(EpisodeColumns.LISTENED, "false");
		// 		database_.insert(TABLE_NAME, null, v);
		// 		//check return value
		// 	}
		// }
	}
}
