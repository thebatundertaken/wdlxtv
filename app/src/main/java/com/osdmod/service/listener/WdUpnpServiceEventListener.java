package com.osdmod.service.listener;

import com.osdmod.service.WdMediaService;

public interface WdUpnpServiceEventListener {

    void onServiceConnected(WdMediaService wdMediaService);
    void onServiceConnectedError();
    void onServiceDisconnected();

}
