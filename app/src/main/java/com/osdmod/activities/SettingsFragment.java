package com.osdmod.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.osdmod.remote.R;

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
                .setView(LinkifyText(getString(R.string.d_help)))
                .setPositiveButton(getString(R.string.rem_txt_ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }).show();
    }

    private void openAboutDialog() {
        new AlertDialog.Builder(this.getContext()).setIcon(R.drawable.ic_menu_info_details)
                .setTitle(getString(R.string.m_about_tit))
                .setView(LinkifyText(getString(R.string.m_about_txt)))
                .setPositiveButton(getString(R.string.b_pref_ok),
                        (dialog, which) -> {
                        }).show();
    }

    private void openChangelogDialog() {
        new AlertDialog.Builder(this.getContext()).setIcon(R.drawable.ic_menu_info_details)
                .setTitle(getString(R.string.d_chlog_tit))
                .setView(LinkifyText(getString(R.string.d_chlog_txt)))
                .setPositiveButton(getString(R.string.b_pref_ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }).show();
    }

    private ScrollView LinkifyText(String message) {
        ScrollView svMessage = new ScrollView(this.getContext());
        TextView tvMessage = new TextView(this.getContext());
        tvMessage.setText(Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY));
        tvMessage.setMovementMethod(LinkMovementMethod.getInstance());
        tvMessage.setTextColor(-1);
        svMessage.setPadding(14, 2, 10, 12);
        svMessage.addView(tvMessage);
        return svMessage;
    }

    /** @noinspection DataFlowIssue*/
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        findPreference("btn_help").setOnPreferenceClickListener(btn_click);
        findPreference("btn_chglog").setOnPreferenceClickListener(btn_click);
        findPreference("btn_about").setOnPreferenceClickListener(btn_click);
    }
}
