package com.osdmod.service;

public interface WdMediaServiceCallback {
    void onCurrentTrackMetaData();
    void onPlaySpeed(int playSpeed);
    void onVolumenChanged(int volumen);
    void onPlaybackPositionChanged(String trackDuration, String relTime);
    void onPlaymodeChanged(String playMode);
    void onPlaybackStatusChanged(String playbackStatus);
    void onTransportState(String transportState);
    void onMediaTitleReceived(String title);
}
