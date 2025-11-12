package com.osdmod.remote;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;

import ch.boye.httpclientandroidlib.client.params.AuthPolicy;
import ch.boye.httpclientandroidlib.client.utils.URLEncodedUtils;

public class PostLive implements Runnable {
    private final String ip;
    private final String password;
    private final ResultIntSetter setter;
    private final String user;
    private DefaultHttpClient httpclient = new DefaultHttpClient();
    private String str;

    public PostLive(ResultIntSetter setter2, String str2, String ip2, String user2,
                    String password2) {
        this.str = str2;
        this.ip = ip2;
        this.user = user2;
        this.password = password2;
        this.setter = setter2;
    }

    private void convertString() {
        switch (str.charAt(0)) {
            case 'T':
                str = "T_back";
                return;

            case 't':
                str = "t_stop";
                return;

            default:
                return;
        }
    }

    public void run() {
        convertString();
        httpclient.getCredentialsProvider()
                .setCredentials(new AuthScope(ip, 80, null, AuthPolicy.DIGEST),
                        new UsernamePasswordCredentials(user, password));
        Authenticator.setDefault(new Authenticator() {
            /* access modifiers changed from: protected */
            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password.toCharArray());
            }
        });
        HttpPost httppost = new HttpPost("http://" + ip + "/addons/remote/");
        try {
            StringEntity se = new StringEntity("button=&button=" + str);
            se.setContentType(URLEncodedUtils.CONTENT_TYPE);
            httppost.setHeader("Content-Type", URLEncodedUtils.CONTENT_TYPE);
            httppost.setEntity(se);
            if (httpclient.execute(httppost).getStatusLine().getStatusCode() > 201) {
                setter.setResult(0);
            } else {
                setter.setResult(1);
            }
        } catch (IOException e) {
            setter.setResult(0);
        }
        httpclient.getConnectionManager().shutdown();
    }
}
