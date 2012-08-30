package com.mamewo.podplayer0;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.mamewo.podplayer0.PlayerService.MusicInfo;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;

public class BaseGetPodcastTask
	extends AsyncTask<URL, MusicInfo, Void>
{
	private Context context_;
	private String[] allPodcastURLs_;
	private URL[] iconURL_;
	private Drawable[] iconData_;
	private boolean showPodcastIcon_;
	private List<MusicInfo> buffer_;
	int timeoutSec_;
	final static
	private int BUFFER_SIZE = 10;
	final static
	MusicInfo[] DUMMY_ARRAY = new MusicInfo[0];
	
	final static
	private String TAG = "podplayer";

	private enum TagName {
		TITLE, PUBDATE, LINK, NONE
	};

	public BaseGetPodcastTask(Context context, String[] allPodcastURLs,
							URL[] iconURL, Drawable[] iconData,
							boolean showPodcastIcon, int timeout) {
		context_ = context;
		allPodcastURLs_ = allPodcastURLs;
		iconURL_ = iconURL;
		iconData_ = iconData;
		showPodcastIcon_ = showPodcastIcon;
		timeoutSec_ = timeout;
		buffer_ = new ArrayList<MusicInfo>();
	}

	static
	protected InputStream getInputStreamFromURL(URL url, int timeout)
		throws IOException
	{
		URLConnection conn = url.openConnection();
		conn.setReadTimeout(timeout * 1000);
		return conn.getInputStream();
	}

	static
	protected BitmapDrawable downloadIcon(Context context, URL iconURL, int timeout) {
		//get data
		InputStream is = null;
		BitmapDrawable result = null;
		try {
			is = getInputStreamFromURL(iconURL, timeout);
			result = new BitmapDrawable(context.getResources(), is);
		}
		catch(IOException e) {
			Log.i(TAG, "cannot load icon", e);
		}
		finally {
			if(null != is) {
				try{
					is.close();
				}
				catch(IOException e) {
					//nop..
				}
			}
		}
		return result;
	}

	@Override
	protected Void doInBackground(URL... urllist) {
		XmlPullParserFactory factory;
		try {
			factory = XmlPullParserFactory.newInstance();
		}
		catch (XmlPullParserException e1) {
			Log.i(TAG, "cannot get xml parser", e1);
			return null;
		}

		int podcastIndex = 0;
		for(int i = 0; i < urllist.length; i++) {
			URL url = urllist[i];
			if(isCancelled()){
				break;
			}
			//get index of podcastURL to filter
			while(! allPodcastURLs_[podcastIndex].equals(url.toString())){
				podcastIndex++;
			}
			Log.d(TAG, "get URL: " + podcastIndex + ": "+ url);
			InputStream is = null;
			try {
				is = getInputStreamFromURL(url, timeoutSec_);
				XmlPullParser parser = factory.newPullParser();
				//TODO: use reader or give correct encoding
				parser.setInput(is, "UTF-8");
				String title = null;
				String podcastURL = null;
				String pubdate = "";
				TagName tagName = TagName.NONE;
				int eventType;
				String link = null;
				while((eventType = parser.getEventType()) != XmlPullParser.END_DOCUMENT && !isCancelled()) {
					if(eventType == XmlPullParser.START_TAG) {
						String currentName = parser.getName();
						if("title".equalsIgnoreCase(currentName)) {
							tagName = TagName.TITLE;
						}
						else if("pubdate".equalsIgnoreCase(currentName)) {
							tagName = TagName.PUBDATE;
						}
						else if("link".equalsIgnoreCase(currentName)) {
							tagName = TagName.LINK;
						}
						else if("enclosure".equalsIgnoreCase(currentName)) {
							podcastURL = parser.getAttributeValue(null, "url");
						}
						else if("itunes:image".equalsIgnoreCase(currentName)) {
							if(null == iconURL_[podcastIndex]) {
								URL iconURL = new URL(parser.getAttributeValue(null, "href"));
								iconURL_[podcastIndex] = iconURL;
								if(showPodcastIcon_ && null == iconData_[podcastIndex]) {
									iconData_[podcastIndex] = downloadIcon(context_, iconURL, timeoutSec_);
								}
							}
						}
					}
					else if(eventType == XmlPullParser.TEXT) {
						switch(tagName) {
						case TITLE:
							title = parser.getText();
							break;
						case PUBDATE:
							//TODO: convert time zone
							pubdate = parser.getText();
							break;
						case LINK:
							link = parser.getText();
							break;
						default:
							break;
						}
					}
					else if(eventType == XmlPullParser.END_TAG) {
						String currentName = parser.getName();
						if("item".equalsIgnoreCase(currentName)) {
							if(podcastURL != null) {
								if(title == null) {
									title = podcastURL;
								}
								MusicInfo info = new MusicInfo(podcastURL, title, pubdate, link, podcastIndex);
								buffer_.add(info);
								if (buffer_.size() >= BUFFER_SIZE) {
									publish();
								}
							}
							podcastURL = null;
							title = null;
							link = null;
						}
						else {
							//always set to NONE, because there is no nested tag for now
							tagName = TagName.NONE;
						}
					}
					eventType = parser.next();
				}
				publish();
			}
			catch (IOException e) {
				Log.i(TAG, "IOException", e);
			}
			catch (XmlPullParserException e) {
				Log.i(TAG, "XmlPullParserException", e);
			}
			finally {
				if(null != is) {
					try {
						is.close();
					}
					catch (IOException e) {
						Log.i(TAG, "input stream cannot be close", e);
					}
				}
			}
		}
		return null;
	}
	
	private void publish() {
		if (buffer_.isEmpty()) {
			return;
		}
		publishProgress(buffer_.toArray(DUMMY_ARRAY));
		buffer_.clear();
	}
}
