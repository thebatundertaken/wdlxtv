package com.osdmod.android.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.osdmod.remote.R;
import com.osdmod.utils.TextUtils;

public class SettingsFragment extends PreferenceFragmentCompat {

    private final Preference.OnPreferenceClickListener btn_click = preference -> {
        if (preference.getKey().equals("btn_help")) {
            openHelpDialog();
            return false;
        }
        if (preference.getKey().equals("btn_chglog")) {
            openChangelogDialog();
            return false;
        }
        if (!preference.getKey().equals("btn_about")) {
            return false;
        }

        openAboutDialog();
        return false;
    };

    private void openHelpDialog() {
        new AlertDialog.Builder(this.getContext()).setIcon(R.drawable.ic_menu_help)
                .setTitle(getString(R.string.edia_txt_help))
                .setView(TextUtils.linkifyText(getString(R.string.d_help), this.getContext()))
                .setPositiveButton(getString(R.string.rem_txt_ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }).show();
    }

    private void openAboutDialog() {
        new AlertDialog.Builder(this.getContext()).setIcon(R.drawable.ic_menu_info_details)
                .setTitle(getString(R.string.m_about_tit))
                .setView(TextUtils.linkifyText(getString(R.string.m_about_txt), this.getContext()))
                .setPositiveButton(getString(R.string.b_pref_ok),
                        (dialog, which) -> {
                        }).show();
    }

    private void openChangelogDialog() {
        new AlertDialog.Builder(this.getContext()).setIcon(R.drawable.ic_menu_info_details)
                .setTitle(getString(R.string.d_chlog_tit))
                .setView(TextUtils.linkifyText(getString(R.string.d_chlog_txt), this.getContext()))
                .setPositiveButton(getString(R.string.b_pref_ok),
                        (dialog, which) -> {
                        }).show();
    }

    /**
     * @noinspection DataFlowIssue
     */
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        findPreference("btn_help").setOnPreferenceClickListener(btn_click);
        findPreference("btn_chglog").setOnPreferenceClickListener(btn_click);
        findPreference("btn_about").setOnPreferenceClickListener(btn_click);
    }
}
