package com.osdmod.service.listener;

import org.teleal.cling.model.gena.GENASubscription;

public interface WdUpnpSubscriptionEventListener {

    void onServiceSubscribed();

    void onServiceSubscriptionEnded();

    /** @noinspection rawtypes*/
    void onSubscriptionEventReceived(GENASubscription genaSubscription);

    void onSubscriptionEventMissed();

}
