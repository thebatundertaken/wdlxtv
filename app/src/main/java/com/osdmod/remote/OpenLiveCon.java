package com.osdmod.remote;

import org.apache.commons.net.tftp.TFTP;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import ch.boye.httpclientandroidlib.impl.auth.BasicScheme;
import ch.boye.httpclientandroidlib.impl.auth.DigestScheme;
import ch.boye.httpclientandroidlib.impl.client.BasicAuthCache;
import ch.boye.httpclientandroidlib.impl.client.BasicCredentialsProvider;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.params.BasicHttpParams;
import ch.boye.httpclientandroidlib.params.HttpConnectionParams;
import ch.boye.httpclientandroidlib.params.HttpParams;
import ch.boye.httpclientandroidlib.protocol.BasicHttpContext;

public class OpenLiveCon implements Runnable {
    private String ip;
    private String pass;
    private ResultIntSetter setter;
    private volatile String user;

    public OpenLiveCon(ResultIntSetter setter2, String ip2, String user2, String pass2) {
        this.setter = setter2;
        this.ip = ip2;
        this.user = user2;
        this.pass = pass2;
    }

    public void run() {
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
        credProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST, -1), new UsernamePasswordCredentials(user, pass));
        client.setCredentialsProvider(credProvider);
        AuthCache authCache = new BasicAuthCache();
        HttpHost host = new HttpHost(get.getURI().getHost(), get.getURI().getPort(), get.getURI().getScheme());
        authCache.put(host, new BasicScheme());
        authCache.put(host, new DigestScheme());
        new BasicHttpContext().setAttribute(ClientContext.AUTH_CACHE, authCache);
        try {
            StatusLine line = client.execute(get).getStatusLine();
            if (line.getStatusCode() <= 201) {
                setter.setResult(1);
            } else if (line.getStatusCode() == 401) {
                setter.setResult(2);
            } else {
                setter.setResult(0);
            }
        } catch (IOException e) {
            setter.setResult(0);
        }
    }
}
