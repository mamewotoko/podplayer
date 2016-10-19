package com.mamewo.podplayer0;

import android.widget.FrameLayout;
import android.os.Bundle;
//TODO: use AppCompatActivity
//import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;

//dummy actionbar activity
//http://stackoverflow.com/questions/27091845/android-appcompat-dark-theme-settings-checkbox

public class PodplayerPreference
    extends AppCompatActivity
{
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        FrameLayout frame = new FrameLayout(this);
        setContentView(frame);
        frame.setId(R.id.frame);
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.frame, new PodplayerPreferenceFragment(), null).commit();
    }

}
