package com.osdmod.android.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.osdmod.remote.R;

public class WDPrefs {
    private static final String KEEP_SCREEN_ON = "conf_screen";
    private static final String SHAKE_CONTROL = "conf_shakecontrol";
    private static final String GESTURE_PANEL_DEFAULT = "conf_deftouchpanel";
    private static final String VIBRATION_ENABLED = "conf_vibrate";
    private static final String TRACKBALL_ENABLED = "conf_trackball";
    private static final String BUTTONS_ENABLED = "conf_buttons";
    private static final String VOLUMEN_BUTTONS_ENABLED = "conf_volumebuttons";
    private static final String DISCOVERY_TIMEOUT = "conf_discoverytime";
    private final Context context;
    private SharedPreferences pref;

    public WDPrefs(Context context) {
        this.context = context;
        loadPreferences();
    }

    private void loadPreferences() {
        PreferenceManager.setDefaultValues(context, R.xml.preferences, false);
        pref = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean isKeepScreenOn() {
        return pref.getBoolean(KEEP_SCREEN_ON, false);
    }

    public boolean isShakeControl() {
        return pref.getBoolean(SHAKE_CONTROL, false);
    }

    public boolean isGesturePanelDefault() {
        return pref.getBoolean(GESTURE_PANEL_DEFAULT, false);
    }

    public boolean isVibrationEnabled() {
        return pref.getBoolean(VIBRATION_ENABLED, true);
    }

    public boolean isVTrackballEnabled() {
        return pref.getBoolean(TRACKBALL_ENABLED, true);
    }

    public boolean areBackSearchButtonsEnabled() {
        return pref.getBoolean(BUTTONS_ENABLED, false);
    }

    public boolean areVolumenButtonsEnabled() {
        return pref.getBoolean(VOLUMEN_BUTTONS_ENABLED, true);
    }

    public int getDiscoveryTimeoutSeconds() {
        try {
            return Integer.parseInt(pref.getString(DISCOVERY_TIMEOUT, "3"));
        }
        catch (Exception ignored) {
            return 3;
        }
    }

}
