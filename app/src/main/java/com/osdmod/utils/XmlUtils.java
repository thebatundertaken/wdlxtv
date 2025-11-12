package com.osdmod.utils;

public class XmlUtils {
    public static String unescape(String xml) {
        if (xml == null) {
            return null;
        }

        return xml.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&apos;", "'");
    }

    public static String escape(String xml) {
        if (xml == null) {
            return null;
        }

        return xml.replaceAll("&", "&amp;");
    }
}
