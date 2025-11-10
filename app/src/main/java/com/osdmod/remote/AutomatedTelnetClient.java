package com.osdmod.remote;

import android.util.Log;

import org.apache.commons.net.telnet.TelnetClient;

import java.io.PrintStream;

public class AutomatedTelnetClient {
    private static final String TAG = "AutomatedTelnetClient";
    private static PrintStream out;
    private static final TelnetClient telnet = new TelnetClient();

    //TODO SCF transformar a singleton
    public AutomatedTelnetClient(String server) throws Exception {
        if (telnet.isConnected()) {
            telnet.disconnect();
        }
        telnet.connect(server, 30000);
        out = new PrintStream(telnet.getOutputStream());
    }

    public static void write(String value) {
        try {
            out.println(value);
            out.flush();
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    public static void sendCommand(String command) {
        try {
            write(command);
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    public static void disconnect() {
        try {
            telnet.disconnect();
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    public static boolean isConnected() {
        return telnet.isConnected();
    }
}
