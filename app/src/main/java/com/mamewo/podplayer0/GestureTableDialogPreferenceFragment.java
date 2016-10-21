package com.mamewo.podplayer0;

import android.support.v7.preference.PreferenceDialogFragmentCompat;
import android.os.Bundle;

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
    public void onDialogClosed(boolean positiveResult){
    }

}
