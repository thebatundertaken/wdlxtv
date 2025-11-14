package com.osdmod.android.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.osdmod.model.WdDevice;
import com.osdmod.model.WdDeviceRepository;
import com.osdmod.remote.R;
import com.osdmod.utils.TextUtils;

import java.util.Objects;
import java.util.regex.Pattern;

public class EditDeviceActivity extends AppCompatActivity {
    private final Integer[] helpImgList = {R.id.img_hmodel, R.id.img_hname, R.id.img_hip, R.id.img_huuid, R.id.img_hwdlxtv};

    @SuppressLint("NonConstantResourceId")
    private final View.OnClickListener mOnClickListener = v -> {
        switch (v.getId()) {
            case R.id.img_hmodel:
                openHelpDialog("model");
                return;
            case R.id.img_hname:
                openHelpDialog("name");
                return;
            case R.id.img_hip:
                openHelpDialog("ip");
                return;
            case R.id.img_huuid:
                openHelpDialog("uuid");
                return;
            case R.id.img_hwdlxtv:
                openHelpDialog("wdlxtv");
                return;
        }
    };

    private final CompoundButton.OnCheckedChangeListener chk_listener = (buttonView, isChecked) -> {
        if (buttonView.getId() != R.id.chk_wdlxtv) {
            return;
        }

        LinearLayout ly_lxusepass = findViewById(R.id.ly_lxusepass);
        ly_lxusepass.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        CheckBox chk_wdlxtv = findViewById(R.id.chk_wdlxtv);
        Gallery gallery = findViewById(R.id.gl_devices);
        if (gallery.getSelectedItemPosition() == 1) {
            chk_wdlxtv.setChecked(true);
        } else if (gallery.getSelectedItemPosition() == 0) {
            chk_wdlxtv.setChecked(true);
            ly_lxusepass.setVisibility(View.GONE);
        }
    };
    private WdDeviceRepository wdDeviceRepository;
    private String action;
    private boolean helping = false;
    private int deviceId;
    private EditText etxt_name;
    private EditText etxt_ip;
    private EditText etxt_uuid;
    private EditText etxt_user;
    private EditText etxt_pass;
    private CheckBox chk_wdlxtv;
    private Gallery gallery;
    private LinearLayout lay_wdlxtv;
    private LinearLayout ly_lxusepass;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wdDeviceRepository = new WdDeviceRepository(getApplicationContext());
        setContentView(R.layout.activity_editdevice);
        etxt_name = findViewById(R.id.etxt_name);
        etxt_ip = findViewById(R.id.etxt_ip);
        etxt_uuid = findViewById(R.id.etxt_uuid);
        etxt_user = findViewById(R.id.etxt_user);
        etxt_pass = findViewById(R.id.etxt_pass);
        chk_wdlxtv = findViewById(R.id.chk_wdlxtv);
        gallery = findViewById(R.id.gl_devices);
        lay_wdlxtv = findViewById(R.id.lay_wdlxtv);
        ly_lxusepass = findViewById(R.id.ly_lxusepass);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowHomeEnabled(true);

        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            throw new IllegalArgumentException("EditActivity requires intent extras");
        }
        action = extras.getString("action");
        if (action == null) {
            throw new IllegalArgumentException("EditActivity requires intent extra \"action\"");
        }
        if (action.equals("edit")) {
            getSupportActionBar().setTitle(getString(R.string.edia_txt_editdev));
            deviceId = extras.getInt("id");
        } else /*if (action.equals("newCreate")) */ {
            getSupportActionBar().setTitle(getString(R.string.edia_txt_createnew));
            deviceId = -1;
        }

        chk_wdlxtv.setOnCheckedChangeListener(chk_listener);
        gallery.setAdapter(new AddImgAdp());
        gallery.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View arg1, final int position,
                                       long arg3) {
                runOnUiThread(() -> {
                    switch (position) {
                        case 0:
                        case 1:
                            lay_wdlxtv.setVisibility(View.GONE);
                            chk_wdlxtv.setEnabled(true);
                            return;
                        case 2:
                        case 3:
                            lay_wdlxtv.setVisibility(View.VISIBLE);
                            chk_wdlxtv.setEnabled(true);
                            return;
                        case 4:
                            lay_wdlxtv.setVisibility(View.VISIBLE);
                            chk_wdlxtv.setChecked(true);
                            chk_wdlxtv.setEnabled(false);
                            ly_lxusepass.setVisibility(View.VISIBLE);
                            return;
                        case 5:
                            lay_wdlxtv.setVisibility(View.VISIBLE);
                            chk_wdlxtv.setChecked(true);
                            chk_wdlxtv.setEnabled(false);
                            ly_lxusepass.setVisibility(View.GONE);
                            return;
                    }
                });
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        runOnUiThread(() -> {
            for (int intValue : helpImgList) {
                ImageView btnC = findViewById(intValue);
                btnC.setOnClickListener(mOnClickListener);
                btnC.setVisibility(View.GONE);
            }
        });

        if (action.equals("edit")) {
            WdDevice wdDevice = wdDeviceRepository.findById(deviceId);
            etxt_name.setText(wdDevice.getFriendlyName());
            etxt_ip.setText(wdDevice.getIp());
            etxt_uuid.setText(wdDevice.getUuid());
            etxt_uuid.setEnabled(false);
            if (wdDevice.getUuid().isEmpty()) {
                findViewById(R.id.ly_uuid).setVisibility(View.GONE);
                findViewById(R.id.txt_uuid).setVisibility(View.GONE);
            }

            int deviceGalleryIcon = wdDevice.getDeviceGalleryIcon();
            if (deviceGalleryIcon < 0) {
                gallery.setSelection(0);
                etxt_name.setText(getString(R.string.edia_txt_errmodel));
            } else {
                gallery.setSelection(deviceGalleryIcon);
            }
            if (wdDevice.iswDlxTVFirmware()) {
                chk_wdlxtv.setChecked(true);
                etxt_user.setText(wdDevice.getUsername());
                etxt_pass.setText(wdDevice.getPassword());
                return;
            }
            chk_wdlxtv.setChecked(false);
            etxt_user.setText("");
            etxt_pass.setText("");
            ly_lxusepass.setVisibility(View.GONE);
            return;
        }

        etxt_name.setText("");
        etxt_ip.setText("");
        findViewById(R.id.ly_uuid).setVisibility(View.GONE);
        findViewById(R.id.txt_uuid).setVisibility(View.GONE);
        etxt_uuid.setText("");
        etxt_user.setText("");
        etxt_user.setText("");
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            case R.id.edit_activity_menu_help:
                showHelpIcons();
                return true;

            case R.id.edit_activity_menu_cancel:
                if (getParent() == null) {
                    setResult(0, new Intent());
                } else {
                    getParent().setResult(0, new Intent());
                }
                finish();
                return true;

            case R.id.edit_activity_menu_save:
                if(etxt_name.getText().toString().isEmpty()) {
                    Toast.makeText(this, getString(R.string.edia_txt_invname), Toast.LENGTH_LONG)
                            .show();
                    return true;
                }
                if (!validateHostAddress(etxt_ip.getText().toString())) {
                    Toast.makeText(this, getString(R.string.edia_txt_invip), Toast.LENGTH_LONG)
                            .show();
                    return true;
                }

                persistChanges();
                if (getParent() == null) {
                    setResult(-1, new Intent());
                } else {
                    getParent().setResult(-1, new Intent());
                }
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_activity_menu, menu);
        menu.findItem(R.id.edit_activity_menu_help).setVisible(!helping);
        return true;
    }

    private void showHelpIcons() {
        helping = true;
        runOnUiThread(() -> {
            for (Integer intValue : helpImgList) {
                findViewById(intValue).setVisibility(View.VISIBLE);
            }
        });
        invalidateOptionsMenu();
        new CountDownTimer(10000, 1000) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                runOnUiThread(() -> {
                    for (Integer intValue : helpImgList) {
                        ImageView btnC = findViewById(intValue);
                        if (btnC != null) {
                            btnC.setVisibility(View.GONE);
                        }
                    }
                });
                helping = false;
                invalidateOptionsMenu();
            }
        }.start();
    }

    private void openHelpDialog(String mode) {
        String message = "";
        switch (mode) {
            case "model":
                message = getString(R.string.edia_txt_modelhelp);
                break;
            case "name":
                message = getString(R.string.edia_txt_namehelp);
                break;
            case "ip":
                message = getString(R.string.edia_txt_iphelp);
                break;
            case "uuid":
                message = getString(R.string.edia_txt_uuidhelp);
                break;
            case "wdlxtv":
                message = getString(R.string.edia_txt_wdlxtvhelp);
                break;
        }
        new AlertDialog.Builder(this).setIcon(R.drawable.ic_menu_help)
                .setTitle(getString(R.string.edia_txt_help)).setView(TextUtils.linkifyText(message, this))
                .setPositiveButton(getString(R.string.edia_txt_ok),
                        (dialog, which) -> {
                        }).show();
    }

    private Boolean validateHostAddress(String address) {
        if (Pattern.compile(
                        "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])$")
                .matcher(address).matches()) {
            return true;
        }
        if (!address.startsWith("http://") && !address.startsWith("https://")) {
            address = "http://" + address;
        }
        return address.matches(
                "^http(s{0,1})://[a-zA-Z0-9_/\\-\\.]+\\.([A-Za-z/]{2,5})[a-zA-Z0-9_/\\&\\?\\=\\-\\.\\~\\%]*");
    }

    private void persistChanges() {
        String ipRawValue = etxt_ip.getText().toString();
        if (ipRawValue.startsWith("http://")) {
            ipRawValue = ipRawValue.replace("http://", "");
        } else if (ipRawValue.startsWith("https://")) {
            ipRawValue = ipRawValue.replace("https://", "");
        }

        WdDevice wdDevice = new WdDevice(
                WdDevice.getGalleryModelName(gallery.getSelectedItemPosition()),
                etxt_name.getText().toString(), ipRawValue, etxt_uuid.getText().toString());

        if (gallery.getSelectedItemPosition() >= 2) {
            wdDevice.setwDlxTVFirmware(chk_wdlxtv.isChecked());
            wdDevice.setUsername(etxt_user.getText().toString());
            wdDevice.setPassword(etxt_pass.getText().toString());
        }

        if (action.equals("newCreate")) {
            wdDeviceRepository.add(wdDevice);
        } else {
            wdDevice.setDeviceId(deviceId);
            wdDeviceRepository.update(wdDevice);
        }
    }

    private class AddImgAdp extends BaseAdapter {
        public AddImgAdp() {
        }

        public int getCount() {
            return WdDevice.getGalleryAvailableWdDevices();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            int modelIcon = WdDevice.getGalleryModelIcon(position);
            String modelName = WdDevice.getGalleryModelName(position);
            //new ImageView(EditDeviceActivity.this).setImageResource(modelIcon);
            LinearLayout ll = new LinearLayout(EditDeviceActivity.this);
            ll.setOrientation(LinearLayout.VERTICAL);
            ll.setGravity(1);

            ImageView i = new ImageView(EditDeviceActivity.this);
            i.setImageResource(modelIcon);
            i.setScaleType(ImageView.ScaleType.FIT_XY);
            ll.addView(i);

            TextView tv = new TextView(ll.getContext());
            tv.setTag(modelName);
            tv.setText(modelName);
            tv.setTextColor(getResources().getColor(R.color.blue2, getTheme()));
            tv.setGravity(1);
            ll.addView(tv);
            return ll;
        }
    }
}
