package com.osdmod.remote;

import android.util.Log;

import com.osdmod.model.WdDevice;
import com.osdmod.remote.telnet.AutomatedTelnetClient;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class WDTVLivePlus implements WdRemoteController {
    private static final String TAG = "WDTVLivePlus";
    private final String ip;
    private int countPing = 0;
    private AutomatedTelnetClient telnetClient;

    public WDTVLivePlus(String ip) {
        this.ip = ip;
    }

    @Override
    public int check() {
        try {
            telnetClient = new AutomatedTelnetClient(ip);
            return 1;
        } catch (Exception ignored) {
            return 0;
        }
    }

    @Override
    public int sendCommand(String command) {
        if (command.startsWith("p") || command.startsWith("E")) {
            return -1;
        }
        try {
            boolean connected = telnetClient.isConnected();
            if (!connected) {
                return 0;
            }

            if (countPing++ >= 3) {
                countPing = 0;
                try {
                    connected = InetAddress.getByName(ip).isReachable(500);
                } catch (IOException e) {
                    Log.w(TAG, e);
                    return 0;
                }
            }

            if (!connected) {
                return 0;
            }

            command = convertString(command);
            if (!command.equals("'")) {
                telnetClient.sendCommand(command);
            }

            return 1;
        } catch (Exception e) {
            Log.w(TAG, e);
            return 0;
        }
    }

    @Override
    public void sendText(String text) {
        //TODO SCF implementar
    }

    @Override
    public Map<String, Object> getInfo() {
        String[] resp = new GetInfoFromLx().getLiveConfig(ip, WdDevice.DEFAULT_USERNAME,
                WdDevice.DEFAULT_PASSWORD);
        Map<String, Object> response = new HashMap<>();
        response.put(INFO_REMOTECONTROL_AVAILABLE, Boolean.parseBoolean(resp[5]));
        response.put(INFO_KEYBOARD_AVAILABLE, Boolean.parseBoolean(resp[6]));
        response.put(INFO_WDLXTV_FIRMWARE, Boolean.parseBoolean(resp[0]));
        response.put(INFO_USERNAME, resp[1]);
        response.put(INFO_PASSWORD, resp[2]);
        response.put(INFO_CONNECTED, Boolean.parseBoolean(resp[4]));
        response.put(INFO_UPNP, Boolean.parseBoolean(resp[7]));

        return response;
    }

    @Override
    public String[][] getDeviceServices() {
        return null;
    }


    private String convertString(String command) {
        switch (command.charAt(0)) {
            case 'E':
            case 'p':
                return "'";
            case 'G':
                return "m";
            case 'H':
                return "w";
            case 'I':
                return "f";
            case 'X':
                return "j";
            case '[':
                return "v";
            case ']':
                return "n";
            case 'd':
                return "D";
            case 'l':
                return "L";
            case 'n':
                return "k";
            case 'r':
                return "R";
            case 't':
                return "s";
            case 'u':
                return "U";
            case 'w':
                return "x";
            case 'o':
            case 'T':
            default:
                return command;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        try {
            telnetClient.disconnect();
        } catch (Exception ignored) {
        }
    }
}
