package com.osdmod.service;

import android.util.Log;

import com.osdmod.service.listener.WdUpnpSubscriptionEventListener;

import org.teleal.cling.controlpoint.SubscriptionCallback;
import org.teleal.cling.model.gena.CancelReason;
import org.teleal.cling.model.gena.GENASubscription;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Service;

public class SubscriptionCallbackWrapper extends SubscriptionCallback {
    private static final String TAG = "SubscriptionCallbackWrapper";

    private final WdUpnpSubscriptionEventListener listener;

    protected SubscriptionCallbackWrapper(Service service, WdUpnpSubscriptionEventListener listener) {
        super(service, 600);
        this.listener = listener;
    }

    @Override
    protected void established(GENASubscription genaSubscription) {
        listener.onServiceSubscribed();
    }

    @Override
    protected void failed(GENASubscription genaSubscription, UpnpResponse upnpResponse, Exception e,
                          String s) {
        Log.d(TAG, "Failed = " + genaSubscription);

        listener.onServiceSubscriptionEnded();
    }

    @Override
    protected void eventReceived(GENASubscription genaSubscription) {
        listener.onSubscriptionEventReceived(genaSubscription);
    }

    @Override
    protected void eventsMissed(GENASubscription genaSubscription, int i) {
        Log.d(TAG, "EventsMissed = " + genaSubscription);

        listener.onSubscriptionEventMissed();
    }

    @Override
    protected void ended(GENASubscription genaSubscription, CancelReason cancelReason,
                         UpnpResponse upnpResponse) {
        Log.d(TAG, "Ended = " + genaSubscription);

        end();

        listener.onServiceSubscriptionEnded();
    }

}
