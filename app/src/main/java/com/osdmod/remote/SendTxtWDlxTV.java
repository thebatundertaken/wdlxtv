package com.osdmod.remote;

import android.util.Log;

import com.osdmod.model.WdDevice;

import java.lang.reflect.Array;

public class SendTxtWDlxTV implements Runnable {
    private static final String TAG = "SendTxtWDlxTV";
    private final int id;
    private final String ip;
    private final String password;
    private final int[] pos_act;
    private final ResultIntSetter setter = result -> {
    };
    private final String user;
    private boolean running;
    private String text;

    public SendTxtWDlxTV(String ip2, int id2, String text2, String user2, String password2) {
        int[] iArr = new int[2];
        iArr[0] = -1;
        this.pos_act = iArr;
        this.text = text2;
        this.ip = ip2;
        this.id = id2;
        this.user = user2;
        this.password = password2;
    }

    public void run() {
        text = replaceString(text);
        running = true;
        for (int i = 0; i < text.length() && running; i++) {
            char caract = text.charAt(i);
            if (Character.isDigit(caract)) {
                action_digit(caract);
            } else if (Character.isLowerCase(caract)) {
                action_lower(caract);
            } else if (Character.isUpperCase(caract)) {
                action_uper(caract);
            } else if (Character.isWhitespace(caract)) {
                action_space();
            }
        }
        if (running) {
            motionAct(9, 1);
            sendChar("d");
            sendChar("d");
            running = false;
        }
    }

    public void empieza() {
        this.running = true;
    }

    public void stop() {
        this.running = false;
    }

    public String replaceString(String str) {
        for (int i = 0; i < "ÀÁÂÃÄÅàáâãäåÒÓÔÕÖØòóôõöøÈÉÊËéèêëÇçÌÍÎÏìíîïÙÚÛÜùúûüÿÑñ".length(); i++) {
            str = str.replace("ÀÁÂÃÄÅàáâãäåÒÓÔÕÖØòóôõöøÈÉÊËéèêëÇçÌÍÎÏìíîïÙÚÛÜùúûüÿÑñ".charAt(i),
                    "AAAAAAaaaaaaOOOOOOooooooEEEEeeeeCcIIIIiiiiUUUUuuuuyNn".charAt(i));
        }
        return str;
    }

    private void motionAct(int pos_x, int pos_y) {
        int x = Array.getInt(this.pos_act, 0) - pos_x;
        int y = Array.getInt(this.pos_act, 1) - pos_y;
        String charx = "";
        String chary = "";
        if (x != 0) {
            if (x < 0) {
                charx = "r";
            }
            if (x > 0) {
                charx = "l";
            }
            int x2 = Math.abs(x);
            for (int i = 0; i < x2; i++) {
                sendChar(charx);
            }
        }
        if (y != 0) {
            if (y > 0) {
                chary = "d";
            }
            if (y < 0) {
                chary = "d";
            }
            int y2 = Math.abs(y);
            for (int i2 = 0; i2 < y2; i2++) {
                sendChar(chary);
            }
        }
        Array.setInt(this.pos_act, 0, pos_x);
        Array.setInt(this.pos_act, 1, pos_y);
    }

    private void action_uper(char caract) {
        char[][] arrupletras = {new char[]{'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M'}, new char[]{'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'}};
        motionAct(0, 1);
        sendChar("d");
        sendChar("n");
        sendChar("sleep");
        switch (caract) {
            case 'A':
                motionAct(0, 0);
                sendChar("l");
                sendChar("n");
                sendChar("r");
                break;

            case 'N':
                motionAct(11, 0);
                sendChar("r");
                sendChar("n");
                sendChar("l");
                break;

            default:
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 12; j++) {
                        if (arrupletras[i][j] == caract) {
                            motionAct(j, i);
                            sendChar("n");
                        } else {
                            j++;
                        }
                    }
                }
                break;
        }
        motionAct(0, 1);
        sendChar("d");
        sendChar("n");
        sendChar("sleep");
    }

    private void action_lower(char caract) {
        char[][] arrlwletras = {new char[]{'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm'}, new char[]{'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'}};
        switch (caract) {
            case 'a':
                motionAct(0, 0);
                sendChar("l");
                sendChar("n");
                sendChar("r");
                return;
            case 'n':
                motionAct(11, 0);
                sendChar("r");
                sendChar("n");
                sendChar("l");
                return;
            default:
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 12; j++) {
                        if (arrlwletras[i][j] == caract) {
                            motionAct(j, i);
                            sendChar("n");
                        } else {
                            j++;
                        }
                    }
                }
                return;
        }
    }

    private void action_space() {
        if (id == 0) {
            motionAct(6, 1);
        } else {
            motionAct(4, 1);
        }
        sendChar("d");
        sendChar("d");
        sendChar("n");
    }

    private void action_digit(char caract) {
        switch (caract) {
            case '0':
                sendChar("0");
                return;
            case '1':
                sendChar("1");
                return;
            case '2':
                sendChar("2");
                return;
            case '3':
                sendChar("3");
                return;
            case '4':
                sendChar("4");
                return;
            case '5':
                sendChar("5");
                return;
            case '6':
                sendChar("6");
                return;
            case '7':
                sendChar("7");
                return;
            case '8':
                sendChar("8");
                return;
            case '9':
                sendChar("9");
                return;
            default:
                return;
        }
    }

    private void gen1Post(String str) {
        new Thread(new PostGen1(setter, ip, str)).start();
    }

    private void livePost(String str) {
        new Thread(new PostLive(setter, str, ip, user, password)).start();
    }

    private void sendChar(String str) {
        try {
            if (str.equals("sleep")) {
                Thread.sleep(500);
            } else if (id == WdDevice.MODELID_GEN1) {
                gen1Post(str);
                Thread.sleep(350);
            } else {
                livePost(str);
                Thread.sleep(500);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }
}
