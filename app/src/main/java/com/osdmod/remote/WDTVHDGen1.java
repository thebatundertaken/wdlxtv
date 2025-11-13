package com.osdmod.remote;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.util.HashMap;
import java.util.Map;

import ch.boye.httpclientandroidlib.client.utils.URLEncodedUtils;
import ch.boye.httpclientandroidlib.protocol.HTTP;

public class WDTVHDGen1 implements WdRemoteController {
    private final String ip;
    private final HttpClient httpclient = new DefaultHttpClient();

    public WDTVHDGen1(String ip) {
        this.ip = ip;
    }

    @Override
    public int check() {
        return 1;
    }

    @Override
    public int sendCommand(String command) {
        if (command.startsWith("s_") && command.startsWith("t_")) {
            return 1;
        }

        HttpPost httppost = new HttpPost("http://" + ip + "/webcontrol/remote/");
        try {
            StringEntity se = new StringEntity("button=&button=" + convertString(command),
                    HTTP.UTF_8);
            se.setContentType(URLEncodedUtils.CONTENT_TYPE);
            httppost.setHeader("Content-Type",
                    "application/x-www-form-urlencoded;charset=UTF-8");
            httppost.setEntity(se);
            if (httpclient.execute(httppost).getStatusLine().getStatusCode() > 201) {
                return 0;
            }
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public void sendText(String text) {
        //TODO SCF implementar
    }

    @Override
    public String[][] getDeviceServices() {
        return null;
    }

    @Override
    public Map<String, Object> getInfo() {
        String[] resp = new GetInfoFromLx().getGen12Config(ip);
        Map<String, Object> response = new HashMap<>();
        //modelName = resp[0];
        response.put(INFO_REMOTECONTROL_AVAILABLE, Boolean.parseBoolean(resp[6]));
        response.put(INFO_KEYBOARD_AVAILABLE, Boolean.parseBoolean(resp[7]));
        response.put(INFO_WDLXTV_FIRMWARE, Boolean.parseBoolean(resp[1]));
        response.put(INFO_USERNAME, resp[2]);
        response.put(INFO_PASSWORD, resp[3]);
        response.put(INFO_CONNECTED, Boolean.parseBoolean(resp[5]));
        response.put(INFO_UPNP, Boolean.parseBoolean(resp[8]));

        return response;
    }

    private String convertString(String command) {
        switch (command.charAt(0)) {
            case 'E':
                return "SEARCH";

            case 'G':
                return "OPTION";

            case 'H':
                return "REWIND";

            case 'I':
                return "FORWARD";

            case 'T':
                return "BACK";

            case 'X':
                return "EJECT";

            case '[':
                return "PREVIOUS";

            case ']':
                return "NEXT";

            case 'd':
                return "DOWN";

            case 'l':
                return "LEFT";

            case 'n':
                return "ENTER";

            case 'o':
                return "HOME";

            case 'p':
                return "PLAY";

            case 'r':
                return "RIGHT";

            case 't':
                return "STOP";

            case 'u':
                return "UP";

            case 'w':
                return "POWER";

            default:
                return command;
        }
    }

}
