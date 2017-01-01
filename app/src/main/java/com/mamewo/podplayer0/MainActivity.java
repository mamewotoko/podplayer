package com.mamewo.podplayer0;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import static com.mamewo.podplayer0.Const.*;

public class MainActivity
    extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        SharedPreferences pref=
                PreferenceManager.getDefaultSharedPreferences(this);
        Resources res = getResources();
        int viewMode =
            Integer.valueOf(pref.getString("view_mode", String.valueOf(Const.VIEW_PULLTOREFRESH)));
        Class<?> targetClass;
        switch(viewMode){
        case VIEW_PULLTOREFRESH:
            targetClass = PodplayerActivity.class;
            break;
        case VIEW_EXP:
            targetClass = PodplayerExpActivity.class;
            break;
        case VIEW_CARD:
            targetClass = PodplayerCardActivity.class;
            break;
        default:
            targetClass = PodplayerActivity.class;
            break;
        }
        Intent intent = new Intent(this, targetClass);
        startActivity(intent);
        finish();
    }
}
