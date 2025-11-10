package com.osdmod.remote;

import ch.boye.httpclientandroidlib.client.params.AuthPolicy;
import ch.boye.httpclientandroidlib.client.utils.URLEncodedUtils;
import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.teleal.cling.model.message.header.EXTHeader;

public class PostLive implements Runnable {
    String contentType = "";
    DefaultHttpClient httpclient = new DefaultHttpClient();
    private volatile String ip;
    /* access modifiers changed from: private */
    public volatile String password;
    int port = 80;
    String postBody = "";
    String realmName = "WDLXTV-Webend";
    private ResultIntSetter setter;
    private volatile String str;
    /* access modifiers changed from: private */
    public volatile String user;

    public PostLive(ResultIntSetter setter2, String str2, String ip2, String user2, String password2) {
        this.str = str2;
        this.ip = ip2;
        this.user = user2;
        this.password = password2;
        this.setter = setter2;
    }

    private void convertString() {
        switch (this.str.charAt(0)) {
            case 'T':
                this.str = "T_back";
                return;
            case 't':
                this.str = "t_stop";
                return;
            default:
                return;
        }
    }

    public void run() {
        convertString();
        httpclient.getCredentialsProvider().setCredentials(new AuthScope(this.ip, 80, (String) null, AuthPolicy.DIGEST), new UsernamePasswordCredentials(this.user, this.password));
        Authenticator.setDefault(new Authenticator() {
            /* access modifiers changed from: protected */
            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(PostLive.this.user, PostLive.this.password.toCharArray());
            }
        });
        HttpPost httppost = new HttpPost("http://" + this.ip + "/addons/remote/");
        try {
            StringEntity se = new StringEntity("button=&button=" + this.str);
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
