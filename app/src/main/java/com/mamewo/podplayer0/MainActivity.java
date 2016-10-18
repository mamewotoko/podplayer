package com.mamewo.podplayer0;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;

public class MainActivity
    extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        SharedPreferences pref=
                PreferenceManager.getDefaultSharedPreferences(this);
        Resources res = getResources();
        boolean useExpandableList =
                pref.getBoolean("use_expandable_ui", 
                                res.getBoolean(R.bool.default_use_expandable_ui));
        Class<?> targetClass;
        if (useExpandableList) {
            targetClass = PodplayerExpActivity.class;
        }
        else {
            targetClass = PodplayerActivity.class;
        }
        Intent intent = new Intent(this, targetClass);
        startActivity(intent);
        finish();
    }
}
