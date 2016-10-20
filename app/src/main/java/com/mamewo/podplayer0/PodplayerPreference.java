package com.mamewo.podplayer0;

import android.widget.FrameLayout;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
//import android.support.v7.widget.Toolbar;
//dummy actionbar activity
//http://stackoverflow.com/questions/27091845/android-appcompat-dark-theme-settings-checkbox
public class PodplayerPreference
    extends AppCompatActivity
{
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        //FrameLayout frame = new FrameLayout(this);
        //frame.setId(R.id.frame);
        setContentView(R.layout.preference_toolbar);
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.frame2, new PodplayerPreferenceFragment(), null).commit();
    }
}
