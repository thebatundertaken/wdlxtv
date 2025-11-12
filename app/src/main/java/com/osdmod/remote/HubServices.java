package com.osdmod.remote;

import android.util.Log;

import org.teleal.cling.model.ServiceReference;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.lang.reflect.Array;
import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HubServices {
    private static final String TAG = "HubServices";

    public String[][] getServices(String ip) {
        OkHttpClient client = new OkHttpClient();
        String jsonBody = "{\"service\": -1}";
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("text/plain; charset=utf-8"));
        Request request = new Request.Builder()
                .url("http://" + ip + ":3388/cgi-bin/toServerValue.cgi")
                .addHeader("Accept-Encoding", "identity")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return null;
            }

            return extractServicesFromXML(Objects.requireNonNull(response.body()).string());
        } catch (Exception e) {
            Log.d(TAG, e.getMessage(), e);
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
