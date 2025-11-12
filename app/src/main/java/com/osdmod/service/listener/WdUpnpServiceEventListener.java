package com.osdmod.service.listener;

import org.teleal.cling.android.AndroidUpnpService;

public interface WdUpnpServiceEventListener {

    void onServiceConnected(AndroidUpnpService service);
    void onServiceConnectedError();
    void onServiceDisconnected();

}
