package com.osdmod.android.sensor;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.List;

public class ShakeListener implements SensorEventListener {
    private final SensorManager mSensorManager;
    private double mForceThreshHold = 1.5d;
    private ShakeListenerCallback shakeListenerCallback = null;
    private double mTotalForcePrev;

    public ShakeListener(SensorManager sm) {
        mSensorManager = sm;
        List<Sensor> mSensors = mSensorManager.getSensorList(1);
        if (!mSensors.isEmpty()) {
            Sensor mAccelerationSensor = mSensors.get(0);
            mSensorManager.registerListener(this, mAccelerationSensor, 1);
        }
    }

    public void setForceThreshHold(double threshhold) {
        mForceThreshHold = threshhold;
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        double totalForce = Math.sqrt(0.0d + Math.pow(event.values[0] / 9.80665f, 2.0d) + Math.pow(
                event.values[1] / 9.80665f, 2.0d) + Math.pow(event.values[2] / 9.80665f, 2.0d));
        if (totalForce < mForceThreshHold && mTotalForcePrev > mForceThreshHold) {
            onShake();
        }
        mTotalForcePrev = totalForce;
    }

    public void setShakeListenerCallback(ShakeListenerCallback listener) {
        shakeListenerCallback = listener;
    }

    private void onShake() {
        if (shakeListenerCallback != null) {
            shakeListenerCallback.onShake();
        }
    }

    public void shutdown() {
        mSensorManager.unregisterListener(this);
    }

}
