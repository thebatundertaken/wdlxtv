package com.osdmod.remote;

import java.util.Map;

public interface WdRemoteController {
    public static final String INFO_REMOTECONTROL_AVAILABLE = "remoteControlAvailable";
    public static final String INFO_KEYBOARD_AVAILABLE = "keyboard";
    public static final String INFO_WDLXTV_FIRMWARE = "wdlxtv";
    public static final String INFO_USERNAME = "user";
    public static final String INFO_PASSWORD = "password";
    public static final String INFO_CONNECTED = "connected";
    public static final String INFO_UPNP = "upnp";

    int check();

    int sendCommand(String command);

    void sendText(String text);

    String[][] getDeviceServices();
    Map<String, Object> getInfo();
}
