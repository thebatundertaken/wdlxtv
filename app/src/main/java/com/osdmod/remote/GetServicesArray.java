package com.osdmod.remote;

import java.io.StringReader;
import java.lang.reflect.Array;
import org.teleal.cling.model.ServiceReference;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

public class GetServicesArray {
    public String[][] getServicesArray(String xml) {
        String xml2 = xml.replaceAll("\\{ \"success\": 1, \"services\": \\[ ", "<services>").replaceAll(" \\] \\}", "</services>").replaceAll("\\{ \"name\": ", "<serv><name>").replaceAll("\", \"image_url\": \\{ \"", "\"</name><image_url ").replaceAll("\": \"h", "=\"h").replaceAll("g\", \"", "g\" ").replaceAll("g\" \\}, \"service_id\": ", "g\"/><service_id>\"").replaceAll(", \"description\": \"", "\"</service_id><description>\"").replaceAll(" \\}, <serv", "</description></serv><serv").replaceAll(" \\}</services>", "</description></serv></services>").replaceAll(">\"", ">").replaceAll("\"<", "<").replaceAll("\\\\/", ServiceReference.DELIMITER);
        String[][] serviceList = (String[][]) Array.newInstance(String.class, new int[]{50, 5});
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new StringReader(xml2));
            int i = 0;
            boolean primera = true;
            for (int eventType = xpp.getEventType(); eventType != 1; eventType = xpp.next()) {
                if (eventType == 2) {
                    if (xpp.getName().equals("serv")) {
                        if (!primera) {
                            i++;
                        }
                        primera = false;
                    } else if (xpp.getName().equals("name")) {
                        serviceList[i][0] = xpp.nextText();
                    } else if (xpp.getName().equals("image_url")) {
                        serviceList[i][1] = xpp.getAttributeValue(0);
                        serviceList[i][2] = xpp.getAttributeValue(1);
                    } else if (xpp.getName().equals("service_id")) {
                        serviceList[i][3] = xpp.nextText();
                    } else if (xpp.getName().equals("description")) {
                        serviceList[i][4] = xpp.nextText();
                    }
                }
            }
            return serviceList;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
