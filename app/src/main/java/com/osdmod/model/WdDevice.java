package com.osdmod.model;

import androidx.annotation.Nullable;

import com.osdmod.remote.R;

import java.io.Serializable;
import java.security.SecureRandom;

public class WdDevice implements Comparable<WdDevice>, Serializable {
    public static final String DEFAULT_UUID = "a8af5494-0000-0000-0000-cec78a16fc05";
    public static final String DEFAULT_USERNAME = "wdlxtv";
    public static final String DEFAULT_PASSWORD = "wdlxtv";
    public static final int MODELID_STREAMING = 5;
    public static final int MODELID_HUB = 4;
    public static final int MODELID_PLUS = 3;
    public static final int MODELID_LIVE = 2;
    public static final int MODELID_GEN2 = 1;
    public static final int MODELID_GEN1 = 0;

    //TODO SCF Gallery rewrite (EditAction)
    private static final String[] galleryModelNames = {"WD TV® Live Streaming™", "WD TV® Live Hub™", "WD TV® Live Plus™", "WD TV® Live™", "WD TV® Gen2", "WD TV® Gen1"};
    private static final String[] modelNames = {"WD TV® Gen1", "WD TV® Gen2", "WD TV® Live™", "WD TV® Live Plus™", "WD TV® Live Hub™", "WD TV® Live Streaming™"};

    private static final int[] galleryModelIcons = {R.drawable.icon_streaming, R.drawable.icon_hub, R.drawable.icon_live, R.drawable.icon_live, R.drawable.icon_gen1, R.drawable.icon_gen1};
    private static final int[] modelSmallIcons = {R.drawable.micon_streaming, R.drawable.micon_hub, R.drawable.micon_live, R.drawable.micon_live, R.drawable.micon_gen1, R.drawable.micon_gen1};

    private final String modelName;
    private String friendlyName;
    private String ip;
    private String uuid;
    private boolean wDlxTVFirmware;
    private boolean upnp;
    private boolean keyboardAvailable;
    private boolean remoteControlAvailable;
    private boolean connected;
    private String username;
    private String password;
    private int deviceId;
    public static String CMD_BACK = "T";
    public static String CMD_OK = "n";
    public static String CMD_OPTION = "G";
    public static String CMD_DOWN = "d";
    public static String CMD_UP = "u";
    public static String CMD_PREV = "[";
    public static String CMD_NEXT = "]";
    public static String CMD_HOME = "o";
    public static String CMD_RIGHT = "r";
    public static String CMD_LEFT = "l";
    public static String CMD_REWIND = "H";
    public static String CMD_FASTFORWARD = "I";
    public static String CMD_PLAY = "p";
    public static String CMD_STOP = "t";
    public static String CMD_POWER = "w";
    public static String CMD_EJECT = "X";
    public static String CMD_SEARCH = "E";
    public static String CMD_PGB = "U";
    public static String CMD_PGN = "D";
    public static String CMD_CONFIG = "s";
    public static String CMD_AUDIO = ",";
    public static String CMD_MUTE = "M";
    public static String CMD_SUBS = "\\\\";
    public static String CMD_BTN_A = "x";
    public static String CMD_BTN_B = "y";
    public static String CMD_BTN_C = "z";
    public static String CMD_BTN_D = "A";

    public WdDevice(String modelName, String friendlyName, String ip, String uuid) {
        this(modelName, friendlyName, ip, uuid, false, null, null,
                false, false, false, false);
    }

    public WdDevice(String modelName, String friendlyName, String ip, String uuid,
                    boolean wDlxTVFirmware, String username, String password,
                    boolean remoteControlAvailable, boolean keyboard, boolean upnp, boolean connected) {
        this(modelName, friendlyName, ip, uuid, wDlxTVFirmware, username, password,
                remoteControlAvailable,
                keyboard, upnp, connected, generateDeviceId());
    }

    public WdDevice(String modelName, String friendlyName, String ip, String uuid,
                    boolean wDlxTVFirmware, String username, String password,
                    boolean remoteControlAvailable, boolean keyboard, boolean upnp, boolean connected,
                    int deviceId) {
        this.modelName = modelName;
        this.friendlyName = friendlyName;
        this.ip = ip;
        this.uuid = uuid;
        this.wDlxTVFirmware = wDlxTVFirmware;
        this.username = username;
        this.password = password;
        this.remoteControlAvailable = remoteControlAvailable;
        this.keyboardAvailable = keyboard;
        this.upnp = upnp;
        this.connected = connected;
        this.deviceId = deviceId;
    }

    public static int generateDeviceId() {
        return Math.abs(new SecureRandom().nextInt());
    }

    public static int getGalleryAvailableWdDevices() {
        return galleryModelNames.length;
    }

    public static int getGalleryModelIcon(int galleryPosition) {
        //TODO SCF galeria y pos no me gusta
        return galleryModelIcons[galleryPosition];
    }

    public static String getGalleryModelName(int galleryPosition) {
        //TODO SCF galeria y pos no me gusta
        return galleryModelNames[galleryPosition];
    }

    public static String gentModelStringFromModelID(int modelID) {
        return modelNames[modelID];
    }

    public static int getModelIDFromString(String model) {
        //TODO SCF unificar con getDeviceDrawable para utilizar el mismo modelID
        String lower = model.toLowerCase();
        if (lower.contains("streaming")) {
            return MODELID_STREAMING;
        }
        if (lower.contains("hub")) {
            return MODELID_HUB;
        }
        if (lower.contains("plus")) {
            return MODELID_PLUS;
        }
        if (lower.contains("live")) {
            return MODELID_LIVE;
        }
        if (lower.contains("gen2")) {
            return MODELID_GEN2;
        }

        return MODELID_GEN1;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getModelName() {
        return modelName;
    }

    public boolean iswDlxTVFirmware() {
        return wDlxTVFirmware;
    }

    public void setwDlxTVFirmware(boolean wDlxTVFirmware) {
        this.wDlxTVFirmware = wDlxTVFirmware;
    }

    public boolean isUpnp() {
        return upnp;
    }

    public void setUpnp(boolean upnp) {
        this.upnp = upnp;
    }

    public int getDeviceGalleryIcon() {
        //TODO SCF unificar con getDeviceDrawable para utilizar el mismo modelID
        String lower = modelName.toLowerCase();
        if (lower.contains("streaming")) {
            return 0;
        }

        if (lower.contains("hub")) {
            return 1;
        }
        if (lower.contains("plus")) {
            return 2;
        }
        if (lower.contains("live")) {
            return 3;
        }

        if (lower.contains("gen2")) {
            return 4;
        }

        if (lower.contains("gen1")) {
            return 5;
        }

        return -1;
    }

    public int getDeviceDrawable(boolean bigSize) {
        //TODO SCF unificar con getDeviceDrawable para utilizar el mismo modelID
        String lower = modelName.toLowerCase();
        if (lower.contains("streaming")) {
            return bigSize ? galleryModelIcons[0] : modelSmallIcons[0];
        }

        if (lower.contains("hub")) {
            return bigSize ? galleryModelIcons[1] : modelSmallIcons[1];
        }

        if (lower.contains("plus")) {
            return bigSize ? galleryModelIcons[2] : modelSmallIcons[2];
        }

        if (lower.contains("live")) {
            return bigSize ? galleryModelIcons[3] : modelSmallIcons[3];
        }

        if (lower.contains("gen2")) {
            return bigSize ? galleryModelIcons[4] : modelSmallIcons[4];
        }

        return bigSize ? galleryModelIcons[5] : modelSmallIcons[5];
    }

    public int getModelID() {
        return getModelIDFromString(modelName);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isKeyboardAvailable() {
        return keyboardAvailable;
    }

    public void setKeyboardAvailable(boolean keyboardAvailable) {
        this.keyboardAvailable = keyboardAvailable;
    }

    public boolean isRemoteControlAvailable() {
        return remoteControlAvailable;
    }

    public void setRemoteControlAvailable(boolean remote) {
        this.remoteControlAvailable = remote;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    @Override
    public int compareTo(WdDevice o) {
        return getDeviceId() - o.getDeviceId();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof WdDevice)) {
            return false;
        }

        return ((WdDevice) obj).getDeviceId() == this.getDeviceId();
    }
}
