package com.osdmod.remote;

import android.util.Log;

import com.osdmod.remote.telnet.AutomatedTelnetClient;

import org.apache.commons.net.tftp.TFTP;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

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
import ch.boye.httpclientandroidlib.protocol.HTTP;

public class GetInfoFromLx {
    private static final String TAG = "GetInfoFromLx";
    private AutomatedTelnetClient telnetClient;

    private String getGen1DeviceModel(String ip) {
        DefaultHttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet("http://" + ip + "/webcontrol/remote/");
        get.setHeader(HttpHeaders.ACCEPT, "application/xml");
        try {
            StatusLine line = client.execute(get).getStatusLine();
            if (line.getStatusCode() <= 201) {
                return "ok";
            }
            if (line.getStatusCode() == 401) {
                return "err_auth";
            }
            return "err_" + line.getStatusCode();
        } catch (IOException e) {
            if (e.getMessage() != null) {
                return "err_" + e.getMessage();
            }
            return "err_unknow";
        }
    }

    private String checkLxConnection(String ip, String user, String pass) {
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, TFTP.DEFAULT_TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, TFTP.DEFAULT_TIMEOUT);
        DefaultHttpClient client = new DefaultHttpClient(params);
        HttpGet get = new HttpGet("http://" + ip);
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
                return "ok";
            }
            if (line.getStatusCode() == 401) {
                return "err_auth";
            }
            return "err_" + line.getStatusCode();
        } catch (IOException e2) {
            if (e2.getMessage() != null) {
                return "err_" + e2.getMessage();
            }
            return "err_unknow";
        }
    }

    public String[] getLiveConfig(String ip, String user, String pass) {
        String[] s = new String[8];
        String response = checkLxConnection(ip, user, pass);
        if (response.equals("err_auth")) {
            s[0] = "true";
            s[1] = "";
            s[2] = "";
            s[3] = "false";
            s[4] = "true";
            s[5] = "false";
            s[6] = "false";
            s[7] = "true";
            return s;
        }

        if (response.equals("err_")) {
            s[0] = "true";
            s[1] = "";
            s[2] = "";
            s[3] = "false";
            s[4] = "true";
            s[5] = "false";
            s[6] = "false";
            s[7] = "true";
            return s;
        }
        if (!response.equals("ok")) {
            return tryTelnet(ip);
        }

        s[0] = "true";
        s[1] = "wdlxtv";
        s[2] = "wdlxtv";
        s[3] = "true";
        s[4] = "true";
        s[5] = "true";
        s[6] = "true";
        s[7] = "true";
        return s;
    }

    private String[] tryTelnet(String ip) {
        boolean remote = false;
        try {
            telnetClient = new AutomatedTelnetClient(ip);
            remote = true;
        } catch (Exception e) {
            Log.d(TAG, e.getMessage(), e);
        }

        String[] s = new String[8];
        if (remote) {
            s[0] = "false";
            s[1] = "";
            s[2] = "";
            s[3] = "false";
            s[4] = "true";
            s[5] = "true";
            s[6] = "false";
            s[7] = "true";
        } else {
            s[0] = "false";
            s[1] = "";
            s[2] = "";
            s[3] = "false";
            s[4] = "true";
            s[5] = "false";
            s[6] = "false";
            s[7] = "true";
        }
        try {
            if (telnetClient != null) {
                telnetClient.disconnect();
            }
        } catch (Exception e) {
            Log.d(TAG, e.getMessage(), e);
        }
        return s;
    }

    public String[] getGen12Config(String cip) {
        String[] s = new String[9];
        if (getGen1DeviceModel(cip).equals("ok")) {
            s[0] = "WDTV Gen1";
            s[1] = "true";
            s[2] = "";
            s[3] = "";
            s[4] = "true";
            s[5] = "true";
            s[6] = "true";
            s[7] = "true";
            s[8] = "true";
            return s;
        }

        String s2 = checkLxConnection(cip, "wdlxtv", "wdlxtv");
        if (s2.equals("ok")) {
            s[0] = "WDTV Gen2";
            s[1] = "true";
            s[2] = "wdlxtv";
            s[3] = "wdlxtv";
            s[4] = "true";
            s[5] = "true";
            s[6] = "true";
            s[7] = "true";
            s[8] = "false";
            return s;
        }

        if (s2.startsWith("err_auth")) {
            s[0] = "WDTV Gen2";
            s[1] = "true";
            s[2] = "";
            s[3] = "";
            s[4] = "false";
            s[5] = "true";
            s[6] = "false";
            s[7] = "false";
            s[8] = "false";
            return s;
        }

        s[0] = "ERROR GETTING DEVICE";
        s[1] = "false";
        s[2] = "";
        s[3] = "";
        s[4] = "false";
        s[5] = "true";
        s[6] = "false";
        s[7] = "false";
        s[8] = "false";
        return s;
    }

    public String[] getHubConfig(String ip) {
        return new String[]{
                Boolean.toString(getHubSupport(ip, "remote")),
                Boolean.toString(getHubSupport(ip, "keyboard"))
        };
    }

    private boolean getHubSupport(String ip, String mode) {
        String command = mode.equals("remote") ? "{\"remote\":\"\"}" : "{\"keyboard\":\"\"}";
        HttpClient httpclient = new org.apache.http.impl.client.DefaultHttpClient();
        try {
            HttpPost httppost = new HttpPost("http://" + ip + "/cgi-bin/toServerValue.cgi");
            StringEntity se = new StringEntity(command, HTTP.UTF_8);
            se.setContentType(URLEncodedUtils.CONTENT_TYPE);
            httppost.setHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            httppost.setEntity(se);
            return httpclient.execute(httppost).getStatusLine().getStatusCode() <= 201;
        } catch (IOException e) {
            Log.d(TAG, e.getMessage(), e);
            return false;
        }
    }
}
