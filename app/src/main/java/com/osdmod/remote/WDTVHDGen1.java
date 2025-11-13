package com.osdmod.remote;

import com.osdmod.model.WdDevice;

import org.apache.commons.net.tftp.TFTP;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
import ch.boye.httpclientandroidlib.params.BasicHttpParams;
import ch.boye.httpclientandroidlib.params.HttpConnectionParams;
import ch.boye.httpclientandroidlib.params.HttpParams;
import ch.boye.httpclientandroidlib.protocol.BasicHttpContext;
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
    public void openService(String service) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public int sendCommand(String command) {
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
        sendCommand(text);
    }

    @Override
    public String[][] getDeviceServices() {
        return null;
    }

    @Override
    public Map<String, Object> getInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put(INFO_WDLXTV_FIRMWARE, true);
        response.put(INFO_USERNAME, "");
        response.put(INFO_PASSWORD, "");
        response.put(INFO_REMOTECONTROL_AVAILABLE, true);
        response.put(INFO_KEYBOARD_AVAILABLE, true);
        response.put(INFO_CONNECTED, true);

        if (getGen1DeviceModel().equals("ok")) {
            //modelName = "WDTV Gen1";
            response.put(INFO_UPNP, true);
            return response;
        }

        response.put(INFO_UPNP, false);
        String connectionResponse = checkLxConnection();
        if (connectionResponse.equals("ok")) {
            //modelName = "WDTV Gen2";
            response.put(INFO_USERNAME, WdDevice.DEFAULT_USERNAME);
            response.put(INFO_PASSWORD, WdDevice.DEFAULT_PASSWORD);
            return response;
        }

        response.put(INFO_KEYBOARD_AVAILABLE, false);
        response.put(INFO_REMOTECONTROL_AVAILABLE, false);
        //modelName = connectionResponse.equals("err_auth") ? "WDTV Gen2" : "ERROR GETTING DEVICE";
        response.put(INFO_CONNECTED, connectionResponse.equals("err_auth"));
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

    private String checkLxConnection() {
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, TFTP.DEFAULT_TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, TFTP.DEFAULT_TIMEOUT);
        ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient client = new ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient(
                params);
        HttpGet get = new HttpGet("http://" + ip);
        get.setHeader(HttpHeaders.ACCEPT, "application/xml");
        List<String> authpref = new ArrayList<>();
        authpref.add(AuthPolicy.BASIC);
        authpref.add(AuthPolicy.DIGEST);
        client.getParams().setParameter(AuthPNames.PROXY_AUTH_PREF, authpref);
        CredentialsProvider credProvider = new BasicCredentialsProvider();
        credProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST, -1),
                new UsernamePasswordCredentials(WdDevice.DEFAULT_USERNAME,
                        WdDevice.DEFAULT_PASSWORD));
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

    private String getGen1DeviceModel() {
        ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient client = new ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient();
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

}
