package com.osdmod.remote;

import android.util.Log;

import com.osdmod.utils.TextUtils;

public class RemoteKeyboard {
    private static final String TAG = "SendText";
    private static final char[][] upperConversion = {new char[]{'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M'},
            new char[]{'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'}};
    private static final char[][] lowerConversion = {new char[]{'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm'},
            new char[]{'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'}};
    private final int[] pos_act;
    private final WdRemoteController wdRemoteController;

    public RemoteKeyboard(WdRemoteController wdRemoteController) {
        pos_act = new int[]{-1, 0};
        this.wdRemoteController = wdRemoteController;
    }

    public void send(String text) {
        text = TextUtils.convertToASCII(text);
        for (int i = 0; i < text.length(); i++) {
            char caract = text.charAt(i);
            if (Character.isDigit(caract)) {
                sendDigitChar(caract);
                continue;
            }

            if (Character.isLowerCase(caract)) {
                sendLowercaseChar(caract);
                continue;
            }

            if (Character.isUpperCase(caract)) {
                sendUppercaseChar(caract);
                continue;
            }

            if (Character.isWhitespace(caract)) {
                sendWhitespaceChar();
            }
        }

        motionAct(9, 1);
        sendChar("d");
        sendChar("d");
    }

    private void motionAct(int pos_x, int pos_y) {
        int x = pos_act[0] - pos_x;
        int y = pos_act[1] - pos_y;
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
        pos_act[0] = pos_x;
        pos_act[1] = pos_y;
    }

    private void sendUppercaseChar(char character) {
        motionAct(0, 1);
        sendChar("d");
        sendChar("n");
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignored) {
        }

        sendAlphaChar(character, upperConversion);

        motionAct(0, 1);
        sendChar("d");
        sendChar("n");
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignored) {
        }
    }

    private void sendLowercaseChar(char character) {
        sendAlphaChar(character, lowerConversion);
    }

    private void sendAlphaChar(char character, char[][] charConversion) {
        switch (character) {
            case 'A':
            case 'a':
                motionAct(0, 0);
                sendChar("l");
                sendChar("n");
                sendChar("r");
                return;

            case 'N':
            case 'n':
                motionAct(11, 0);
                sendChar("r");
                sendChar("n");
                sendChar("l");
                return;

            default:
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 12; j++) {
                        if (charConversion[i][j] == character) {
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

    private void sendWhitespaceChar() {
        if (wdRemoteController instanceof WDTVHDGen1) {
            motionAct(6, 1);
        } else {
            motionAct(4, 1);
        }
        sendChar("d");
        sendChar("d");
        sendChar("n");
    }

    private void sendDigitChar(char character) {
        switch (character) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                sendChar("" + character);
                return;
        }
    }

    private void sendChar(String character) {
        if (wdRemoteController == null) {
            return;
        }

        try {
            Thread t = new Thread(
                    () -> {
                        wdRemoteController.sendText(character);
                    }
            );
            t.start();
            t.wait(500);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }
}
