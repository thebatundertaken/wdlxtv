package com.osdmod.remote;

import android.util.Log;

import com.osdmod.model.WdDevice;
import com.osdmod.remote.telnet.AutomatedTelnetClient;

import org.apache.commons.net.tftp.TFTP;

import java.io.IOException;
import java.net.InetAddress;
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
import ch.boye.httpclientandroidlib.impl.auth.BasicScheme;
import ch.boye.httpclientandroidlib.impl.auth.DigestScheme;
import ch.boye.httpclientandroidlib.impl.client.BasicAuthCache;
import ch.boye.httpclientandroidlib.impl.client.BasicCredentialsProvider;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.params.BasicHttpParams;
import ch.boye.httpclientandroidlib.params.HttpConnectionParams;
import ch.boye.httpclientandroidlib.params.HttpParams;
import ch.boye.httpclientandroidlib.protocol.BasicHttpContext;

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
        throw new RuntimeException("Not supported");
    }

    @Override
    public void openService(String service) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public Map<String, Object> getInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put(INFO_WDLXTV_FIRMWARE, true);
        response.put(INFO_CONNECTED, true);
        response.put(INFO_UPNP, true);

        String connectionResponse = checkLxConnection();
        if (connectionResponse.equals("ok")) {
            response.put(INFO_REMOTECONTROL_AVAILABLE, true);
            response.put(INFO_KEYBOARD_AVAILABLE, true);
            response.put(INFO_USERNAME, WdDevice.DEFAULT_USERNAME);
            response.put(INFO_PASSWORD, WdDevice.DEFAULT_PASSWORD);

            return response;
        }

        response.put(INFO_USERNAME, "");
        response.put(INFO_PASSWORD, "");
        response.put(INFO_KEYBOARD_AVAILABLE, false);

        if (connectionResponse.equals("err_")) {
            response.put(INFO_REMOTECONTROL_AVAILABLE, false);

            return response;
        }

        response.put(INFO_REMOTECONTROL_AVAILABLE, telnetClient.isConnected());
        response.put(INFO_WDLXTV_FIRMWARE, false);

        return response;
    }

    @Override
    public String[][] getDeviceServices() {
        return null;
    }

    @Override
    public String getWebUIUrl() {
        return  "http://" + ip;
    }

    private String checkLxConnection() {
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
        } catch (IOException e) {
            if (e.getMessage() != null) {
                return "err_" + e.getMessage();
            }
            return "err_unknow";
        }
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
