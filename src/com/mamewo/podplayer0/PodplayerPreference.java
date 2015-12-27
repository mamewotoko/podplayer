package com.mamewo.podplayer0;

import static com.mamewo.podplayer0.Const.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
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
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.os.Build;

public class PodplayerPreference
	extends PreferenceActivity
	implements OnPreferenceClickListener,
	View.OnClickListener,
	OnSharedPreferenceChangeListener
{
	private View logo_;
	static final
	private int VERSION_DIALOG = 1;
	static final
	private int GESTURE_TABLE_DIALOG = 2;
	static final
	private int LICENSE_DIALOG = 3;

	static final
	public String PREF_KEY_HIDE_LISTENED_EPISODE = "hide_listened_episode";
	
	private Preference podcastList_;
	private Preference version_;
	private Preference license_;
	private Preference gestureTable_;
	private ListPreference readTimeout_;
	private Preference scoreThreshold_;
	private Preference clearCache_;
	private Preference episodeLimit_;
	private SharedPreferences pref_;
	private ListPreference episodeOrder_;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(R.string.app_podcastpref_title);
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
		podcastList_ = findPreference("podcastlist");
		podcastList_.setOnPreferenceClickListener(this);
		readTimeout_ = (ListPreference)findPreference("read_timeout");
		scoreThreshold_ = findPreference("gesture_score_threshold");
		episodeLimit_ = findPreference("episode_limit");
		episodeOrder_ = (ListPreference)findPreference("episode_order");
		gestureTable_ = findPreference("gesture_list");
		gestureTable_.setOnPreferenceClickListener(this);
		version_.setOnPreferenceClickListener(this);
		license_ = findPreference("license");
		license_.setOnPreferenceClickListener(this);
		CheckBoxPreference cachePreference = (CheckBoxPreference)findPreference("use_response_cache");
		//Build.VERSION_CODES.HONEYCOMB_MR2;
		clearCache_ = findPreference("clear_response_cache");

		boolean cacheSupported = Build.VERSION.SDK_INT >= 13;
		cachePreference.setEnabled(cacheSupported);
		clearCache_.setEnabled(cacheSupported);
		if(!cacheSupported){
			cachePreference.setChecked(false);
		}
		else {
			clearCache_.setOnPreferenceClickListener(this);
		}
		pref_ = PreferenceManager.getDefaultSharedPreferences(this);
		pref_.registerOnSharedPreferenceChangeListener(this);
		updateSummary(pref_, "ALL");
	}
	
	@Override
	public void onDestroy() {
		pref_.unregisterOnSharedPreferenceChangeListener(this);
		super.onDestroy();
	}

	@Override
	public boolean onPreferenceClick(Preference item) {
		if (item == podcastList_) {
			Intent i = new Intent(this, PodcastListPreference.class);
			startActivity(i);
			return true;
		}
		if (item == clearCache_){
			//dummy field....
			boolean flag = pref_.getBoolean("clear_response_cache", true);
			pref_.edit()
				.putBoolean("clear_response_cache", !flag)
				.apply();
			return true;
		}
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
			dialog = new Dialog(this);
			dialog.setContentView(R.layout.gesture_table);
			dialog.setTitle(R.string.pref_gesture_list);
			dialog.show();
			break;
		case VERSION_DIALOG:
			dialog = new Dialog(this);
			dialog.setContentView(R.layout.version_dialog);
			dialog.setTitle(R.string.google_play_and_github);
			logo_ = dialog.findViewById(R.id.github_logo);
			logo_.setOnClickListener(this);
			break;
		case LICENSE_DIALOG:
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
		Resources res = getResources();
		
		if (updateAll || "read_timeout".equals(key)) {
			readTimeout_.setSummary(readTimeout_.getEntry());
		}
		if (updateAll || "gesture_score_threshold".equals(key)) {	
			double threshold = Double.valueOf(pref.getString("gesture_score_threshold", 
															res.getString(R.string.default_gesture_score_threshold)));
			scoreThreshold_.setSummary(String.format("%.2f", threshold));
		}
		if (updateAll || "episode_limit".equals(key)){
			int limit = Integer.valueOf(pref.getString("episode_limit", 
														res.getString(R.string.default_episode_limit)));
			String summary = getString(R.string.pref_episode_nolimit_summary);
			if (limit > 0){
				summary = MessageFormat.format(getString(R.string.pref_episode_limit_summary), limit);
			}
			episodeLimit_.setSummary(summary);
		}
		if(updateAll || "episode_order".equals(key)){
			int order = Integer.valueOf(pref.getString("episode_order", "0"));
			episodeOrder_.setSummary(res.getStringArray(R.array.episode_item_order_entries)[order]);
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
		updateSummary(pref, key);
	}
}
