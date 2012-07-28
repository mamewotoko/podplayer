package com.mamewo.podplayer0;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;

public class PodplayerPreference
	extends PreferenceActivity
	implements OnPreferenceClickListener
{
	static final
	private String GIT_URL = "https://github.com/mamewotoko/podplayer";
	static final
	private String TAG = "podplayer";
	
	
	private Preference version_;
	private Preference license_;

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
		license_ = findPreference("license");
		license_.setOnPreferenceClickListener(this);
	}

	@Override
	public boolean onPreferenceClick(Preference item) {
		if (item == version_) {
			Intent i =
				new Intent(Intent.ACTION_VIEW, Uri.parse(GIT_URL));
			startActivity(new Intent(i));
		}
		else if(item == license_) {
			//TODO: Localize?
			StringBuffer licenseText = new StringBuffer();
			Resources res = getResources();
			InputStream is = res.openRawResource(R.raw.apache20);
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line;
			try {
				while((line = br.readLine()) != null) {
					licenseText.append(line+"\n");
				}
			}
			catch(IOException e) {
				Log.d(TAG, "cannot read license", e);
			}
			finally {
				try{
					br.close();
					is.close();
				}
				catch(IOException e) {
					
				}
			}
			new AlertDialog.Builder(this)
			.setMessage(licenseText.toString())
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					//nop
				}
			})
			.create()
			.show();
		}
		return false;
	}
}
