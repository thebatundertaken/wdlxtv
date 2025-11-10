package com.osdmod.remote;

import ch.boye.httpclientandroidlib.client.utils.URLEncodedUtils;
import ch.boye.httpclientandroidlib.protocol.HTTP;
import java.io.IOException;
import org.apache.commons.net.nntp.NNTP;
import org.apache.commons.net.tftp.TFTP;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

public class PostGen1 implements Runnable {
    private volatile String ip;
    private ResultIntSetter setter;
    private volatile String str;

    public PostGen1(ResultIntSetter setter2, String ip2, String str2) {
        this.str = str2;
        this.ip = ip2;
        this.setter = setter2;
    }

    private void convertString() {
        switch (this.str.charAt(0)) {
            case TFTP.DEFAULT_PORT /*69*/:
                this.str = "SEARCH";
                return;
            case 'G':
                this.str = "OPTION";
                return;
            case 'H':
                this.str = "REWIND";
                return;
            case 'I':
                this.str = "FORWARD";
                return;
            case 'T':
                this.str = "BACK";
                return;
            case 'X':
                this.str = "EJECT";
                return;
            case '[':
                this.str = "PREVIOUS";
                return;
            case ']':
                this.str = "NEXT";
                return;
            case 'd':
                this.str = "DOWN";
                return;
            case 'l':
                this.str = "LEFT";
                return;
            case 'n':
                this.str = "ENTER";
                return;
            case 'o':
                this.str = "HOME";
                return;
            case 'p':
                this.str = "PLAY";
                return;
            case 'r':
                this.str = "RIGHT";
                return;
            case 't':
                this.str = "STOP";
                return;
            case 'u':
                this.str = "UP";
                return;
            case NNTP.DEFAULT_PORT /*119*/:
                this.str = "POWER";
                return;
            default:
                return;
        }
    }

    public void run() {
        if (!this.str.startsWith("s_") && !this.str.startsWith("t_")) {
            convertString();
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://" + this.ip + "/webcontrol/remote/");
            try {
                StringEntity se = new StringEntity("button=&button=" + this.str, HTTP.UTF_8);
                se.setContentType(URLEncodedUtils.CONTENT_TYPE);
                httppost.setHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
                httppost.setEntity(se);
                if (httpclient.execute(httppost).getStatusLine().getStatusCode() > 201) {
                    this.setter.setResult(0);
                } else {
                    this.setter.setResult(1);
                }
            } catch (ClientProtocolException e) {
                this.setter.setResult(0);
            } catch (IOException e2) {
                this.setter.setResult(0);
            }
        }
    }
}
