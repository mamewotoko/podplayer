package com.mamewo.podplayer0;

import android.view.View;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

//dummy actionbar activity
//http://stackoverflow.com/questions/27091845/android-appcompat-dark-theme-settings-checkbox
public class PodplayerPreference
    extends AppCompatActivity
{
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preference_toolbar);

        Toolbar toolbar = (Toolbar) findViewById(R.id.preference_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.frame2, new PodplayerPreferenceFragment(), null).commit();
    }
}
