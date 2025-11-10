package com.osdmod.remote;

import org.apache.commons.net.nntp.NNTP;
import org.apache.commons.net.tftp.TFTP;

public class SendLiveOrig implements Runnable {
    private volatile String str;

    public SendLiveOrig(String str2) {
        this.str = str2;
    }

    private void convertString() {
        switch (this.str.charAt(0)) {
            case TFTP.DEFAULT_PORT /*69*/:
                this.str = "'";
                return;
            case 'G':
                this.str = "m";
                return;
            case 'H':
                this.str = "w";
                return;
            case 'I':
                this.str = "f";
                return;
            case 'T':
                return;
            case 'X':
                this.str = "j";
                return;
            case '[':
                this.str = "v";
                return;
            case ']':
                this.str = "n";
                return;
            case 'd':
                this.str = "D";
                return;
            case 'l':
                this.str = "L";
                return;
            case 'n':
                this.str = "k";
                return;
            case 'o':
                return;
            case 'p':
                this.str = "'";
                return;
            case 'r':
                this.str = "R";
                return;
            case 't':
                this.str = "s";
                return;
            case 'u':
                this.str = "U";
                return;
            case NNTP.DEFAULT_PORT /*119*/:
                this.str = "x";
                return;
            default:
                return;
        }
    }

    public void run() {
        convertString();
        if (!this.str.equals("'")) {
            try {
                AutomatedTelnetClient.sendCommand(this.str);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
