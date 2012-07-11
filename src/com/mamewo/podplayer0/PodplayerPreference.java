package com.mamewo.podplayer0;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;

public class PodplayerPreference
	extends PreferenceActivity
	implements OnPreferenceClickListener
{
	static final
	private String GIT_URL = "https://github.com/mamewotoko/podplayer";
	
	private Preference version_;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preference);
		version_ = findPreference("version");
		PackageInfo pi;
		try {
			pi = getPackageManager().getPackageInfo(PodplayerPreference.class.getPackage().getName(), 0);
			version_.setSummary(pi.versionName);
		}
		catch (NameNotFoundException e) {
			version_.setSummary("unknown");
		}
		version_.setOnPreferenceClickListener(this);
	}

	@Override
	public boolean onPreferenceClick(Preference item) {
		if (item == version_) {
			Intent i =
				new Intent(Intent.ACTION_VIEW, Uri.parse(GIT_URL));
			startActivity(new Intent(i));
		}
		return false;
	}
}
