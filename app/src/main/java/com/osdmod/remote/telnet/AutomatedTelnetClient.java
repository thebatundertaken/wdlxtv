package com.osdmod.remote.telnet;

import android.util.Log;

import org.apache.commons.net.telnet.TelnetClient;

import java.io.PrintStream;

public class AutomatedTelnetClient {
    private static final String TAG = "AutomatedTelnetClient";
    private final PrintStream out;
    private final TelnetClient telnet;

    public AutomatedTelnetClient(String server) throws Exception {
        telnet = new TelnetClient();
        if (telnet.isConnected()) {
            telnet.disconnect();
        }
        telnet.connect(server, 30000);
        out = new PrintStream(telnet.getOutputStream());
    }

    public void write(String value) {
        try {
            out.println(value);
            out.flush();
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    public void sendCommand(String command) {
        try {
            write(command);
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    public void disconnect() {
        try {
            telnet.disconnect();
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    public boolean isConnected() {
        return telnet.isConnected();
    }
}
