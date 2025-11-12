package com.osdmod.remote;

import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.teleal.cling.model.ServiceReference;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.Array;

import ch.boye.httpclientandroidlib.protocol.HTTP;

public class HubServices {
    private static final String TAG = "HubServices";

    public String[][] getServices(String ip) {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(
                "http://" + ip + ":3388/cgi-bin/toServerValue.cgi");
        try {
            StringEntity se = new StringEntity("{\"service\":-1}", "ISO-8859-1");
            se.setContentType(HTTP.PLAIN_TEXT_TYPE);
            httppost.setHeader("Content-Type", "text/plain;charset=ISO-8859-1");
            httppost.setEntity(se);
            HttpResponse response = httpclient.execute(httppost);
            if (response.getStatusLine().getStatusCode() > 201) {
                return null;
            }
            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(response.getEntity().getContent()));
            String receivedString = "";
            while (true) {
                String line2 = rd.readLine();
                if (line2 == null) {
                    return extractServicesFromXML(receivedString);
                }
                receivedString += line2;
            }
        } catch (IOException e2) {
            return null;
        }
    }

    public String[][] extractServicesFromXML(String xml) {
        String xml2 = xml.replaceAll("\\{ \"success\": 1, \"services\": \\[ ", "<services>")
                .replaceAll(" \\] \\}", "</services>").replaceAll("\\{ \"name\": ", "<serv><name>")
                .replaceAll("\", \"image_url\": \\{ \"", "\"</name><image_url ")
                .replaceAll("\": \"h", "=\"h").replaceAll("g\", \"", "g\" ")
                .replaceAll("g\" \\}, \"service_id\": ", "g\"/><service_id>\"")
                .replaceAll(", \"description\": \"", "\"</service_id><description>\"")
                .replaceAll(" \\}, <serv", "</description></serv><serv")
                .replaceAll(" \\}</services>", "</description></serv></services>")
                .replaceAll(">\"", ">").replaceAll("\"<", "<")
                .replaceAll("\\\\/", ServiceReference.DELIMITER);
        String[][] serviceList = (String[][]) Array.newInstance(String.class, new int[]{50, 5});
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new StringReader(xml2));
            int i = 0;
            boolean primera = true;
            for (int eventType = xpp.getEventType(); eventType != 1; eventType = xpp.next()) {
                if (eventType != 2) {
                    continue;
                }

                switch (xpp.getName()) {
                    case "serv":
                        if (!primera) {
                            i++;
                        }
                        primera = false;
                        break;

                    case "name":
                        serviceList[i][0] = xpp.nextText();
                        break;

                    case "image_url":
                        serviceList[i][1] = xpp.getAttributeValue(0);
                        serviceList[i][2] = xpp.getAttributeValue(1);
                        break;

                    case "service_id":
                        serviceList[i][3] = xpp.nextText();
                        break;

                    case "description":
                        serviceList[i][4] = xpp.nextText();
                        break;
                }
            }
            return serviceList;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return null;
        }
    }
}
