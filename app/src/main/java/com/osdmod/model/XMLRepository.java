package com.osdmod.model;

import android.content.Context;
import android.util.Log;
import android.util.Xml;

import com.osdmod.cipher.SimpleCipher;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class XMLRepository {
    private static final String TAG ="XMLRepository";

    public ArrayList<String[]> savedDevicesToArrayList(String filename, Context cntxt) {
        ArrayList<String[]> savedDevicesArray = new ArrayList<>();
        String XML = readFile(filename, cntxt);
        if ((XML == null)) {
            return savedDevicesArray;
        }

        try {
            int prevId = -1;
            String[] datos = new String[14];
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new StringReader(XML));
            for (int eventType = xpp.getEventType(); eventType != 1; eventType = xpp.next()) {
                if (eventType != 2) {
                    continue;
                }

                switch (xpp.getName()) {
                    case "device":
                        if (xpp.getAttributeCount() != 0) {
                            int id = Integer.parseInt(xpp.getAttributeValue(null, "id"));
                            if (id != prevId) {
                                if (prevId != -1) {
                                    savedDevicesArray.add(datos);
                                    datos = new String[14];
                                }
                                prevId = id;
                                datos[0] = Integer.toString(id);
                            }
                        }
                        break;
                    case "model":
                        datos[1] = xpp.nextText();
                        break;
                    case "name":
                        datos[2] = xpp.nextText();
                        break;
                    case "uuid":
                        datos[3] = xpp.nextText();
                        break;
                    case "ip":
                        datos[4] = xpp.nextText();
                        break;
                    case "wdlxtv":
                        datos[5] = xpp.nextText();
                        break;
                    case "lxuser":
                        datos[6] = xpp.nextText();
                        break;
                    case "lxpass":
                        datos[7] = xpp.nextText();
                        try {
                            datos[7] = SimpleCipher.decrypt(datos[7]);
                        } catch (Exception e) {
                            Log.d(TAG, e.getMessage(), e);
                        }
                        break;
                    case "remote":
                        datos[8] = xpp.nextText();
                        break;
                    case "keyboard":
                        datos[9] = xpp.nextText();
                        break;
                    case "upnp":
                        datos[10] = xpp.nextText();
                        break;
                }
            }

            if (prevId != 1 && datos[0] != null) {
                savedDevicesArray.add(datos);
            }
        } catch (Exception e) {
            Log.w(TAG, e.getMessage(), e);
        }

        return savedDevicesArray;
    }

    public void allDevicesToFile(List<String[]> devicesArray, String filename, Context cntxt) {
        try {
            FileOutputStream fos = cntxt.openFileOutput(filename, 0);
            XmlSerializer serializer = Xml.newSerializer();
            serializer.setOutput(fos, Xml.Encoding.UTF_8.toString());
            serializer.startDocument(null, true);
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.startTag(null, "list");
            for (int i = 0; i < devicesArray.size(); i++) {
                String[] str = devicesArray.get(i);
                serializer.startTag(null, "device");
                serializer.attribute(null, "id", str[0]);
                serializer.startTag(null, "model");
                serializer.text(str[1]);
                serializer.endTag(null, "model");
                serializer.startTag(null, "name");
                serializer.text(str[2]);
                serializer.endTag(null, "name");
                serializer.startTag(null, "uuid");
                serializer.text(str[3]);
                serializer.endTag(null, "uuid");
                serializer.startTag(null, "ip");
                serializer.text(str[4]);
                serializer.endTag(null, "ip");
                serializer.startTag(null, "wdlxtv");
                serializer.text(str[5]);
                serializer.endTag(null, "wdlxtv");
                serializer.startTag(null, "lxuser");
                serializer.text(str[6]);
                serializer.endTag(null, "lxuser");
                try {
                    str[7] = SimpleCipher.encrypt(str[7]);
                } catch (Exception e) {
                    Log.d(TAG, e.getMessage(), e);
                }
                serializer.startTag(null, "lxpass");
                serializer.text(str[7]);
                serializer.endTag(null, "lxpass");
                serializer.startTag(null, "remote");
                serializer.text(str[8]);
                serializer.endTag(null, "remote");
                serializer.startTag(null, "keyboard");
                serializer.text(str[9]);
                serializer.endTag(null, "keyboard");
                serializer.startTag(null, "upnp");
                serializer.text(str[10]);
                serializer.endTag(null, "upnp");
                XmlSerializer device = serializer.endTag(null, "device");
            }
            serializer.endTag(null, "list");
            serializer.endDocument();
            serializer.flush();
            fos.close();
        } catch (Exception e) {
            Log.w(TAG, e.getMessage(), e);
        }
    }

    private String readFile(String filename, Context cntxt) {
        FileInputStream fos = null;
        try {
            fos = cntxt.openFileInput(filename);
            BufferedReader r = new BufferedReader(new InputStreamReader(fos));
            StringBuilder total = new StringBuilder();
            while (true) {
                String line = r.readLine();
                if (line == null) {
                    return total.toString();
                }
                total.append(line);

            }
        } catch (IOException e) {
            Log.w(TAG, e.getMessage(), e);
            return null;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignore) {
                }
            }
        }
    }
}
