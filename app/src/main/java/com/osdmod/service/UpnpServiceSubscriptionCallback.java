package com.osdmod.service;

import android.util.Log;

import org.teleal.cling.controlpoint.SubscriptionCallback;
import org.teleal.cling.model.gena.CancelReason;
import org.teleal.cling.model.gena.GENASubscription;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.state.StateVariableValue;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.util.Map;
import java.util.Objects;

public class UpnpServiceSubscriptionCallback extends SubscriptionCallback {
    private static final String TAG = "UpnpServiceSubscriptionCallback";

    private final WdMediaServiceCallback mediaDeviceCallback;
    private final UpnpServiceCallback upnpServiceCallback;

    protected UpnpServiceSubscriptionCallback(Service service,
                                              WdMediaServiceCallback mediaDeviceCallback,
                                              UpnpServiceCallback upnpServiceCallback) {
        super(service, 600);
        this.mediaDeviceCallback = mediaDeviceCallback;
        this.upnpServiceCallback = upnpServiceCallback;
    }

    @Override
    protected void ended(GENASubscription subs, CancelReason reason, UpnpResponse resp) {
        end();

        //TODO SCF review call to parent to reconnect
        //Re-connect
        //subscribeToService();
    }

    @Override
    protected void established(GENASubscription subs) {
        upnpServiceCallback.onServiceSubscripted();
    }

    @Override
    protected void eventReceived(GENASubscription genaSubscription) {
        if (genaSubscription.getCurrentSequence().getValue() == 0) {
            return;
        }

        //noinspection unchecked
        Map<String, StateVariableValue<?>> values = genaSubscription.getCurrentValues();
        String XML = Objects.requireNonNull(values.get("LastChange")).getValue().toString();
        newEvent(XML);
    }

    @Override
    protected void eventsMissed(GENASubscription subs, int i) {

    }

    @Override
    protected void failed(GENASubscription subs, UpnpResponse resp, Exception e, String s) {
    }

    private void newEvent(String XML) {
        String XML2 = XML.replaceAll("&", "&amp;");
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new StringReader(XML2));
            for (int eventType = xpp.getEventType(); eventType != 1; eventType = xpp.next()) {
                if (eventType != 2) {
                    continue;
                }

                switch (xpp.getName()) {
                    case "TransportState":
                        String state = xpp.getAttributeValue(null, "val");
                        mediaDeviceCallback.onTransportState(state);
                        if (state.equals(WdMediaService.PLAYBACK_STOPPED) || state.equals(WdMediaService.PLAYBACK_NO_MEDIA_PRESENT)) {
                            //Done
                            return;
                        }
                        break;

                    case "CurrentTrackMetaData":
                        mediaDeviceCallback.onCurrentTrackMetaData();
                        break;

                    case "TransportPlaySpeed":
                        int playSpeed = Integer.parseInt(xpp.getAttributeValue(null, "val"));
                        mediaDeviceCallback.onPlaySpeed(playSpeed);
                        break;

                    case "TransportStatus":
                        String status = xpp.getAttributeValue(null, "val");
                        if (status.equals(WdMediaService.PLAYBACK_PREBUFFING)) {
                            mediaDeviceCallback.onTransportState(WdMediaService.PLAYBACK_PAUSED_PLAYBACK);
                        }
                        break;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

}