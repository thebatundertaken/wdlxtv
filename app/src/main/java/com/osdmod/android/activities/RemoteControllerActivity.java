package com.osdmod.android.activities;

import static java.util.Map.entry;

import android.animation.AnimatorInflater;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.drawable.TransitionDrawable;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.RecognizerIntent;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.osdmod.android.activities.listener.RemoteControllerOnScreenSwitchListener;
import com.osdmod.android.customviews.HorizontalPager;
import com.osdmod.android.customviews.NumberPicker;
import com.osdmod.android.customviews.NumberPickerChangeListener;
import com.osdmod.android.drawable.HubServicesDrawable;
import com.osdmod.android.prefs.WDPrefs;
import com.osdmod.android.sensor.ShakeListener;
import com.osdmod.formatter.PlaybackTimeFormatter;
import com.osdmod.model.WdDevice;
import com.osdmod.remote.R;
import com.osdmod.remote.RemoteKeyboard;
import com.osdmod.remote.WDTVLiveHub;
import com.osdmod.remote.WdRemoteController;
import com.osdmod.service.UpnpDiscoveryService;
import com.osdmod.service.WdMediaService;
import com.osdmod.service.WdUpnpService;
import com.osdmod.service.listener.WdMediaServiceEventListener;
import com.osdmod.service.listener.WdUpnpServiceEventListener;
import com.osdmod.utils.TextUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

public class RemoteControllerActivity extends AppCompatActivity {
    public static final String INTENT_DEVICE = "device";
    private static final String TAG = "RemoteControllerActivity";
    private static final int PREFERENCES_REQUEST_CODE = 1123;
    private static final int VOICE_RECOGNITION_REQUEST_CODE = 83;
    private final PlaybackTimeFormatter formatter = new PlaybackTimeFormatter();
    private final Handler backgroundTaskHandler = new Handler();
    private final RemoteControllerButtonClickListener btnClick = new RemoteControllerButtonClickListener();
    private long mediaPlaybackStatusLastCheck = -1;
    private int mediaPlaybackStatusCheckerCount = 0;
    private boolean ismediaPlaybackStatusCheckerRunning = false;
    private String[][] serviceList;
    private boolean dialogOpened = false;
    private long lastost = 0;
    private WdDevice wdDevice;
    private WdRemoteController wdRemoteController;
    private WdMediaService wdMediaService;
    private int errorCount = 0;
    private TextView txt_vol;
    private TextView txt_time_total;
    private TextView txt_time_current;
    private TextView txt_media;
    private ImageButton btn_mode;
    private ImageButton btn_vol;
    private ImageButton btn_cvol;
    private HorizontalPager horizontal_pager;
    private SeekBar sk_vol;
    private SeekBar sk_play;
    private View view_vol1;
    private GestureDetector gestureDetector;
    private boolean isTablet = false;
    private long lastshake = 0L;
    private long mLCurTime = 0;
    private long mLTotTime = 0;
    private ShakeListener shakeListener;
    private AlertDialog alertKeyboard;
    private boolean conf_buttons;
    private boolean conf_trackball;
    private boolean conf_vibrate;
    private boolean conf_volumebuttons;
    private boolean isMediaRewinding = false;
    private WdUpnpService wdUpnpService;
    private LinearLayout ly_finalr;
    private ImageView img_led;
    private ObjectAnimator pauseColorToggleAnimator;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            throw new IllegalArgumentException("Missing extras for RemoteControllerActivity");
        }

        wdDevice = (WdDevice) extras.get(INTENT_DEVICE);
        if (wdDevice == null) {
            throw new IllegalArgumentException("Missing DEVICE in Activity intent");
        }

        setContentView(R.layout.activity_remotecontroller);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle(
                wdDevice.getFriendlyName() != null && !wdDevice.getFriendlyName()
                        .isEmpty() ? wdDevice.getFriendlyName() : (!wdDevice.getIp()
                        .isEmpty() ? wdDevice.getIp() : getString(
                        R.string.rem_txt_nodev)));
        if (wdDevice.getFriendlyName() != null && !wdDevice.getFriendlyName()
                .isEmpty() && !wdDevice.getIp().isEmpty()) {
            getSupportActionBar().setSubtitle(wdDevice.getIp());
        }

        txt_vol = findViewById(R.id.txt_vol);
        btn_vol = findViewById(R.id.btn_vol);
        btn_cvol = findViewById(R.id.btn_cvol);
        sk_vol = findViewById(R.id.sk_vol);
        sk_play = findViewById(R.id.sk_play);
        view_vol1 = findViewById(R.id.view_vol1);
        txt_time_current = findViewById(R.id.txt_time_current);
        txt_time_total = findViewById(R.id.txt_time_total);
        txt_media = findViewById(R.id.txt_media);
        btn_mode = findViewById(R.id.btn_mode);
        horizontal_pager = findViewById(R.id.horizontal_pager);
        img_led = findViewById(R.id.img_led);
        ly_finalr = findViewById(R.id.ly_finalr);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        if (((float) metrics.widthPixels) / metrics.density >= 600.0f) {
            isTablet = true;
        }
        horizontal_pager.setOnScreenSwitchListener(
                new RemoteControllerOnScreenSwitchListener(this));

        final Integer[] remoteButtons = {R.id.btn_whome, R.id.btn_weject, R.id.btn_wsearch, R.id.btn_wpower,
                R.id.btn_rew, R.id.btn_pplay, R.id.btn_ff, R.id.btn_prev, R.id.btn_stop, R.id.btn_next,
                R.id.btn_home, R.id.btn_eject, R.id.btn_search, R.id.btn_power, R.id.btn_pgb, R.id.btn_config,
                R.id.btn_pgn, R.id.btn_audio, R.id.btn_subs, R.id.btn_mute, R.id.btn_a, R.id.btn_b, R.id.btn_c,
                R.id.btn_d, R.id.btn_up, R.id.btn_left, R.id.btn_ok, R.id.btn_right, R.id.btn_down, R.id.btn_back, R.id.btn_option};
        for (Integer remoteButton : remoteButtons) {
            ImageButton btnC = findViewById(remoteButton);
            if (btnC != null) {
                btnC.setOnClickListener(btnClick);
            }
        }
        btn_vol.setOnClickListener(btnClick);
        ImageButton btn_cvol;
        if (!isTablet && (btn_cvol = findViewById(R.id.btn_cvol)) != null) {
            btn_cvol.setOnClickListener(btnClick);
        }
        btn_mode.setOnClickListener(btnClick);
        findViewById(R.id.btn_touch).setOnClickListener(btnClick);
        findViewById(R.id.btn_tinfo).setOnClickListener(btnClick);
        findViewById(R.id.img_gespa).setVisibility(View.GONE);
        RemoteControllerSeekBarChangeListener onSeek = new RemoteControllerSeekBarChangeListener();
        sk_play.setOnSeekBarChangeListener(onSeek);
        sk_vol.setOnSeekBarChangeListener(onSeek);
        txt_time_current.setOnClickListener(btnClick);
        setTimesTxtsUI(PlaybackTimeFormatter.EMPTY, PlaybackTimeFormatter.EMPTY);
        setTimeSeekUI(mLCurTime, mLTotTime);
        gestureDetector = new GestureDetector(getApplicationContext(), new MyGestureDetector());
        findViewById(R.id.img_gespa).setOnTouchListener(
                (v, event) -> gestureDetector.onTouchEvent(event)
        );
        pauseColorToggleAnimator = (ObjectAnimator) AnimatorInflater.loadAnimator(this,
                R.animator.pause_color_toggle);
        pauseColorToggleAnimator.setTarget(txt_time_current);
        setDeviceOptions(wdDevice.getModelID());

        if (wdDevice.isUpnp()) {
            wdUpnpService = new WdUpnpService(wdDevice, new MyWdUpnpServiceEventListener(),
                    new MyWdMediaServiceEventListener());
            getApplicationContext().bindService(new Intent(this, UpnpDiscoveryService.class),
                    this.wdUpnpService, 1);
        } else {
            noUpnp(true);
        }
        invalidateOptionsMenu();
        loadPreferences();
    }

    private void positionAndResizeUI() {
        //getWindowManager().getDefaultDisplay().getMetrics(new DisplayMetrics());
        if (isTablet) {
            LinearLayout ly_dots = findViewById(R.id.ly_dots);
            RelativeLayout.LayoutParams params4 = (RelativeLayout.LayoutParams) ly_dots.getLayoutParams();
            params4.width = horizontal_pager.getWidth();
            ly_dots.setLayoutParams(params4);
            return;
        }

        LinearLayout ly_final = findViewById(R.id.ly_final);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) horizontal_pager.getLayoutParams();
        params.height = ly_final.getHeight();
        horizontal_pager.setLayoutParams(params);

        int size = ly_finalr.getHeight();
        if (ly_finalr.getHeight() > ly_finalr.getWidth()) {
            size = ly_finalr.getWidth();
        }

        int h = (int) ((((double) size) * 31.195d) / 100.0d);
        findViewById(R.id.btn_up).getLayoutParams().height = h;
        findViewById(R.id.btn_down).getLayoutParams().height = h;
        findViewById(R.id.btn_left).getLayoutParams().width = h;
        findViewById(R.id.btn_right).getLayoutParams().width = h;

        int h2 = (int) ((((double) size) * 37.6d) / 100.0d);
        ImageView btn_ok = findViewById(R.id.btn_ok);
        btn_ok.getLayoutParams().height = h2;
        btn_ok.getLayoutParams().width = h2;

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            findViewById(R.id.rl_arrows).getLayoutParams().height = ly_finalr.getHeight();
            LinearLayout ly_dots = findViewById(R.id.ly_dots);
            RelativeLayout.LayoutParams params2 = (RelativeLayout.LayoutParams) ly_dots.getLayoutParams();
            params2.width = ly_final.getWidth();
            ly_dots.setLayoutParams(params2);
            return;
        }

        LinearLayout ly_one_swone = findViewById(R.id.ly_one_swone);
        if (params.height < ly_one_swone.getHeight()) {
            LinearLayout ly_two_stwo = findViewById(R.id.ly_two_swtwo);
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-1, -2);
            ly_one_swone.setLayoutParams(layoutParams);
            if (ly_two_stwo != null) {
                ly_two_stwo.setLayoutParams(layoutParams);
            }
        }
    }

    private void setDeviceOptions(int modelID) {
        if (modelID == WdDevice.MODELID_STREAMING || modelID == WdDevice.MODELID_HUB) {
            horizontal_pager.removeViewAt(isTablet ? 0 : 1);
        } else {
            if (isTablet) {
                findViewById(R.id.ly_dots).setVisibility(View.INVISIBLE);
                horizontal_pager.removeViewAt(1);
                horizontal_pager.removeViewAt(1);
            } else {
                horizontal_pager.removeViewAt(2);
                horizontal_pager.removeViewAt(2);
            }
        }

        wdRemoteController = wdDevice.createRemoteController();
        if (wdRemoteController != null) {
            new Thread(() -> {
                int result = wdRemoteController.check();

                if (result != 1) {
                    remoteNotAvailable();
                    showToastShort(getString(R.string.rem_txt_nocon));
                    return;
                }

                new Thread(
                        () -> {
                            serviceList = wdRemoteController.getDeviceServices();
                            runOnUiThread(() -> {
                                if (serviceList == null) {
                                    horizontal_pager.removeViewAt(isTablet ? 1 : 2);
                                    return;
                                }
                                createHubServicesLayout(serviceList);
                                ((ImageView) findViewById(R.id.img_pos)).setImageResource(
                                        isTablet ? R.drawable.one : R.drawable.tone);
                            });
                        }
                ).start();
            }
            ).start();
        }
    }

    private void noUpnp(boolean silent) {
        wdMediaService = null;
        stopMediaPlaybackCheckerTask();

        if (!silent) {
            runOnUiThread(() -> {
                showToastShort(getString(R.string.rem_txt_upnpnot));
            });
        }
        remoteNotAvailable();
    }

    private void remoteNotAvailable() {
        final float vAlpha = 0.2f;
        runOnUiThread(() -> {
            if (ly_finalr != null) {
                ly_finalr.setAlpha(vAlpha);
            }

            if (horizontal_pager != null) {
                horizontal_pager.setAlpha(vAlpha);
            }
        });
    }

    private void showToastShort(final String val) {
        if (System.currentTimeMillis() - lastost > 3500) {
            runOnUiThread(
                    () -> Toast.makeText(RemoteControllerActivity.this, val, Toast.LENGTH_SHORT)
                            .show());
            lastost = System.currentTimeMillis();
        }
    }

    private void createHubServicesLayout(String[][] services) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout parent = findViewById(R.id.ly_one_swfour);
        HubServicesDrawable get_serv = new HubServicesDrawable();
        int i = 0;
        while (services[i][0] != null) {
            View custom = inflater.inflate(R.layout.row_service, parent);
            ImageButton btn1 = custom.findViewById(R.id.btn_s_1);
            ImageButton btn2 = custom.findViewById(R.id.btn_s_2);
            ImageButton btn3 = custom.findViewById(R.id.btn_s_3);
            get_serv.fetchDrawableOnThread(services[i][1], btn1);
            btn1.setTag("service_" + i);
            btn1.setOnClickListener(btnClick);
            if (services[i + 1][1] != null) {
                get_serv.fetchDrawableOnThread(services[i + 1][1], btn2);
                btn2.setTag("service_" + (i + 1));
                btn2.setOnClickListener(btnClick);
            }
            if (services[i + 2][1] != null) {
                get_serv.fetchDrawableOnThread(services[i + 2][1], btn3);
                btn3.setTag("service_" + (i + 2));
                btn3.setOnClickListener(btnClick);
            }
            int i2 = i + 2;
            if (parent != null) {
                parent.addView(custom);
            }
            i = i2 + 1;
        }
    }

    @SuppressLint("NonConstantResourceId")
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            case R.id.remotecontroller_activity_menu_jumpto:
                openJump2ToTimeDialog();
                return true;

            case R.id.remotecontroller_activity_menu_keyboard:
                openKeyboardDialog();
                return true;

            case R.id.remotecontroller_activity_menu_webin:
                if (wdRemoteController == null) {
                    return true;
                }

                String url = wdRemoteController.getWebUIUrl();
                if (url != null && !url.isEmpty()) {
                    startActivity(new Intent("android.intent.action.VIEW", Uri.parse(url)));
                }
                return true;

            case R.id.remotecontroller_activity_menu_preferences:
                startActivityForResult(
                        new Intent(this, SettingsActivity.class),
                        PREFERENCES_REQUEST_CODE);
                return true;

            case R.id.remotecontroller_activity_menu_testapp:
                startActivity(new Intent(getBaseContext(), DummyControllerActivity.class));
                return true;

            case R.id.remotecontroller_activity_menu_help:
                openHelpDialog();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.remotecontroller_activity_menu, menu);

        if (!wdDevice.isUpnp()) {
            menu.removeItem(R.id.remotecontroller_activity_menu_jumpto);
        }
        if (!wdDevice.isKeyboardAvailable()) {
            menu.removeItem(R.id.remotecontroller_activity_menu_keyboard);
        }
        if (!wdDevice.isRemoteControlAvailable() || !wdDevice.isKeyboardAvailable()) {
            menu.removeItem(R.id.remotecontroller_activity_menu_webin);
        }

        return true;
    }

    private void sendTextToDevice(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        ledflash();
        if (wdRemoteController instanceof WDTVLiveHub) {
            new Thread(() -> {
                wdRemoteController.sendText(text);
            }
            ).start();
            return;
        }

        //WDTVHDGen1, WDTVHDGen2, WDTVLivePlus
        new SendTextTask().execute(text);
        return;
    }

    private void sendCmdToDevice(String cmd) {
        ledflash();
        new Thread(() -> {
            int result = wdRemoteController.sendCommand(cmd);
            switch (result) {
                case 0:
                    if (++errorCount >= 3) {
                        new Thread(new CheckPingTask()).start();
                    }
                    break;

                case 1:
                    errorCount = 0;
                    wdMediaService.refresh();
                    break;

                case -1:
                    openInvalidButtonDialog();
                    return;
            }
        }
        ).start();
        return;
    }

    private void sendServiceToDevice(String service) {
        ledflash();
        new Thread(() -> {
            wdRemoteController.openService(service);
        }
        ).start();
        return;
    }

    private void openInvalidButtonDialog() {
        new AlertDialog.Builder(this).setIcon(R.drawable.ic_menu_info_details)
                .setTitle(getString(R.string.rem_txt_btnuna))
                .setView(TextUtils.linkifyText(getString(R.string.rem_txt_btnunat), this))
                .setPositiveButton(getString(R.string.rem_txt_ok),
                        (dialog, which) -> {
                        }).show();
    }

    private void openHelpDialog() {
        new AlertDialog.Builder(this).setIcon(R.drawable.ic_menu_help)
                .setTitle(getString(R.string.edia_txt_help))
                .setView(TextUtils.linkifyText(getString(R.string.d_help), this))
                .setPositiveButton(getString(R.string.rem_txt_ok),
                        (dialog, which) -> dialogOpened = false)
                .setOnCancelListener(dialog -> dialogOpened = false)
                .show();
        dialogOpened = true;
    }

    private void openConnectionErrorDialog(boolean ping) {
        if (dialogOpened) {
            return;
        }

        String tit = getString(ping ? R.string.rem_txt_conerror : R.string.rem_txt_conlost);
        String txt = getString(ping ? R.string.rem_txt_conerrort : R.string.rem_txt_conlostt);
        try {
            new AlertDialog.Builder(this).setIcon(R.drawable.appicon).setTitle(tit)
                    .setView(TextUtils.linkifyTextNoLink(txt, this))
                    .setPositiveButton(getString(R.string.m_txt_ok),
                            (dialog, which) -> dialogOpened = false)
                    .setNeutralButton(getString(R.string.m_txt_help),
                            (dialog, which) -> openHelpDialog())
                    .setOnCancelListener(dialog -> dialogOpened = false).show();
        } catch (Exception e) {
            Log.w(TAG, e);
        }
        dialogOpened = true;
    }

    private void setArrowsOrGesturePanel(boolean gesturePanelAsDefault) {
        ImageButton btn_touch = findViewById(R.id.btn_touch);
        ImageButton btn_up = findViewById(R.id.btn_up);
        ImageButton btn_left = findViewById(R.id.btn_left);
        ImageButton btn_ok = findViewById(R.id.btn_ok);
        ImageButton btn_right = findViewById(R.id.btn_right);
        ImageButton btn_down = findViewById(R.id.btn_down);
        ImageButton btn_back = findViewById(R.id.btn_back);
        ImageButton btn_option = findViewById(R.id.btn_option);
        ImageView img_gespa = findViewById(R.id.img_gespa);
        ImageView btn_tinfo = findViewById(R.id.btn_tinfo);
        int vis;
        if (gesturePanelAsDefault) {
            btn_touch.setImageResource(R.drawable.arr);
            img_gespa.setVisibility(View.VISIBLE);
            btn_tinfo.setVisibility(View.VISIBLE);
            vis = View.INVISIBLE;
        } else {
            btn_touch.setImageResource(R.drawable.ges);
            img_gespa.setVisibility(View.INVISIBLE);
            btn_tinfo.setVisibility(View.INVISIBLE);
            vis = View.VISIBLE;
        }
        btn_up.setVisibility(vis);
        btn_left.setVisibility(vis);
        btn_ok.setVisibility(vis);
        btn_right.setVisibility(vis);
        btn_down.setVisibility(vis);
        btn_back.setVisibility(vis);
        btn_option.setVisibility(vis);
    }

    private void ledflash() {
        runOnUiThread(() -> {
            TransitionDrawable transition = (TransitionDrawable) ResourcesCompat.getDrawable(
                    getResources(), R.drawable.ledflash, null);
            Objects.requireNonNull(transition).startTransition(100);
            img_led.setImageDrawable(transition);
        });

        if (conf_vibrate) {
            ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(
                    VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    @SuppressLint("SetTextI18n")
    private void setVolTxtIconIU(int vol) {
        runOnUiThread(() -> {
            txt_vol.setText("" + vol);
            int d;
            if (vol > 66) {
                d = R.drawable.volh;
            } else if (vol > 33) {
                d = R.drawable.volm;
            } else if (vol > 0) {
                d = R.drawable.voll;
            } else if (vol == 0) {
                d = R.drawable.volz;
            } else {
                d = R.drawable.volh;
            }
            btn_vol.setImageResource(d);
        });
    }

    private void setVolSeekUI(int vol) {
        runOnUiThread(() -> {
            sk_vol.setProgress(vol);
        });
    }

    private void setTimesTxtsUI(String cur, String tot) {
        runOnUiThread(() -> {
            txt_time_current.setText(cur);
            txt_time_total.setText(tot);
        });
    }

    private void setTimeSeekUI(long cur, long tot) {
        final long totalTime = tot;
        final long currentTime = cur;
        runOnUiThread(() -> {
            int progress = 0;
            if (totalTime != 0) {
                progress = (int) ((currentTime * 255) / totalTime);
                mLCurTime = currentTime;
            }
            sk_play.setProgress(progress);
        });
    }

    private void setTitleTxtUI(String title) {
        runOnUiThread(() -> {
            txt_media.setText(title == null ? getString(R.string.no_media_present) : title);
        });
    }

    private void setPlayModeUI(String playmode) {
        runOnUiThread(() -> {
            int rid;
            switch (playmode) {
                case WdMediaService.PLAY_MODE_REPEAT_ONE:
                    rid = R.drawable.rone;
                    break;
                case WdMediaService.PLAY_MODE_REPEAT_ALL:
                    rid = R.drawable.rall;
                    break;
                case WdMediaService.PLAY_MODE_RANDOM:
                    rid = R.drawable.rrandom;
                    break;
                default:
                    rid = R.drawable.rnormal;
            }
            btn_mode.setImageResource(rid);
        });
    }

    private void openGesturesDialog() {
        AlertDialog alertHelp = new AlertDialog.Builder(this).setTitle(
                        getString(R.string.rem_txt_gespan))
                .setView(getLayoutInflater().inflate(R.layout.dialog_help_ges,
                        null)).setPositiveButton(getString(R.string.rem_txt_ok),
                        (dialog, whichButton) -> {
                        }).create();
        alertHelp.setIcon(R.drawable.ic_menu_info_details);
        alertHelp.show();
    }

    private void openKeyboardDialog() {
        View addView = getLayoutInflater().inflate(R.layout.dialog_keyboard, null);
        alertKeyboard = new AlertDialog.Builder(this).setTitle(
                        getString(R.string.rem_txt_keyboard)).setView(addView)
                .setPositiveButton(getString(R.string.rem_txt_send),
                        (dialog, whichButton) -> sendTextToDevice(
                                ((EditText) addView.findViewById(R.id.etx_text)).getText()
                                        .toString()))
                .setNegativeButton(getString(R.string.rem_txt_cancel),
                        (dialog, whichButton) -> {
                        }).create();
        alertKeyboard.setIcon(R.drawable.ic_keyboard);

        ImageButton btn_mic = addView.findViewById(R.id.btn_mic);
        if (wdDevice.getModelID() == WdDevice.MODELID_STREAMING || wdDevice.getModelID() == WdDevice.MODELID_HUB) {
            addView.findViewById(R.id.txt_help).setVisibility(View.GONE);
        }
        if (!getPackageManager().queryIntentActivities(
                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0).isEmpty()) {
            btn_mic.setOnClickListener(v -> startVoiceRecognitionActivity());
        } else {
            btn_mic.setVisibility(View.GONE);
        }
        alertKeyboard.show();
    }

    private void startVoiceRecognitionActivity() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                Objects.requireNonNull(getClass().getPackage()).getName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "free_form");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PREFERENCES_REQUEST_CODE) {
            loadPreferences();
            return;
        }

        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == -1) {
            ArrayList<String> matches = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            EditText etx_text = alertKeyboard.findViewById(R.id.etx_text);
            etx_text.setText(Objects.requireNonNull(matches).get(0));
        }
    }

    private void loadPreferences() {
        WDPrefs pref = new WDPrefs(getApplicationContext());
        conf_vibrate = pref.isVibrationEnabled();
        conf_trackball = pref.isVTrackballEnabled();
        conf_buttons = pref.areBackSearchButtonsEnabled();
        conf_volumebuttons = pref.areVolumenButtonsEnabled();

        if (pref.isKeepScreenOn()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        if (pref.isShakeControl()) {
            if (shakeListener == null) {
                shakeListener = new ShakeListener(
                        (SensorManager) getSystemService(Context.SENSOR_SERVICE));
                shakeListener.setForceThreshHold(1.9d);
                shakeListener.setShakeListenerCallback(() -> {
                    if ((System.currentTimeMillis() - lastshake) > 500) {
                        lastshake = System.currentTimeMillis();
                        sendCmdToDevice(WdDevice.CMD_PLAY);
                    }
                });
            }
        } else {
            if (shakeListener != null) {
                shakeListener.shutdown();
                shakeListener = null;
            }
        }

        setArrowsOrGesturePanel(pref.isGesturePanelDefault());
    }

    private void openJump2ToTimeDialog() {
        final View addView = getLayoutInflater().inflate(R.layout.dialog_jump_to_time, null);
        NumberPicker nHor = addView.findViewById(R.id.num_hor);
        NumberPicker nMin = addView.findViewById(R.id.num_min);
        NumberPicker nSec = addView.findViewById(R.id.num_sec);
        String[] totalTime = formatter.secToStringArray(mLTotTime);
        int ht = Integer.parseInt(totalTime[0]);
        int mt = Integer.parseInt(totalTime[1]);
        int st = Integer.parseInt(totalTime[2]);
        String[] currentTime = formatter.secToStringArray(mLCurTime);
        int hc = Integer.parseInt(currentTime[0]);
        int mc = Integer.parseInt(currentTime[1]);
        int sc = Integer.parseInt(currentTime[2]);
        if (hc < ht) {
            mt = 59;
            st = 59;
        }
        if (mc < mt) {
            st = 59;
        }
        nHor.setRange(0, ht);
        nMin.setRange(0, mt);
        nSec.setRange(0, st);
        nHor.setCurrent(hc);
        nMin.setCurrent(mc);
        nSec.setCurrent(sc);

        AlertDialog alert = new AlertDialog.Builder(this).setTitle(
                        getString(R.string.rem_txt_jumpto)).setView(addView)
                .setPositiveButton(getString(R.string.rem_txt_ok),
                        (dialog, whichButton) -> {
                            int h = nHor.getCurrent();
                            int m = nMin.getCurrent();
                            int s = nSec.getCurrent();
                            String[] time = {"00", "00", "00"};
                            time[0] = (h < 10 ? "0" : "") + h;
                            time[1] = (m < 10 ? "0" : "") + m;
                            time[2] = (s < 10 ? "0" : "") + s;
                            String jumpToPosition = time[0] + ":" + time[1] + ":" + time[2];
                            mLCurTime = formatter.stringToSec(jumpToPosition);
                            wdMediaService.setPlaybackPosition(jumpToPosition);
                            setTimeSeekUI(mLCurTime, mLTotTime);
                            setTimesTxtsUI(jumpToPosition, formatter.secToString(mLTotTime));
                        }).setNegativeButton(getString(R.string.rem_txt_cancel),
                        (dialog, whichButton) -> {
                        }).create();
        alert.setIcon(R.drawable.ic_menu_time);

        NumberPickerChangeListener nChangeListener = (picker, oldVal, newVal) -> {
            int h = Integer.parseInt(totalTime[0]);
            int m = Integer.parseInt(totalTime[1]);
            int s = Integer.parseInt(totalTime[2]);
            int pickerId = picker.getId();

            if (pickerId == R.id.num_hor) {
                int act = nMin.getCurrent();
                int niu = 59;
                if (nHor.getCurrent() >= h) {
                    niu = m;
                }
                nMin.setRange(0, niu);
                nMin.setCurrent(Math.min(act, niu));
                int act2 = nSec.getCurrent();
                int niu2 = 59;
                if (nHor.getCurrent() >= h && nMin.getCurrent() >= m) {
                    niu2 = s;
                }
                nSec.setRange(0, niu2);
                if (act2 > niu2) {
                    nSec.setCurrent(niu2);
                    return;
                }

                nSec.setCurrent(act2);
            }
            if (pickerId == R.id.num_min) {
                int act3 = nSec.getCurrent();
                int niu3 = 59;
                if (nHor.getCurrent() >= h && nMin.getCurrent() >= m) {
                    niu3 = s;
                }
                nSec.setRange(0, niu3);
                if (act3 > niu3) {
                    nSec.setCurrent(niu3);
                } else {
                    nSec.setCurrent(act3);
                }
            }
        };

        nHor.setOnChangeListener(nChangeListener);
        nMin.setOnChangeListener(nChangeListener);
        alert.show();
    }

    private void flashView(View v) {
        final View view = v;
        new CountDownTimer(300, 100) {
            public void onTick(long millisUntilFinished) {
                if (millisUntilFinished <= 300) {
                    view.setVisibility(View.VISIBLE);
                    view.setClickable(false);
                }
            }

            public void onFinish() {
                view.setVisibility(View.INVISIBLE);
                view.setClickable(true);
            }
        }.start();
    }

    @SuppressLint("GestureBackNavigation")
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Map<Integer, String> commands = Map.of(
                KeyEvent.KEYCODE_DPAD_UP, WdDevice.CMD_UP,
                KeyEvent.KEYCODE_DPAD_LEFT, WdDevice.CMD_LEFT,
                KeyEvent.KEYCODE_DPAD_CENTER, WdDevice.CMD_OK,
                KeyEvent.KEYCODE_DPAD_RIGHT, WdDevice.CMD_RIGHT,
                KeyEvent.KEYCODE_DPAD_DOWN, WdDevice.CMD_DOWN,
                KeyEvent.KEYCODE_BACK, WdDevice.CMD_BACK,
                KeyEvent.KEYCODE_SEARCH, WdDevice.CMD_OPTION
        );

        if (commands.containsKey(keyCode)) {
            if (!conf_buttons) {
                if (keyCode == KeyEvent.KEYCODE_SEARCH) {
                    return false;
                }
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    return super.onKeyDown(keyCode, event);
                }
            }

            if (!conf_trackball) {
                return false;
            }
            sendCmdToDevice(commands.get(keyCode));
        }

        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (!conf_volumebuttons || wdMediaService == null) {
                return false;
            }

            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                wdMediaService.volumenUp();
            } else {
                wdMediaService.volumenDown();
            }
            ledflash();
            setVolTxtIconIU(wdMediaService.getVolumen());
            setVolSeekUI(wdMediaService.getVolumen());
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void offlinePlaybackPositionUpdate() {
        if (mLTotTime == 0) {
            mLCurTime = 0;
            setTimesTxtsUI(PlaybackTimeFormatter.EMPTY, PlaybackTimeFormatter.EMPTY);
        }

        if (isMediaRewinding) {
            if (mLCurTime > 0) {
                mLCurTime--;
            }
        } else if (mLCurTime > mLTotTime) {
            mLCurTime = 0;
        } else {
            mLCurTime++;
        }

        setTimesTxtsUI(formatter.secToString(mLCurTime), formatter.secToString(mLTotTime));
    }

    private void startColorTogglerTask() {
        runOnUiThread(() -> {
            pauseColorToggleAnimator.start();
        });
    }

    private void stopColorTogglerTask() {
        runOnUiThread(() -> {
            pauseColorToggleAnimator.end();
        });
    }

    private void checkWdMediaServiceState(String newPlaybackState) {
        switch (newPlaybackState) {
            case WdMediaService.PLAYBACK_PLAYING:
            case WdMediaService.PLAYBACK_TRANSITIONING:
                stopColorTogglerTask();
                startMediaPlaybackCheckerTask();
                break;

            case WdMediaService.PLAYBACK_STOPPED:
            case WdMediaService.PLAYBACK_NO_MEDIA_PRESENT:
                stopMediaPlaybackCheckerTask();
                stopColorTogglerTask();
                setTitleTxtUI(newPlaybackState.equals(
                        WdMediaService.PLAYBACK_STOPPED) ? getString(
                        R.string.remote_stopped) : null);
                mLTotTime = 0;
                mLCurTime = 0;
                setTimesTxtsUI(PlaybackTimeFormatter.EMPTY, PlaybackTimeFormatter.EMPTY);
                break;

            case WdMediaService.PLAYBACK_PREBUFFING:
            case WdMediaService.PLAYBACK_PAUSED_PLAYBACK:
                stopMediaPlaybackCheckerTask();
                if (wdMediaService != null) {
                    wdMediaService.syncPlaybackPosition();
                }
                startColorTogglerTask();
                break;
        }
    }

    protected void onDestroy() {
        super.onDestroy();

        if (wdUpnpService != null) {
            getApplicationContext().unbindService(wdUpnpService);
        }

        stopMediaPlaybackCheckerTask();

        if (shakeListener != null) {
            shakeListener.shutdown();
        }
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        getWindow().setFormat(PixelFormat.RGBA_8888);
    }

    protected void onStart() {
        super.onStart();
        new Thread(
                () -> {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Log.w(TAG, e);
                    }
                    runOnUiThread(RemoteControllerActivity.this::positionAndResizeUI);
                }
        ).start();
    }

    private void startMediaPlaybackCheckerTask() {
        synchronized (mediaPlaybackStatusChecker) {
            if (ismediaPlaybackStatusCheckerRunning) {
                return;
            }
            ismediaPlaybackStatusCheckerRunning = true;
            mediaPlaybackStatusLastCheck = -1;
            mediaPlaybackStatusChecker.run();
        }
    }

    private void stopMediaPlaybackCheckerTask() {
        synchronized (mediaPlaybackStatusChecker) {
            ismediaPlaybackStatusCheckerRunning = false;
            backgroundTaskHandler.removeCallbacks(mediaPlaybackStatusChecker);
        }
    }

    private class SendTextTask extends AsyncTask<String, Integer, Void> {
        private ProgressDialog mProgress;
        private Thread sendTextThread;
        private RemoteKeyboard remoteKeyboard;

        protected void onPreExecute() {
            super.onPreExecute();
            mProgress = new ProgressDialog(RemoteControllerActivity.this);
            mProgress.setTitle(getString(R.string.rem_txt_sndtxt));
            mProgress.setMessage(getString(R.string.rem_txt_wait));
            mProgress.setIndeterminate(true);
            mProgress.setCancelable(true);
            mProgress.setButton(getString(R.string.rem_txt_cancel),
                    (dialog, whichButton) -> sendTextThread.stop());
            mProgress.show();
        }

        protected Void doInBackground(String... keyboardText) {
            sendTextThread = new Thread(
                    () -> {
                        remoteKeyboard = new RemoteKeyboard(wdRemoteController);
                        remoteKeyboard.send(keyboardText[0]);
                    }
            );
            return null;
        }

        protected void onPostExecute(Void unused) {
            mProgress.dismiss();
        }
    }

    private class CheckPingTask implements Runnable {
        @Override
        public void run() {
            int result = 0;
            try {
                if (InetAddress.getByName(wdDevice.getIp()).isReachable(500)) {
                    result = 1;
                }
            } catch (IOException e) {
                Log.w(TAG, e);
            }

            final int finalResult = result;
            runOnUiThread(() -> {
                remoteNotAvailable();
                openConnectionErrorDialog(finalResult == 1);
            });
        }
    }

    private class MyGestureDetector extends GestureDetector.SimpleOnGestureListener {
        public boolean onDoubleTap(@NonNull MotionEvent ev) {
            flashView(findViewById(R.id.btn_back));
            sendCmdToDevice(WdDevice.CMD_BACK);
            return false;
        }

        public boolean onSingleTapConfirmed(@NonNull MotionEvent ev) {
            flashView(findViewById(R.id.btn_ok));
            sendCmdToDevice(WdDevice.CMD_OK);
            return false;
        }

        public void onLongPress(@NonNull MotionEvent ev) {
            flashView(findViewById(R.id.btn_option));
            sendCmdToDevice(WdDevice.CMD_OPTION);
        }

        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            //noinspection DataFlowIssue
            if (Math.abs(e2.getRawY() - e1.getRawY()) > Math.abs(e2.getRawX() - e1.getRawX())) {
                if (e2.getRawY() - e1.getRawY() > 0.0f) {
                    flashView(findViewById(R.id.btn_down));
                    sendCmdToDevice(WdDevice.CMD_DOWN);
                    if (Math.abs(velocityY) <= 1000.0f) {
                        return false;
                    }
                    sendCmdToDevice(WdDevice.CMD_DOWN);
                    if (Math.abs(velocityY) <= 2000.0f) {
                        return false;
                    }
                    sendCmdToDevice(WdDevice.CMD_DOWN);
                    return false;
                }
                flashView(findViewById(R.id.btn_up));
                sendCmdToDevice(WdDevice.CMD_UP);
                if (Math.abs(velocityY) <= 1000.0f) {
                    return false;
                }
                sendCmdToDevice(WdDevice.CMD_UP);
                if (Math.abs(velocityY) <= 2000.0f) {
                    return false;
                }
                sendCmdToDevice(WdDevice.CMD_UP);
                return false;
            }

            if (e2.getRawX() - e1.getRawX() > 0.0f) {
                flashView(findViewById(R.id.btn_right));
                sendCmdToDevice(WdDevice.CMD_RIGHT);
                if (Math.abs(velocityX) <= 1000.0f) {
                    return false;
                }
                sendCmdToDevice(WdDevice.CMD_RIGHT);
                if (Math.abs(velocityX) <= 2000.0f) {
                    return false;
                }
                sendCmdToDevice(WdDevice.CMD_RIGHT);
                return false;
            }

            flashView(findViewById(R.id.btn_left));
            sendCmdToDevice(WdDevice.CMD_LEFT);
            if (Math.abs(velocityX) <= 1000.0f) {
                return false;
            }
            sendCmdToDevice(WdDevice.CMD_LEFT);
            if (Math.abs(velocityX) <= 2000.0f) {
                return false;
            }
            sendCmdToDevice(WdDevice.CMD_LEFT);
            return false;
        }
    }

    private class RemoteControllerSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            int seekBarId = seekBar.getId();
            if (seekBarId == R.id.sk_play) {
                if (mLTotTime == 0) {
                    mLCurTime = 0;
                    setTimesTxtsUI(PlaybackTimeFormatter.EMPTY, PlaybackTimeFormatter.EMPTY);
                    return;
                }

                if (fromUser) {
                    String sTot = txt_time_total.getText().toString();
                    mLCurTime = (((long) progress) * formatter.stringToSec(sTot)) / 255;
                    setTimesTxtsUI(formatter.secToString(mLCurTime), sTot);
                    return;
                }
                return;
            }

            if (seekBarId == R.id.sk_vol) {
                if (fromUser) {
                    setVolTxtIconIU(progress);
                }
            }
        }

        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        public void onStopTrackingTouch(SeekBar seekBar) {
            if (seekBar.getId() == R.id.sk_play && mLTotTime != 0) {
                String position = formatter.secToString(mLCurTime);
                wdMediaService.setPlaybackPosition(position);
            } else if (seekBar.getId() == R.id.sk_vol) {
                wdMediaService.setVolumen(seekBar.getProgress());
            }
        }
    }

    private class RemoteControllerButtonClickListener implements View.OnClickListener {
        private final Map<Integer, String> buttonsToCmd;

        public RemoteControllerButtonClickListener() {
            super();

            buttonsToCmd = Map.ofEntries(
                    entry(R.id.btn_down, WdDevice.CMD_DOWN),
                    entry(R.id.btn_up, WdDevice.CMD_UP),
                    entry(R.id.btn_ok, WdDevice.CMD_OK),
                    entry(R.id.btn_left, WdDevice.CMD_LEFT),
                    entry(R.id.btn_right, WdDevice.CMD_RIGHT),
                    entry(R.id.btn_back, WdDevice.CMD_BACK),
                    entry(R.id.btn_whome, WdDevice.CMD_HOME),
                    entry(R.id.btn_home, WdDevice.CMD_HOME),
                    entry(R.id.btn_option, WdDevice.CMD_OPTION),
                    entry(R.id.btn_rew, WdDevice.CMD_REWIND),
                    entry(R.id.btn_pplay, WdDevice.CMD_PLAY),
                    entry(R.id.btn_ff, WdDevice.CMD_FASTFORWARD),
                    entry(R.id.btn_prev, WdDevice.CMD_PREV),
                    entry(R.id.btn_next, WdDevice.CMD_NEXT),
                    entry(R.id.btn_stop, WdDevice.CMD_STOP),
                    entry(R.id.btn_wpower, WdDevice.CMD_POWER),
                    entry(R.id.btn_power, WdDevice.CMD_POWER),
                    entry(R.id.btn_weject, WdDevice.CMD_EJECT),
                    entry(R.id.btn_eject, WdDevice.CMD_EJECT),
                    entry(R.id.btn_wsearch, WdDevice.CMD_SEARCH),
                    entry(R.id.btn_search, WdDevice.CMD_SEARCH),
                    entry(R.id.btn_pgb, WdDevice.CMD_PGB),
                    entry(R.id.btn_pgn, WdDevice.CMD_PGN),
                    entry(R.id.btn_config, WdDevice.CMD_CONFIG),
                    entry(R.id.btn_audio, WdDevice.CMD_AUDIO),
                    entry(R.id.btn_subs, WdDevice.CMD_SUBS),
                    entry(R.id.btn_mute, WdDevice.CMD_MUTE),
                    entry(R.id.btn_a, WdDevice.CMD_BTN_A),
                    entry(R.id.btn_b, WdDevice.CMD_BTN_B),
                    entry(R.id.btn_c, WdDevice.CMD_BTN_C),
                    entry(R.id.btn_d, WdDevice.CMD_BTN_D)
            );
        }

        @SuppressLint("NonConstantResourceId")
        @Override
        public void onClick(View v) {
            String tag = (String) v.getTag();
            if (tag != null && tag.startsWith("service_")) {
                if (serviceList != null) {
                    sendServiceToDevice(serviceList[Integer.parseInt(tag.substring(8))][3]);
                }
            }

            int buttonId = v.getId();
            if (buttonsToCmd.containsKey(buttonId)) {
                sendCmdToDevice(buttonsToCmd.get(buttonId));
                return;
            }

            switch (buttonId) {
                case R.id.txt_time_current:
                    openJump2ToTimeDialog();
                    return;

                case R.id.btn_cvol:
                    sk_vol.setVisibility(View.GONE);
                    txt_vol.setVisibility(View.GONE);
                    btn_cvol.setVisibility(View.GONE);
                    view_vol1.setVisibility(View.VISIBLE);
                    txt_media.setVisibility(View.VISIBLE);
                    btn_mode.setVisibility(View.VISIBLE);
                    return;

                case R.id.btn_mode:
                    wdMediaService.circlePlayMode();
                    setPlayModeUI(wdMediaService.getPlayMode());
                    return;

                case R.id.btn_vol:
                    if (sk_vol.getVisibility() == View.GONE) {
                        sk_vol.setVisibility(View.VISIBLE);
                        txt_vol.setVisibility(View.VISIBLE);
                        btn_cvol.setVisibility(View.VISIBLE);
                        view_vol1.setVisibility(View.GONE);
                        txt_media.setVisibility(View.GONE);
                        btn_mode.setVisibility(View.GONE);
                        return;
                    }
                    return;

                case R.id.btn_touch:
                    ImageView img_gespa = findViewById(R.id.img_gespa);
                    if (img_gespa.getVisibility() == View.INVISIBLE || img_gespa.getVisibility() == View.GONE) {
                        setArrowsOrGesturePanel(true);
                        return;
                    }

                    setArrowsOrGesturePanel(false);
                    return;

                case R.id.btn_tinfo:
                    openGesturesDialog();
                    return;
            }
        }
    }

    private class MyWdUpnpServiceEventListener implements WdUpnpServiceEventListener {
        @Override
        public void onServiceConnected(WdMediaService mediaService) {
            wdMediaService = mediaService;
            startMediaPlaybackCheckerTask();
        }

        @Override
        public void onServiceDisconnected() {
            noUpnp(false);
        }

        @Override
        public void onServiceConnectedError() {
            //TODO SCF connect on empty registry (home -> remote -> back -> remote again)
            noUpnp(false);
        }
    }

    private class MyWdMediaServiceEventListener implements WdMediaServiceEventListener {
        @Override
        public void onVolumenChanged(int volumen) {
            setVolTxtIconIU(volumen);
            setVolSeekUI(volumen);
        }

        @Override
        public void onPlaybackPositionChanged(String trackDuration, String relTime) {
            mLTotTime = formatter.stringToSec(trackDuration);
            mLCurTime = Math.min(formatter.stringToSec(relTime), mLTotTime);
            setTimesTxtsUI(formatter.secToString(mLCurTime), trackDuration);
            setTimeSeekUI(mLCurTime, mLTotTime);
        }

        @Override
        public void onPlaymodeChanged(String playMode) {
            setPlayModeUI(playMode);
        }

        @Override
        public void onPlaybackSpeedChanged(int playSpeed) {
            isMediaRewinding = playSpeed < 0;
        }

        @Override
        public void onPlaybackStatusChanged(String playbackStatus) {
            checkWdMediaServiceState(playbackStatus);
        }

        @Override
        public void onMediaTitleReceived(String title) {
            setTitleTxtUI(title);
        }

        @Override
        public void onFail(Exception e) {
            noUpnp(false);
        }
    }

    //TODO SCF se puede meter en WdMediaService??
    private final Runnable mediaPlaybackStatusChecker = () -> {
        if (WdMediaService.PLAYBACK_STOPPED.equals(wdMediaService.getPlaybackState())) {
            mediaPlaybackStatusLastCheck = -1;
            //wdMediaService.syncPlaybackStatus();
            return;
        }

        if (mediaPlaybackStatusLastCheck == -1) {
            mediaPlaybackStatusLastCheck = System.currentTimeMillis();
            wdMediaService.syncPlaybackPosition();
            wdMediaService.syncPlaybackStatus();
        } else {
            mediaPlaybackStatusCheckerCount++;
            if (isMediaRewinding || ((mediaPlaybackStatusCheckerCount % 5) != 0)) {
                offlinePlaybackPositionUpdate();
            } else {
                if (mediaPlaybackStatusCheckerCount == 5) {
                    wdMediaService.syncPlaybackPosition();
                } else if (mediaPlaybackStatusCheckerCount == 10) {
                    wdMediaService.syncPlaybackPosition();
                    wdMediaService.syncPlaybackStatus();
                    mediaPlaybackStatusCheckerCount = 0;
                }
            }
        }

        backgroundTaskHandler.postDelayed(this.mediaPlaybackStatusChecker, 1000);
    };

}
