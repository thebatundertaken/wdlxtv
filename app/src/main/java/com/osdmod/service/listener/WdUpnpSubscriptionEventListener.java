package com.osdmod.service.listener;

import org.teleal.cling.model.gena.CancelReason;
import org.teleal.cling.model.gena.GENASubscription;

public interface WdUpnpSubscriptionEventListener {

    void onServiceSubscribed();

    void onServiceSubscriptionFailed(Exception e);

    void onServiceSubscriptionEnded(CancelReason cancelReason);

    /** @noinspection rawtypes*/
    void onSubscriptionEventReceived(GENASubscription genaSubscription);

    void onSubscriptionEventMissed();

}
