package com.mamewo.podplayer0.parser;

import java.net.URL;

public interface Podcast {
    static final
    public int UNKNOWN = 0;
    static final
    public int PUBLIC = 1;
    static final
    public int AUTH_REQUIRED_LOCKED = 2;
    static final
    public int AUTH_REQUIRED_UNLOCKED = 3;
    static final
    public int ERROR = 4;
    
    public void setTitle(String title);
    public void setURL(String url);
    public void setIconURL(String iconURL);
    public void setEnabled(boolean enabled);
    public void setUsername(String username);
    public void setPassword(String password);
    public void setStatus(int status);

    public URL getParsedURL();

    public String getTitle();
    public String getURL();
    public String getIconURL();
    public boolean getEnabled();
    public String getUsername();
    public String getPassword();
    public int getStatus();

    public String addUserInfo(String url);
}
