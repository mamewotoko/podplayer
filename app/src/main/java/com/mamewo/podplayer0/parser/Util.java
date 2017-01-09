package com.mamewo.podplayer0.parser;

import android.util.Base64;

public class Util {
    static
    public String makeHTTPAuthorizationHeader(String username, String password){
        if(null == username || null == password){
            return null;
        }
        String data = username+":"+password;
        String encoded = Base64.encodeToString(data.getBytes(), Base64.NO_WRAP);
        return "Basic "+encoded;
    }
}
