package com.mamewo.podplayer0;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

public class PodplayerPreference
	extends PreferenceActivity
	implements OnPreferenceClickListener,
	View.OnClickListener,
	OnSharedPreferenceChangeListener
{
	static final
	private String GIT_URL = "https://github.com/mamewotoko/podplayer";
	static final
	private String TAG = "podplayer";
	private View logo_;
	static final
	private int VERSION_DIALOG = 1;
	static final
	private int GESTURE_TABLE_DIALOG = 2;
	static final
	private int LICENSE_DIALOG = 3;
	
	private Preference version_;
	private Preference license_;
	private Preference gestureTable_;
	private Preference readTimeout_;
	private Preference scoreThreshold_;
	
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
		readTimeout_ = findPreference("read_timeout");
		scoreThreshold_ = findPreference("gesture_score_threshold");
		gestureTable_ = findPreference("gesture_list");
		gestureTable_.setOnPreferenceClickListener(this);
		version_.setOnPreferenceClickListener(this);
		license_ = findPreference("license");
		license_.setOnPreferenceClickListener(this);
		SharedPreferences pref =
				PreferenceManager.getDefaultSharedPreferences(this);
		pref.registerOnSharedPreferenceChangeListener(this);
		updateSummary(pref, "ALL");
	}
	
	@Override
	public void onDestroy() {
		SharedPreferences pref =
				PreferenceManager.getDefaultSharedPreferences(this);
		pref.unregisterOnSharedPreferenceChangeListener(this);
		super.onDestroy();
	}

	@Override
	public boolean onPreferenceClick(Preference item) {
		if (item == gestureTable_) {
			showDialog(GESTURE_TABLE_DIALOG);
			return true;
		}
		if (item == version_) {
			showDialog(VERSION_DIALOG);
			return true;
		}
		if(item == license_) {
			showDialog(LICENSE_DIALOG);
			return true;
		}
		return false;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;

		switch(id) {
		case GESTURE_TABLE_DIALOG:
			//TODO: add close button
			dialog = new Dialog(this);
			dialog.setContentView(R.layout.gesture_table);
			dialog.setTitle("Gesture table");
			dialog.show();
			break;
		case VERSION_DIALOG:
			//TODO: add close button
			dialog = new Dialog(this);
			dialog.setContentView(R.layout.version_dialog);
			dialog.setTitle("Google Play & github");
			logo_ = dialog.findViewById(R.id.github_logo);
			logo_.setOnClickListener(this);
			break;
		case LICENSE_DIALOG:
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
			dialog = new AlertDialog.Builder(this)
			.setMessage(licenseText.toString())
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					//nop
				}
			})
			.create();
			break;
		default:
			dialog = null;
			break;
		}
		return dialog;
	}
	
	@Override
	public void onClick(View view) {
		if (view == logo_) {
			Intent i =
					new Intent(Intent.ACTION_VIEW, Uri.parse(GIT_URL));
			startActivity(new Intent(i));
		}
	}

	public void updateSummary(SharedPreferences pref, String key) {
		boolean updateAll = "ALL".equals(key);
		if (updateAll || "read_timeout".equals(key)) {
			String strValue = pref.getString("read_timeout", "30");
			if ("0".equals(strValue)) {
				strValue = "None";
			}
			else {
				strValue = strValue + " sec";
			}
			readTimeout_.setSummary(strValue);
		}
		if (updateAll || "gesture_score_threshold".equals(key)) {
			double threshold = Double.valueOf(pref.getString("gesture_score_threshold", "3.0"));
			scoreThreshold_.setSummary(String.format("%.2f", threshold));
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
		updateSummary(pref, key);
	}
}