package com.mamewo.podplayer0;

import java.io.Serializable;
import java.net.URL;
import android.graphics.drawable.Drawable;

public class PodcastInfo 
	implements Serializable
{
	public String title_;
	public URL url_;
	public boolean enabled_;
	transient public Drawable icon_;

	public PodcastInfo(String title, URL url, Drawable icon, boolean enabled) {
		title_ = title;
		url_ = url;
		icon_ = icon;
		enabled_ = enabled;
	}
}
