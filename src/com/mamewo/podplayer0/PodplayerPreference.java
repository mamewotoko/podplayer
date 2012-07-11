package com.mamewo.podplayer0;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class PodplayerPreference
	extends PreferenceActivity
{
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preference);
	}
}
