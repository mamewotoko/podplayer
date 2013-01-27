package com.mamewo.podplayer0;

import java.io.Serializable;
import java.net.URL;

import android.graphics.drawable.BitmapDrawable;

public class PodcastInfo 
	implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 7613791894671950703L;
	public String title_;
	public URL url_;
	public URL iconURL_;
	public boolean enabled_;
	transient public BitmapDrawable icon_;

	public PodcastInfo(String title, URL url, URL iconURL, BitmapDrawable icon, boolean enabled) {
		title_ = title;
		url_ = url;
		iconURL_ = iconURL;
		icon_ = icon;
		enabled_ = enabled;
	}
}
