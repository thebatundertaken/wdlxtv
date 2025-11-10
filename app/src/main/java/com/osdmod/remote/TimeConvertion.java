package com.osdmod.remote;

import org.teleal.cling.model.message.header.EXTHeader;

public class TimeConvertion {
    public long stringToSec(String sTime) {
        String[] parsedTime = sTime.split(":");
        int h = 0;
        int m = 0;
        int s = 0;
        try {
            if (parsedTime[0].length() > 2) {
                h = Integer.parseInt(parsedTime[0].substring(0, 2).replace(".", ""));
            } else if (parsedTime[0].length() <= 2) {
                h = Integer.parseInt(parsedTime[0].replace(".", ""));
            }
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException | StringIndexOutOfBoundsException e) {
        }
        try {
            if (parsedTime[1].length() > 2) {
                m = Integer.parseInt(parsedTime[1].substring(0, 3).replace(".", ""));
            } else if (parsedTime[1].length() <= 2) {
                m = Integer.parseInt(parsedTime[1].replace(".", ""));
            }
        } catch (NumberFormatException | StringIndexOutOfBoundsException e2) {
        } catch (ArrayIndexOutOfBoundsException e3) {
            m = h;
            h = 0;
        }
        try {
            if (parsedTime[2].length() > 2) {
                s = Integer.parseInt(parsedTime[2].substring(0, 2).replace(".", ""));
            } else if (parsedTime[2].length() <= 2) {
                s = Integer.parseInt(parsedTime[2].replace(".", ""));
            }
        } catch (NumberFormatException | StringIndexOutOfBoundsException e4) {
        } catch (ArrayIndexOutOfBoundsException e5) {
            s = m;
            m = h;
            h = 0;
        }
        return ((long) (h * 3600)) + ((long) (m * 60)) + ((long) s);
    }

    public String secToString(long secs) {
        int seconds = (int) (secs % 60);
        int minutes = (int) ((secs / 60) % 60);
        int hours = (int) ((secs / 3600) % 24);
        String[] time = {"00", "00", "00"};
        time[0] = String.valueOf(hours < 10 ? "0" : "") + hours;
        time[1] = String.valueOf(minutes < 10 ? "0" : "") + minutes;
        time[2] = String.valueOf(seconds < 10 ? "0" : "") + seconds;
        return new String(String.valueOf(time[0]) + ":" + time[1] + ":" + time[2]);
    }

    public String[] secToStringArray(long secs) {
        int seconds = (int) (secs % 60);
        int minutes = (int) ((secs / 60) % 60);
        int hours = (int) ((secs / 3600) % 24);
        String[] time = {"00", "00", "00"};
        time[0] = String.valueOf(hours < 10 ? "0" : "") + hours;
        time[1] = String.valueOf(minutes < 10 ? "0" : "") + minutes;
        time[2] = String.valueOf(seconds < 10 ? "0" : "") + seconds;
        return time;
    }
}
