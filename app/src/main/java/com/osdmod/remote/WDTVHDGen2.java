package com.osdmod.remote;

import org.apache.commons.net.tftp.TFTP;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ch.boye.httpclientandroidlib.HttpHeaders;
import ch.boye.httpclientandroidlib.HttpHost;
import ch.boye.httpclientandroidlib.StatusLine;
import ch.boye.httpclientandroidlib.auth.AuthScope;
import ch.boye.httpclientandroidlib.auth.UsernamePasswordCredentials;
import ch.boye.httpclientandroidlib.auth.params.AuthPNames;
import ch.boye.httpclientandroidlib.client.AuthCache;
import ch.boye.httpclientandroidlib.client.CredentialsProvider;
import ch.boye.httpclientandroidlib.client.methods.HttpGet;
import ch.boye.httpclientandroidlib.client.params.AuthPolicy;
import ch.boye.httpclientandroidlib.client.protocol.ClientContext;
import ch.boye.httpclientandroidlib.client.utils.URLEncodedUtils;
import ch.boye.httpclientandroidlib.impl.auth.BasicScheme;
import ch.boye.httpclientandroidlib.impl.auth.DigestScheme;
import ch.boye.httpclientandroidlib.impl.client.BasicAuthCache;
import ch.boye.httpclientandroidlib.impl.client.BasicCredentialsProvider;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.params.BasicHttpParams;
import ch.boye.httpclientandroidlib.params.HttpConnectionParams;
import ch.boye.httpclientandroidlib.params.HttpParams;
import ch.boye.httpclientandroidlib.protocol.BasicHttpContext;

public class WDTVHDGen2 implements WdRemoteController {
    private final org.apache.http.impl.client.DefaultHttpClient httpclient = new org.apache.http.impl.client.DefaultHttpClient();
    private final String ip;
    private final String user;
    private final String pass;

    public WDTVHDGen2(String ip, String user, String pass) {
        this.ip = ip;
        this.user = user;
        this.pass = pass;
    }

    @Override
    public int check() {
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, TFTP.DEFAULT_TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, TFTP.DEFAULT_TIMEOUT);
        DefaultHttpClient client = new DefaultHttpClient(params);
        HttpGet get = new HttpGet("http://" + ip + "/addons/remote/");
        get.setHeader(HttpHeaders.ACCEPT, "application/xml");
        List<String> authpref = new ArrayList<>();
        authpref.add(AuthPolicy.BASIC);
        authpref.add(AuthPolicy.DIGEST);
        client.getParams().setParameter(AuthPNames.PROXY_AUTH_PREF, authpref);
        CredentialsProvider credProvider = new BasicCredentialsProvider();
        credProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST, -1),
                new UsernamePasswordCredentials(user, pass));
        client.setCredentialsProvider(credProvider);
        AuthCache authCache = new BasicAuthCache();
        HttpHost host = new HttpHost(get.getURI().getHost(), get.getURI().getPort(),
                get.getURI().getScheme());
        authCache.put(host, new BasicScheme());
        authCache.put(host, new DigestScheme());
        new BasicHttpContext().setAttribute(ClientContext.AUTH_CACHE, authCache);
        try {
            StatusLine line = client.execute(get).getStatusLine();
            if (line.getStatusCode() <= 201) {
                return 1;
            }
            if (line.getStatusCode() == 401) {
                return 2;
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private String convertString(String command) {
        switch (command.charAt(0)) {
            case 'T':
                return "T_back";

            case 't':
                return "t_stop";

            default:
                return command;
        }
    }

    @Override
    public void openService(String service) {
        sendCommand(SERVICE_PREFIX + service);
    }

    @Override
    public int sendCommand(String command) {
        httpclient.getCredentialsProvider()
                .setCredentials(new org.apache.http.auth.AuthScope(ip, 80, null, AuthPolicy.DIGEST),
                        new org.apache.http.auth.UsernamePasswordCredentials(user, pass));
        Authenticator.setDefault(new Authenticator() {
            /* access modifiers changed from: protected */
            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, pass.toCharArray());
            }
        });
        HttpPost httppost = new HttpPost("http://" + ip + "/addons/remote/");
        try {
            StringEntity se = new StringEntity("button=&button=" + convertString(command));
            se.setContentType(URLEncodedUtils.CONTENT_TYPE);
            httppost.setHeader("Content-Type", URLEncodedUtils.CONTENT_TYPE);
            httppost.setEntity(se);
            if (httpclient.execute(httppost).getStatusLine().getStatusCode() > 201) {
                return 0;
            }
            return 1;
        } catch (IOException e) {
            return 0;
        }
        finally {
            httpclient.getConnectionManager().shutdown();
        }
    }

    @Override
    public void sendText(String text) {
        sendCommand(text);
    }

    @Override
    public Map<String, Object> getInfo() {
        throw new RuntimeException("Not supported, use WDTVHDGen1");
    }

    @Override
    public String[][] getDeviceServices() {
        return null;
    }
}
