package com.mamewo.podplayer0;

import android.util.AttributeSet;
import android.content.Context;
import android.support.v7.preference.DialogPreference;
import android.app.Dialog;

public class SimpleDialogPreference
    extends DialogPreference
{
    public SimpleDialogPreference(Context context){
        super(context);
    }

    public SimpleDialogPreference(Context context, AttributeSet attrs){
        super(context, attrs);
    }
    
    public SimpleDialogPreference(Context context, AttributeSet attrs, int defStyleAttr){
        super(context, attrs, defStyleAttr);
    }
}
