package com.mamewo.podplayer0;

import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.os.Bundle;
import android.app.Dialog;
import android.app.AlertDialog;
import android.view.View;
import android.view.LayoutInflater;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import android.content.res.Resources;
import android.util.Log;

import static com.mamewo.podplayer0.Const.*;

public class SimpleDialogPreferenceFragment
    extends PreferenceDialogFragmentCompat
{
    static
    public SimpleDialogPreferenceFragment newInstance(String key){
        SimpleDialogPreferenceFragment fragment = new SimpleDialogPreferenceFragment();
        Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        //check ARG_KEY
        LayoutInflater inflater = getActivity().getLayoutInflater();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        String key = getArguments().getString(ARG_KEY);

        if("gesture_list".equals(key)){
            View view = inflater.inflate(R.layout.gesture_table, null, false);
            builder.setView(view);
            builder.setTitle(R.string.pref_gesture_list);
        }
        else if("license".equals(key)) {
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
            builder.setTitle(R.string.pref_license);
            builder.setMessage(licenseText.toString());
        }
        builder.setPositiveButton("OK", null);
        return builder.create();
    }
   
    @Override
    public void onDialogClosed(boolean positiveResult){
        
    }
}
