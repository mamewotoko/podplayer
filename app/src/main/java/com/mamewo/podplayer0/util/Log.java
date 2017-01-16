package com.mamewo.podplayer0.util;

public class Log {
    static
        final boolean LOG = true;

    public static void i(String tag, String string) {
        if (LOG) android.util.Log.i(tag, string);
    }

    public static void i(String tag, String string, Throwable t) {
        if (LOG) android.util.Log.i(tag, string, t);
    }
    
    public static void d(String tag, String string) {
        if (LOG) android.util.Log.d(tag, string);
    }
    public static void d(String tag, String string, Throwable t) {
        if (LOG) android.util.Log.d(tag, string, t);
    }
}
