package com.osdmod.service;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.osdmod.model.WdDevice;
import com.osdmod.service.listener.WdMediaServiceEventListener;
import com.osdmod.service.listener.WdUpnpServiceEventListener;

import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.types.UDADeviceType;
import org.teleal.cling.model.types.UDN;
import org.teleal.cling.registry.Registry;

public class WdUpnpService implements ServiceConnection {
    private static final String TAG = "WdUpnpService";
    private final WdDevice wdDevice;
    private final WdUpnpServiceEventListener wdUpnpServiceEventListener;
    private final WdMediaServiceEventListener wdMediaServiceEventListener;

    public WdUpnpService(WdDevice wdDevice,
                         WdUpnpServiceEventListener wdUpnpServiceEventListener,
                         WdMediaServiceEventListener wdMediaServiceEventListener) {
        this.wdDevice = wdDevice;
        this.wdUpnpServiceEventListener = wdUpnpServiceEventListener;
        this.wdMediaServiceEventListener = wdMediaServiceEventListener;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        try {
            AndroidUpnpService upnpService = (AndroidUpnpService) service;

            UDN udn = new UDN(wdDevice.getUuid());
            RemoteDevice remoteDevice = (RemoteDevice) upnpService.getRegistry()
                    .getDevice(udn, true);
            if (remoteDevice == null) {
                remoteDevice = findRemoteDeviceByIP(upnpService.getRegistry(), wdDevice.getIp());
            }

            if (remoteDevice == null) {
                wdUpnpServiceEventListener.onServiceConnectedError();
                return;
            }

            WdMediaService wdMediaService = new WdMediaService(remoteDevice,
                    upnpService.getControlPoint(), wdMediaServiceEventListener);
            wdMediaService.subscribeToRemoteDevice();

            wdUpnpServiceEventListener.onServiceConnected(wdMediaService);
        } catch (Exception e) {
            Log.w(TAG, e);
            wdUpnpServiceEventListener.onServiceConnectedError();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        wdUpnpServiceEventListener.onServiceDisconnected();
    }

    private RemoteDevice findRemoteDeviceByIP(Registry registry, String ip) {
        for (Device<?, ?, ?> device : registry.getDevices(new UDADeviceType("MediaRenderer", 1))) {
            RemoteDevice rdevice = registry.getRemoteDevice(device.getIdentity().getUdn(),
                    false);
            String remoteIP = rdevice.getIdentity()
                    .getDescriptorURL().toString()
                    .replace("http://", "");
            if (remoteIP.substring(0, remoteIP.indexOf(":")).equals(ip)) {
                return rdevice;
            }
        }

        return null;
    }

}