package com.osdmod.remote;

import java.lang.reflect.Array;
import org.teleal.cling.model.message.header.EXTHeader;

public class SendTxtHub implements Runnable {
    char[][] arrletras = {new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'}, new char[]{' ', 241, 'a', 'd', 'g', 'j', 'm', 'p', 't', 'w'}, new char[]{241, 241, 'b', 'e', 'h', 'k', 'n', 'q', 'u', 'x'}, new char[]{241, 241, 'c', 'f', 'i', 'l', 'o', 'r', 'v', 'y'}, new char[]{241, 241, 'A', 'D', 'G', 'J', 'M', 's', 'T', 'z'}, new char[]{241, 241, 'B', 'E', 'H', 'K', 'N', 'P', 'U', 'W'}, new char[]{241, 241, 'C', 'F', 'I', 'L', 'O', 'Q', 'V', 'X'}, new char[]{241, 241, 241, 241, 241, 241, 241, 'R', 241, 'Y'}, new char[]{241, 241, 241, 241, 241, 241, 241, 'S', 241, 'Z'}};
    String ip;
    int[] pos = new int[2];
    boolean running;
    ResultIntSetter setter = new ResultIntSetter() {
        public void setResult(int result) {
            switch (result) {
            }
        }
    };
    String text;

    public SendTxtHub(String ip2, String text2) {
        this.text = text2;
        this.ip = ip2;
    }

    public void run() {
        this.running = true;
        for (int i = 0; i < this.text.length() && this.running; i++) {
            getLetraPos(this.text.charAt(i));
            sendChar(this.ip, Array.getInt(this.pos, 0), Array.getInt(this.pos, 1));
        }
        stop();
    }

    public void stop() {
        this.running = false;
    }

    private void getLetraPos(char caract) {
        int pau;
        for (int i = 0; i <= 8; i++) {
            int j = 0;
            while (true) {
                if (j > 9) {
                    break;
                } else if (this.arrletras[i][j] == caract) {
                    if (j == Array.getInt(this.pos, 0)) {
                        pau = 1200;
                    } else {
                        pau = 250;
                    }
                    try {
                        Thread.sleep((long) pau);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Array.setInt(this.pos, 0, j);
                    Array.setInt(this.pos, 1, i);
                } else {
                    j++;
                }
            }
        }
    }

    private void sendChar(String ip2, int num, int repet) {
        String str_num = Integer.toString(num);
        String str_ord = "";
        for (int i = 0; i < repet + 1; i++) {
            str_ord = String.valueOf(str_ord) + str_num;
        }
        new PostHub(this.setter, ip2, str_ord).run();
    }
}
