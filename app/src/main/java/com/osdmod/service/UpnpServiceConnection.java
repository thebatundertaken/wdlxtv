package com.osdmod.service;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.osdmod.model.WdDevice;

import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.controlpoint.SubscriptionCallback;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.types.UDADeviceType;
import org.teleal.cling.model.types.UDN;
import org.teleal.cling.registry.Registry;

public class UpnpServiceConnection implements ServiceConnection {
    private static final String TAG = "UpnpServiceConnection";
    private final WdDevice wdDevice;
    private final UpnpServiceCallback upnpServiceCallback;
    private final WdMediaServiceCallback deviceCallback;
    private AndroidUpnpService upnpService;
    private RemoteDevice remoteDevice;

    public UpnpServiceConnection(WdDevice wdDevice, UpnpServiceCallback upnpServiceCallback,
                                 WdMediaServiceCallback deviceCallback) {
        this.wdDevice = wdDevice;
        this.upnpServiceCallback = upnpServiceCallback;
        this.deviceCallback = deviceCallback;
    }

    public AndroidUpnpService getUpnpService() {
        return upnpService;
    }

    public RemoteDevice getRemoteDevice() {
        return remoteDevice;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        upnpService = (AndroidUpnpService) service;
        try {
            remoteDevice = searchByUDN(new UDN(wdDevice.getUuid()));

            if (remoteDevice == null) {
                remoteDevice = searchByIP(wdDevice.getIp());
            }

            if (remoteDevice == null) {
                upnpServiceCallback.onServiceSubscriptionError();
                return;
            }

            subscribeToService();
        } catch (Exception e) {
            Log.w(TAG, e);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        upnpService = null;
    }

    private RemoteDevice searchByUDN(UDN udn) {
        return (RemoteDevice) upnpService.getRegistry().getDevice(udn, true);
    }

    private RemoteDevice searchByIP(String ip) {
        Registry registry = upnpService.getRegistry();
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

    private void subscribeToService() {
        SubscriptionCallback callback = new UpnpServiceSubscriptionCallback(
                remoteDevice.getServices()[0], deviceCallback, upnpServiceCallback);
        upnpService.getControlPoint().execute(callback);
    }

}