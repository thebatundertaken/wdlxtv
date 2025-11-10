package com.osdmod.remote;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import ch.boye.httpclientandroidlib.client.utils.URLEncodedUtils;

public class PostHub implements Runnable {
    DefaultHttpClient httpClient = new DefaultHttpClient();

    private String ip;
    private ResultIntSetter setter;
    private String command;

    public PostHub(ResultIntSetter resultintsetter, String ip, String cmd) {
        setter = resultintsetter;
        command = cmd;
        this.ip = ip;
    }

    private String convertCommand() {
        if (command.startsWith("check")) {
            return "";
        }

        if (command.startsWith("s_")) {
            return "{\"service\":\"" + command.substring(2) + "\"}";
        }

        if (command.startsWith("t_")) {
            return "{\"keyboard\":\"" + command.substring(2) + "\"}";
        }

        return "{\"remote\":\"" + command + "\"}";
    }

    public void run() {
        HttpPost httppost = new HttpPost("http://" + ip + "/cgi-bin/toServerValue.cgi");
        try {
            StringEntity se = new StringEntity(convertCommand(), "UTF-8");
            se.setContentType(URLEncodedUtils.CONTENT_TYPE);
            httppost.setHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            httppost.setEntity(se);
            if (httpClient.execute(httppost).getStatusLine().getStatusCode() > 201) {
                setter.setResult(0);
            } else {
                setter.setResult(1);
            }
        } catch (Exception e) {
            setter.setResult(0);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }
}
