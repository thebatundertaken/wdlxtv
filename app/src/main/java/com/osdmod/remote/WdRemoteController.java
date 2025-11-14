package com.osdmod.remote;

import java.util.Map;

public interface WdRemoteController {
    String TEXT_PREFIX = "t_";
    String SERVICE_PREFIX = "s_";

    String INFO_REMOTECONTROL_AVAILABLE = "remoteControlAvailable";
    String INFO_KEYBOARD_AVAILABLE = "keyboard";
    String INFO_WDLXTV_FIRMWARE = "wdlxtv";
    String INFO_USERNAME = "user";
    String INFO_PASSWORD = "password";
    String INFO_CONNECTED = "connected";
    String INFO_UPNP = "upnp";

    int check();

    int sendCommand(String command);

    void sendText(String text);
    void openService(String service);

    String[][] getDeviceServices();

    Map<String, Object> getInfo();

    String getWebUIUrl();
}
