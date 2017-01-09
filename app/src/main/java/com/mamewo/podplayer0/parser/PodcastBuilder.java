package com.mamewo.podplayer0.parser;

public interface PodcastBuilder<T> {
    public void setTitle(String title);
    public void setURL(String url);
    public void setIconURL(String iconURL);
    public void setEnabled(boolean enabled);
    public void setUsername(String username);
    public void setPassword(String password);
    public void setStatus(int status);
    public T build();
}
