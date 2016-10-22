package com.mamewo.podplayer0;

import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.os.Bundle;
import android.app.Dialog;
import android.app.AlertDialog;
import android.view.View;
import android.view.LayoutInflater;

public class GestureTableDialogPreferenceFragment
    extends PreferenceDialogFragmentCompat
{
    static
    public GestureTableDialogPreferenceFragment newInstance(String key){
        GestureTableDialogPreferenceFragment fragment = new GestureTableDialogPreferenceFragment();
        Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        //check ARG_KEY
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.gesture_table, null, false);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.pref_gesture_list);
        builder.setView(view);
        builder.setPositiveButton("OK", null);        
        return builder.create();
    }
   
    @Override
    public void onDialogClosed(boolean positiveResult){
        //default
    }
}
