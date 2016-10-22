package com.mamewo.podplayer0;

import android.view.View;
import android.support.v7.preference.DialogPreference;
import android.util.AttributeSet;
import android.content.Context;
import android.support.v7.preference.DialogPreference;
import android.os.Bundle;
import android.app.Dialog;

public class SimpleDialogPreference
    extends DialogPreference
{
    private Dialog dialog_;
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
