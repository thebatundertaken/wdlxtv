package com.osdmod.service.listener;

public interface WdMediaServiceEventListener {
    void onVolumenChanged(int volumen);

    void onPlaybackPositionChanged(String trackDuration, String relTime);
    void onPlaybackSpeedChanged(int playSpeed);

    void onPlaymodeChanged(String playMode);

    void onPlaybackStatusChanged(String playbackStatus);

    void onMediaTitleReceived(String title);
    void onFail(Exception e);
}
