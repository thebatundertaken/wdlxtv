package com.osdmod.model;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WdDeviceRepository {
    private static final String filename = "saved_devices.xml";
    private final Context applicationContext;
    private final XMLRepository fileman = new XMLRepository();

    public WdDeviceRepository(Context applicationContext) {
        this.applicationContext = applicationContext;
    }

    public WdDevice findById(int deviceId) {
        List<String[]> savedDevicesArray = loadFromFile();
        for (String[] str : savedDevicesArray) {
            WdDevice wdDevice = fromXmlData(str);
            if (wdDevice.getDeviceId() == deviceId) {
                return wdDevice;
            }
        }

        throw new IllegalArgumentException("Unable to find device. DeviceId " + deviceId);
    }

    public int add(WdDevice wdDevice) {
        List<String[]> savedDevicesArray = loadFromFile();
        if(wdDevice.getDeviceId() < 0) {
            throw new IllegalArgumentException("Invalid DeviceId " + wdDevice.getDeviceId());
        }
        if(wdDevice.getUuid() == null || wdDevice.getUuid().isEmpty()) {
            wdDevice.setUuid(UUID.randomUUID().toString());
        }
        savedDevicesArray.add(toXmlData(wdDevice));
        fileman.allDevicesToFile(savedDevicesArray, filename, applicationContext);
        return wdDevice.getDeviceId();
    }

    public void update(WdDevice wdDevice) {
        if(wdDevice.getDeviceId() == -1) {
            throw new IllegalArgumentException("Unable to remove device. DeviceId is -1");
        }

        ArrayList<String[]> savedDevicesArray = loadFromFile();

        for (int i = 0; i < savedDevicesArray.size(); i++) {
            String[] str = savedDevicesArray.get(i);
            WdDevice candidate = fromXmlData(str);
            if (wdDevice.getDeviceId() == candidate.getDeviceId()) {
                savedDevicesArray.set(i, toXmlData(wdDevice));
                fileman.allDevicesToFile(savedDevicesArray, filename, applicationContext);
                return;
            }
        }

        throw new IllegalArgumentException("Unable to update device. DeviceId not found");
    }

    public void delete(int deviceId) {
        if(deviceId == -1) {
            throw new IllegalArgumentException("Unable to remove device. DeviceId is -1");
        }

        ArrayList<String[]> savedDevicesArray = loadFromFile();

        for (int i = 0; i < savedDevicesArray.size(); i++) {
            String[] str = savedDevicesArray.get(i);
            WdDevice wdDevice = fromXmlData(str);
            if (wdDevice.getDeviceId() == deviceId) {
                savedDevicesArray.remove(i);
                fileman.allDevicesToFile(savedDevicesArray, filename, applicationContext);
                return;
            }
        }

        throw new IllegalArgumentException("Unable to remove device. DeviceId not found");
    }

    public List<WdDevice> retrieveAll() {
        List<String[]> savedDevicesArray = loadFromFile();
        List<WdDevice> res = new ArrayList<>(savedDevicesArray.size());
        savedDevicesArray.forEach(str -> {
            res.add(fromXmlData(str));
        });
        return res;
    }

    private ArrayList<String[]> loadFromFile() {
        return fileman.savedDevicesToArrayList(filename, applicationContext);
    }

    private String[] toXmlData(WdDevice device) {
        String[] str = new String[14];
        str[0] = Integer.toString(device.getDeviceId());
        str[1] = device.getModelName();
        str[2] = device.getFriendlyName();
        str[3] = device.getUuid();
        str[4] = device.getIp();
        str[5] = Boolean.toString(device.iswDlxTVFirmware());
        str[6] = device.getUsername() == null ? "" : device.getUsername();
        str[7] = device.getPassword() == null ? "" : device.getPassword();
        str[8] = Boolean.toString(device.isRemoteControlAvailable());
        str[9] = Boolean.toString(device.isKeyboardAvailable());
        str[10] = Boolean.toString(device.isUpnp());
        str[11] = Boolean.toString(device.isConnected());
        return str;
    }

    private static WdDevice fromXmlData(String[] str) {
        return new WdDevice(str[1], str[2], str[4], str[3], Boolean.parseBoolean(str[5]), str[6],
                str[7], Boolean.parseBoolean(str[8]),
                Boolean.parseBoolean(str[9]),
                Boolean.parseBoolean(str[10]), Boolean.parseBoolean(str[11]),
                Integer.parseInt(str[0]));
    }

}
