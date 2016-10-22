package com.mamewo.podplayer0;

import android.view.View;
import android.support.v7.preference.DialogPreference;
import android.util.AttributeSet;
import android.content.Context;
import android.support.v7.preference.DialogPreference;
import android.os.Bundle;
import android.app.Dialog;

public class GestureTableDialogPreference
    extends DialogPreference
{
    private Dialog dialog_;
    public GestureTableDialogPreference(Context context){
        super(context);

    }

    public GestureTableDialogPreference(Context context, AttributeSet attrs){
        super(context, attrs);
    }
    
    public GestureTableDialogPreference(Context context, AttributeSet attrs, int defStyleAttr){
        super(context, attrs, defStyleAttr);
    }
    
    // @Override
    // public void onCreate(Bundle savedInstanceState){
    //      super.onCreate(savedInstanceState);
    // }

    // @Override
    // public View onCreateDialogView(){
    //     return View.inflate(getContext(), R.layout.gesture_table, null);
    // }
    
    // @Override
    // protected void onDialogClosed(boolean positiveResult){
    //     //
    // }
}
