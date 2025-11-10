package com.osdmod.activities;

import static java.util.Map.entry;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
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
import android.text.Html;
import android.text.method.LinkMovementMethod;
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
import android.view.animation.AlphaAnimation;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.osdmod.customviews.HorizontalPager;
import com.osdmod.customviews.NumberPicker;
import com.osdmod.customviews.NumberPickerChangeListener;
import com.osdmod.model.WdDevice;
import com.osdmod.prefs.WDPrefs;
import com.osdmod.remote.AutomatedTelnetClient;
import com.osdmod.remote.BrowserUpnpService;
import com.osdmod.remote.GetServicesArray;
import com.osdmod.remote.GetServicesDrawable;
import com.osdmod.remote.OpenLiveCon;
import com.osdmod.remote.PostGen1;
import com.osdmod.remote.PostHub;
import com.osdmod.remote.PostLive;
import com.osdmod.remote.R;
import com.osdmod.remote.ResultIntSetter;
import com.osdmod.remote.SendLiveOrig;
import com.osdmod.remote.SendTxtWDlxTV;
import com.osdmod.remote.ShakeListener;
import com.osdmod.remote.TimeConvertion;
import com.osdmod.service.UpnpServiceCallback;
import com.osdmod.service.UpnpServiceConnection;
import com.osdmod.service.WdMediaService;
import com.osdmod.service.WdMediaServiceCallback;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

import ch.boye.httpclientandroidlib.protocol.HTTP;

public class RemoteControllerActivity extends AppCompatActivity {
    public static final String INTENT_DEVICE = "device";

    private static final String TAG = "RemoteControllerActivity";
    private static final int PREFERENCES_REQUEST_CODE = 1123;
    private static final int VOICE_RECOGNITION_REQUEST_CODE = 83;
    private final TimeConvertion tc = new TimeConvertion();
    private final HorizontalPager.OnScreenSwitchListener onSwitch = screen -> {
        ImageView img_pos = findViewById(R.id.img_pos);
        int tot = ((HorizontalPager) findViewById(
                R.id.horizontal_pager)).getChildCount();
        switch (screen) {
            case 0:
                if (tot != 3) {
                    img_pos.setImageResource(R.drawable.one);
                    break;
                } else {
                    img_pos.setImageResource(R.drawable.tone);
                    break;
                }
            case 1:
                if (tot != 3) {
                    img_pos.setImageResource(R.drawable.two);
                    break;
                } else {
                    img_pos.setImageResource(R.drawable.ttwo);
                    break;
                }
            case 2:
                img_pos.setImageResource(R.drawable.three);
                break;
        }
        if (tot == 1) {
            img_pos.setVisibility(View.GONE);
        }
    };
    private final Integer[] remoteButtons = {R.id.btn_whome, R.id.btn_weject, R.id.btn_wsearch, R.id.btn_wpower,
            R.id.btn_rew, R.id.btn_pplay, R.id.btn_ff, R.id.btn_prev, R.id.btn_stop, R.id.btn_next,
            R.id.btn_home, R.id.btn_eject, R.id.btn_search, R.id.btn_power, R.id.btn_pgb, R.id.btn_config,
            R.id.btn_pgn, R.id.btn_audio, R.id.btn_subs, R.id.btn_mute, R.id.btn_a, R.id.btn_b, R.id.btn_c,
            R.id.btn_d, R.id.btn_up, R.id.btn_left, R.id.btn_ok, R.id.btn_right, R.id.btn_down, R.id.btn_back, R.id.btn_option};
    private final Handler backgroundTaskHandler = new Handler();
    private Map<Integer, String> buttonsToCmd;
    private String[][] serviceList = ((String[][]) Array.newInstance(String.class,
            new int[]{50, 5}));
    private boolean dialogOpened = false;
    private long lastost = 0;
    private ProgressDialog mProgress;
    private WdDevice wdDevice;
    private final ResultIntSetter checker = result -> {
        switch (result) {
            case 0:
                remoteNotAvailable();
                if (wdDevice.iswDlxTVFirmware() && (wdDevice.getModelID() == WdDevice.MODELID_PLUS || wdDevice.getModelID() == WdDevice.MODELID_LIVE)) {
                    showToastLong(getString(R.string.rem_txt_conerror));
                } else {
                    showToastLong(getString(R.string.rem_txt_nocon));
                }

                break;

            case 1:
                if (wdDevice.getModelID() == WdDevice.MODELID_HUB || wdDevice.getModelID() == WdDevice.MODELID_STREAMING) {
                    new ReadHubServicesTask().execute();
                }
                break;

            case 2:
                remoteNotAvailable();
                if (wdDevice.iswDlxTVFirmware() && wdDevice.getModelID() >= WdDevice.MODELID_LIVE) {
                    showToastLong(getString(R.string.rem_txt_logerror));
                } else {
                    showToastLong(getString(R.string.rem_txt_nocon));
                }
                break;
        }
    };
    private boolean isColorTogglerRunning = false;
    private boolean ismediaPlaybackStatusCheckerRunning = false;
    private WdMediaService wdMediaService;
    private SendTxtWDlxTV sendTxtWDlxTV;
    private boolean isToggleBlueColor = false;
    private int mediaPlaybackStatusCheckerCount = 0;
    private int errorCount = 0;
    private final ResultIntSetter setter = result -> {
        switch (result) {
            case 0:
                errorCount++;
                break;

            case 1:
                errorCount = 0;
                break;
        }

        if (errorCount >= 3) {
            new CheckPingTask().execute();
        }
    };
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
    private final View.OnTouchListener gesDetected = (v, event) -> gestureDetector.onTouchEvent(
            event);
    private boolean isTablet = false;
    private long lastshake = 0L;
    private long mLCurTime;
    private long mLTotTime;
    private final SeekBar.OnSeekBarChangeListener onSeek = new SeekBar.OnSeekBarChangeListener() {
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            int seekBarId = seekBar.getId();
            if (seekBarId == R.id.sk_play) {
                if (mLTotTime == 0) {
                    setTimesTxtsUI("00:00:00", "00:00:00");
                    return;
                }

                if (fromUser) {
                    String sTot = txt_time_total.getText().toString();
                    mLCurTime = (((long) progress) * tc.stringToSec(sTot)) / 255;
                    setTimesTxtsUI(tc.secToString(mLCurTime), sTot);
                    return;
                }
                return;
            } else if (seekBarId == R.id.sk_vol) {
                if (fromUser) {
                    setVolTxtIconIU(progress);
                }
            }
        }

        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        public void onStopTrackingTouch(SeekBar seekBar) {
            if (seekBar.getId() == R.id.sk_play && mLTotTime != 0) {
                String position = tc.secToString(mLCurTime);
                wdMediaService.setPlaybackPosition(position);
            } else if (seekBar.getId() == R.id.sk_vol) {
                wdMediaService.setVolumen(seekBar.getProgress());
            }
        }
    };
    private ShakeListener shakeListener;
    private AlertDialog alertKeyboard;
    private boolean conf_buttons;
    private boolean conf_trackball;
    private boolean conf_vibrate;
    private boolean conf_volumebuttons;
    private boolean isActivityPaused = false;
    @SuppressLint("NonConstantResourceId")
    private final View.OnClickListener btnClick = v -> {
        String tag = (String) v.getTag();
        if (tag != null && tag.startsWith("service_")) {
            sendCmdToDevice("s_" + serviceList[Integer.parseInt(tag.substring(8))][3]);
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
                    setPanel(true);
                    return;
                }

                setPanel(false);
                return;

            case R.id.btn_tinfo:
                openGesHelpDialog();
                return;
        }
    };
    private boolean rewinding = false;
    private UpnpServiceConnection serviceConnection;
    private long mediaPlaybackStatusLastCheck = -1;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            throw new IllegalArgumentException("Missing extras for RemoteControllerActivity");
        }

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

        setContentView(R.layout.activity_remotecontroller);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowHomeEnabled(true);

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

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        if (((float) metrics.widthPixels) / metrics.density >= 600.0f) {
            isTablet = true;
        }
        horizontal_pager.setOnScreenSwitchListener(onSwitch);

        wdDevice = (WdDevice) extras.get(INTENT_DEVICE);
        if (wdDevice == null) {
            throw new IllegalArgumentException("Missing DEVICE in Activity intent");
        }

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
        sk_play.setOnSeekBarChangeListener(onSeek);
        sk_vol.setOnSeekBarChangeListener(onSeek);
        txt_time_current.setOnClickListener(btnClick);
        mLCurTime = 0;
        mLTotTime = 0;
        setTimesTxtsUI("00:00:00", "00:00:00");
        setTimeSeekUI(mLCurTime, mLTotTime);
        gestureDetector = new GestureDetector(new MyGestureDetector());
        findViewById(R.id.img_gespa).setOnTouchListener(gesDetected);
        setDeviceOptions(wdDevice.getModelID());
        getSupportActionBar().setTitle(
                wdDevice.getFriendlyName() != null && !wdDevice.getFriendlyName()
                        .isEmpty() ? wdDevice.getFriendlyName() : (!wdDevice.getIp()
                        .isEmpty() ? wdDevice.getIp() : getString(
                        R.string.rem_txt_nodev)));
        if (wdDevice.getFriendlyName() != null && !wdDevice.getFriendlyName()
                .isEmpty() && !wdDevice.getIp().isEmpty()) {
            getSupportActionBar().setSubtitle(wdDevice.getIp());
        }

        if (wdDevice.isUpnp()) {
            WdMediaServiceCallback mediaDeviceCallback = new WdMediaServiceCallback() {
                @Override
                public void onCurrentTrackMetaData() {
                    wdMediaService.initialSync();
                }

                @Override
                public void onPlaySpeed(int playSpeed) {
                    rewinding = playSpeed < 0;
                    //mediaPlaybackStatusCheckerInterval = 1000 / Math.abs(playSpeed);
                }

                @Override
                public void onTransportState(String state) {
                    checkState(state);
                }

                @Override
                public void onVolumenChanged(int volumen) {
                    setVolTxtIconIU(volumen);
                    setVolSeekUI(volumen);
                }

                @Override
                public void onPlaybackPositionChanged(String trackDuration, String relTime) {
                    mLTotTime = tc.stringToSec(trackDuration);
                    mLCurTime = Math.min(tc.stringToSec(relTime), mLTotTime);
                    setTimesTxtsUI(tc.secToString(mLCurTime), trackDuration);
                    setTimeSeekUI(mLCurTime, mLTotTime);
                }

                @Override
                public void onPlaymodeChanged(String playMode) {
                    setPlayModeUI(playMode);
                }

                @Override
                public void onPlaybackStatusChanged(String playbackStatus) {
                    checkState(playbackStatus);
                }

                @Override
                public void onMediaTitleReceived(String title) {
                    setTitleTxtUI(title == null ? "" : title);
                }
            };

            UpnpServiceCallback serviceCallback = new UpnpServiceCallback() {
                @Override
                public void onServiceSubscriptionError() {
                    noUpnp(false);
                }

                @Override
                public void onServiceSubscripted() {
                    wdMediaService = new WdMediaService(serviceConnection, mediaDeviceCallback);
                }
            };

            serviceConnection = new UpnpServiceConnection(wdDevice, serviceCallback,
                    mediaDeviceCallback);
            getApplicationContext().bindService(new Intent(this, BrowserUpnpService.class),
                    serviceConnection, 1);
        } else {
            noUpnp(true);
        }
        invalidateOptionsMenu();
        loadPreferences();
    }

    private void positionAndResizeUI() {
        HorizontalPager horizontalPager;
        getWindowManager().getDefaultDisplay().getMetrics(new DisplayMetrics());
        if (!isTablet && getResources().getConfiguration().orientation != 1) {
            HorizontalPager horizontalPager2 = findViewById(R.id.horizontal_pager);
            LinearLayout ly_final = findViewById(R.id.ly_final);
            LinearLayout ly_finalr = findViewById(R.id.ly_finalr);
            if (horizontalPager2 != null) {
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) horizontalPager2.getLayoutParams();
                params.height = ly_final.getHeight();
                horizontalPager2.setLayoutParams(params);
                findViewById(R.id.rl_arrows).getLayoutParams().height = ly_finalr.getHeight();
                LinearLayout ly_dots = findViewById(R.id.ly_dots);
                RelativeLayout.LayoutParams params2 = (RelativeLayout.LayoutParams) ly_dots.getLayoutParams();
                params2.width = ly_final.getWidth();
                ly_dots.setLayoutParams(params2);
                int size = ly_finalr.getHeight();
                if (ly_finalr.getHeight() > ly_finalr.getWidth()) {
                    size = ly_finalr.getWidth();
                }
                ImageView btn_ok = findViewById(R.id.btn_ok);
                int h = (int) ((((double) size) * 31.195d) / 100.0d);
                findViewById(R.id.btn_up).getLayoutParams().height = h;
                findViewById(R.id.btn_down).getLayoutParams().height = h;
                findViewById(R.id.btn_left).getLayoutParams().width = h;
                findViewById(R.id.btn_right).getLayoutParams().width = h;
                int h2 = (int) ((((double) size) * 37.6d) / 100.0d);
                btn_ok.getLayoutParams().height = h2;
                btn_ok.getLayoutParams().width = h2;
            }
        } else if (!isTablet) {
            HorizontalPager horizontalPager3 = findViewById(R.id.horizontal_pager);
            LinearLayout ly_final2 = findViewById(R.id.ly_final);
            LinearLayout ly_finalr2 = findViewById(R.id.ly_finalr);
            if (horizontalPager3 != null) {
                LinearLayout.LayoutParams params3 = (LinearLayout.LayoutParams) horizontalPager3.getLayoutParams();
                params3.height = ly_final2.getHeight();
                horizontalPager3.setLayoutParams(params3);
                int size2 = ly_finalr2.getHeight();
                if (ly_finalr2.getHeight() > ly_finalr2.getWidth()) {
                    size2 = ly_finalr2.getWidth();
                }
                ImageView btn_ok2 = findViewById(R.id.btn_ok);
                int h3 = (int) ((((double) size2) * 31.195d) / 100.0d);
                findViewById(R.id.btn_up).getLayoutParams().height = h3;
                findViewById(R.id.btn_down).getLayoutParams().height = h3;
                findViewById(R.id.btn_left).getLayoutParams().width = h3;
                findViewById(R.id.btn_right).getLayoutParams().width = h3;
                int h4 = (int) ((((double) size2) * 37.6d) / 100.0d);
                btn_ok2.getLayoutParams().height = h4;
                btn_ok2.getLayoutParams().width = h4;
                LinearLayout ly_one_swone = findViewById(R.id.ly_one_swone);
                LinearLayout ly_two_stwo = findViewById(R.id.ly_two_swtwo);
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-1, -2);
                if (params3.height < ly_one_swone.getHeight()) {
                    ly_one_swone.setLayoutParams(layoutParams);
                    if (ly_two_stwo != null) {
                        ly_two_stwo.setLayoutParams(layoutParams);
                    }
                }
            }
        } else if ((horizontalPager = findViewById(R.id.horizontal_pager)) != null) {
            LinearLayout ly_dots2 = findViewById(R.id.ly_dots);
            RelativeLayout.LayoutParams params4 = (RelativeLayout.LayoutParams) ly_dots2.getLayoutParams();
            params4.width = horizontalPager.getWidth();
            ly_dots2.setLayoutParams(params4);
        }
    }

    private void noUpnp(boolean silent) {
        if (!silent) {
            runOnUiThread(() -> {
                showToastLong(getString(R.string.rem_txt_upnpnot));
            });
        }

        wdMediaService = null;
        LinearLayout row_one_lyone_swone = findViewById(R.id.row_one_lyone_swone);
        AlphaAnimation alpha = new AlphaAnimation(0.2f, 0.2f);
        alpha.setDuration(0);
        alpha.setFillAfter(true);
        ObjectAnimator anim = ObjectAnimator.ofFloat(row_one_lyone_swone, "alpha", 0.2f)
                .setDuration(0);
        anim.start();
        anim.setTarget(btn_mode);
        if (btn_cvol != null) {
            anim.setTarget(btn_cvol);
        }
        anim.start();
        anim.setTarget(btn_mode);
        anim.start();
        anim.setTarget(btn_vol);
        anim.start();
        anim.setTarget(sk_vol);
        anim.start();
        anim.setTarget(txt_media);
        anim.start();
        anim.setTarget(txt_vol);
        anim.start();
    }

    private void setDeviceOptions(int modelID) {
        switch (modelID) {
            case WdDevice.MODELID_HUB:
            case WdDevice.MODELID_STREAMING:
                if (isTablet) {
                    horizontal_pager.removeViewAt(0);
                } else {
                    horizontal_pager.removeViewAt(1);
                }
                hubPost(checker, "check");
                break;

            case WdDevice.MODELID_LIVE:
            case WdDevice.MODELID_PLUS:
                if (isTablet) {
                    horizontal_pager.removeViewAt(1);
                    horizontal_pager.removeViewAt(1);
                    findViewById(R.id.ly_dots).setVisibility(View.INVISIBLE);
                } else {
                    horizontal_pager.removeViewAt(2);
                    horizontal_pager.removeViewAt(2);
                }
                if (!wdDevice.iswDlxTVFirmware()) {
                    if (wdDevice.isRemoteControlAvailable()) {
                        new OpenTelnetTask().execute();
                    }
                    final AlphaAnimation alpha = new AlphaAnimation(0.2f, 0.2f);
                    alpha.setDuration(0);
                    alpha.setFillAfter(true);
                    runOnUiThread(() -> {
                        ImageButton btn_pplay = findViewById(R.id.btn_pplay);
                        ImageButton btn_wsearch = findViewById(R.id.btn_wsearch);
                        ObjectAnimator.ofFloat(btn_pplay, "alpha", 0.2f).setDuration(0).start();
                        ObjectAnimator.ofFloat(btn_wsearch, "alpha", 0.2f).setDuration(0)
                                .start();
                    });
                } else if (!wdDevice.getIp().isEmpty()) {
                    openLive(checker, wdDevice.getIp(), wdDevice.getUsername(),
                            wdDevice.getPassword());
                }
                break;

            case WdDevice.MODELID_GEN2:
                if (!wdDevice.getIp().isEmpty()) {
                    openLive(checker, wdDevice.getIp(), wdDevice.getUsername(),
                            wdDevice.getPassword());
                }
                if (isTablet) {
                    findViewById(R.id.img_pos).setVisibility(View.GONE);
                    horizontal_pager.removeViewAt(1);
                    horizontal_pager.removeViewAt(1);
                    return;
                }
                horizontal_pager.removeViewAt(2);
                horizontal_pager.removeViewAt(2);
                break;

            default:
                if (isTablet) {
                    findViewById(R.id.img_pos).setVisibility(View.GONE);
                    horizontal_pager.removeViewAt(1);
                    horizontal_pager.removeViewAt(1);
                    return;
                }
                horizontal_pager.removeViewAt(2);
                horizontal_pager.removeViewAt(2);
        }
    }

    private void openLive(ResultIntSetter csetter, String cip, String cuser, String cpass) {
        new Thread(new OpenLiveCon(csetter, cip, cuser, cpass)).start();
    }

    private void remoteNotAvailable() {
        final LinearLayout ly_finalr = findViewById(R.id.ly_finalr);
        final LinearLayout ly_one_swfour = findViewById(R.id.ly_one_swfour);
        final float vAlpha = 0.2f;
        runOnUiThread(() -> {
            if (ly_finalr != null) {
                ObjectAnimator.ofFloat(ly_finalr, "alpha", vAlpha).setDuration(0).start();
            }
            if (ly_one_swfour != null) {
                ObjectAnimator.ofFloat(ly_one_swfour, "alpha", vAlpha).setDuration(0)
                        .start();
            }
        });
        for (int findViewById : remoteButtons) {
            final ImageButton btnC = findViewById(findViewById);
            if (btnC != null) {
                runOnUiThread(
                        () -> ObjectAnimator.ofFloat(btnC, "alpha", vAlpha).setDuration(0)
                                .start());
            }
        }
    }

    private void showToastLong(final String val) {
        if (System.currentTimeMillis() - lastost > 3500) {
            runOnUiThread(
                    () -> Toast.makeText(RemoteControllerActivity.this, val, Toast.LENGTH_LONG)
                            .show());
            lastost = System.currentTimeMillis();
        }
    }

    private void createLayout(String[][] services) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout parent = findViewById(R.id.ly_one_swfour);
        GetServicesDrawable get_serv = new GetServicesDrawable();
        int i = 0;
        while (services[i][0] != null) {
            View custom = inflater.inflate(R.layout.row_service, null);
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
                String url = "http://" + wdDevice.getIp();
                if (wdDevice.iswDlxTVFirmware() && wdDevice.getUsername() != null && !wdDevice.getUsername()
                        .isEmpty()) {
                    url = "http://" + wdDevice.getUsername() + ":" + wdDevice.getPassword() + "@" + wdDevice.getIp();
                } else if (wdDevice.getModelID() == WdDevice.MODELID_GEN1) {
                    url = "http://" + wdDevice.getIp() + "/addons";
                }
                startActivity(new Intent("android.intent.action.VIEW", Uri.parse(url)));
                return false;

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

    private void sendCmdToDevice(String cmd) {
        if (isActivityPaused) {
            return;
        }

        ledflash();
        if (wdDevice.getModelID() == WdDevice.MODELID_HUB || wdDevice.getModelID() == WdDevice.MODELID_STREAMING) {
            hubPost(setter, cmd);
            return;
        }

        if (cmd.startsWith("t_")) {
            new SendTextTask().execute(cmd.substring(2));
            return;
        }

        if (wdDevice.getModelID() == WdDevice.MODELID_GEN1) {
            gen1Post(setter, cmd);
            return;
        }

        if (wdDevice.iswDlxTVFirmware() || !wdDevice.isRemoteControlAvailable()) {
            livePost(setter, cmd);
            return;
        }
        new LiveOrigSendTask().execute(cmd);
    }

    private void openInvalidButtonDialog() {
        new AlertDialog.Builder(this).setIcon(R.drawable.ic_menu_info_details)
                .setTitle(getString(R.string.rem_txt_btnuna))
                .setView(LinkifyText(getString(R.string.rem_txt_btnunat)))
                .setPositiveButton(getString(R.string.rem_txt_ok),
                        (dialog, which) -> {
                        }).show();
    }

    private void openHelpDialog() {
        new AlertDialog.Builder(this).setIcon(R.drawable.ic_menu_help)
                .setTitle(getString(R.string.edia_txt_help))
                .setView(LinkifyText(getString(R.string.d_help)))
                .setPositiveButton(getString(R.string.rem_txt_ok),
                        (dialog, which) -> dialogOpened = false)
                .setOnCancelListener(dialog -> dialogOpened = false)
                .show();
        dialogOpened = true;
    }

    private ScrollView LinkifyText(String message) {
        ScrollView svMessage = new ScrollView(this);
        TextView tvMessage = new TextView(this);
        tvMessage.setText(Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY));
        tvMessage.setMovementMethod(LinkMovementMethod.getInstance());
        tvMessage.setTextColor(-1);
        svMessage.setPadding(14, 2, 10, 12);
        svMessage.addView(tvMessage);
        return svMessage;
    }

    private ScrollView LinkifyTextNoLink(String message) {
        ScrollView svMessage = new ScrollView(this);
        TextView tvMessage = new TextView(this);
        tvMessage.setText(message);
        tvMessage.setMovementMethod(LinkMovementMethod.getInstance());
        tvMessage.setTextColor(-1);
        svMessage.setPadding(14, 2, 10, 12);
        svMessage.addView(tvMessage);
        return svMessage;
    }

    private void hubPost(ResultIntSetter csetter, String cstr) {
        if (wdDevice.getIp().isEmpty()) {
            return;
        }
        new Thread(new PostHub(csetter, wdDevice.getIp(), cstr)).start();
    }

    private void livePost(ResultIntSetter csetter, String str) {
        if (wdDevice.getIp().isEmpty()) {
            return;
        }
        new Thread(new PostLive(csetter, str, wdDevice.getIp(), wdDevice.getUsername(),
                wdDevice.getPassword())).start();
    }

    private void gen1Post(ResultIntSetter csetter, String str) {
        if (wdDevice.getIp().isEmpty()) {
            return;
        }
        new Thread(new PostGen1(csetter, wdDevice.getIp(), str)).start();
    }

    private void openErrorDialog(boolean ping) {
        if (dialogOpened) {
            return;
        }

        String tit = getString(R.string.rem_txt_conerror);
        String txt = getString(R.string.rem_txt_conerrort);
        if (!ping) {
            tit = getString(R.string.rem_txt_conlost);
            txt = getString(R.string.rem_txt_conlostt);
        }
        try {
            new AlertDialog.Builder(this).setIcon(R.drawable.appicon).setTitle(tit)
                    .setView(LinkifyTextNoLink(txt))
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

    private void setPanel(boolean gesturePanelAsDefault) {
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
            ((ImageView) findViewById(R.id.img_led)).setImageDrawable(transition);
        });

        if (conf_vibrate) {
            ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(
                    VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    @SuppressLint("SetTextI18n")
    private void setVolTxtIconIU(int vol) {
        runOnUiThread(() -> {
            if (txt_vol != null) {
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
            }
        });
    }

    private void setVolSeekUI(int vol) {
        runOnUiThread(() -> {
            if (sk_vol != null) {
                sk_vol.setProgress(vol);
            }
        });
    }

    private void setTimesTxtsUI(String cur, String tot) {
        runOnUiThread(() -> {
            if (txt_time_current != null) {
                txt_time_current.setText(cur);
                txt_time_total.setText(tot);
            }
        });
    }

    private void setTimeSeekUI(long cur, long tot) {
        final long j = tot;
        final long j2 = cur;
        runOnUiThread(() -> {
            int progress = 0;
            if (j != 0) {
                progress = (int) ((j2 * 255) / j);
                mLCurTime = j2;
            }
            if (sk_play != null) {
                sk_play.setProgress(progress);
            }
        });
    }

    private void setTitleTxtUI(String tit) {
        runOnUiThread(() -> {
            if (txt_media != null) {
                txt_media.setText(tit);
            }
        });
    }

    private void setPlayModeUI(String playmode) {
        runOnUiThread(() -> {
            if (btn_mode == null) {
                return;
            }

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

    private void openGesHelpDialog() {
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
        final View addView = getLayoutInflater().inflate(R.layout.dialog_keyboard, null);
        alertKeyboard = new AlertDialog.Builder(this).setTitle(
                        getString(R.string.rem_txt_keyboard)).setView(addView)
                .setPositiveButton(getString(R.string.rem_txt_send),
                        (dialog, whichButton) -> sendCmdToDevice(
                                "t_" + ((EditText) addView.findViewById(
                                        R.id.etx_text)).getText()))
                .setNegativeButton(getString(R.string.rem_txt_cancel),
                        (dialog, whichButton) -> {
                        }).create();
        alertKeyboard.setIcon(R.drawable.ic_keyboard);
        ImageButton btn_mic = addView.findViewById(R.id.btn_mic);
        if (wdDevice.getModelID() == WdDevice.MODELID_HUB || wdDevice.getModelID() == WdDevice.MODELID_STREAMING) {
            addView.findViewById(R.id.txt_help).setVisibility(View.GONE);
        }
        if (!getPackageManager().queryIntentActivities(
                new Intent("android.speech.action.RECOGNIZE_SPEECH"), 0).isEmpty()) {
            btn_mic.setOnClickListener(v -> startVoiceRecognitionActivity());
        } else {
            btn_mic.setVisibility(View.GONE);
        }
        alertKeyboard.show();
    }

    private void startVoiceRecognitionActivity() {
        Intent intent = new Intent("android.speech.action.RECOGNIZE_SPEECH");
        intent.putExtra("calling_package", getClass().getPackage().getName());
        intent.putExtra("android.speech.extra.LANGUAGE_MODEL", "free_form");
        intent.putExtra("android.speech.extra.MAX_RESULTS", 5);
        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == -1) {
            ArrayList<String> matches = data.getStringArrayListExtra(
                    "android.speech.extra.RESULTS");
            EditText etx_text = alertKeyboard.findViewById(R.id.etx_text);
            if (etx_text != null) {
                etx_text.setText(matches.get(0));
            }
        } else if (requestCode == PREFERENCES_REQUEST_CODE) {
            loadPreferences();
        }
        super.onActivityResult(requestCode, resultCode, data);
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
                shakeListener.setOnShakeListener(() -> {
                    if ((System.currentTimeMillis() - lastshake) > 500) {
                        lastshake = System.currentTimeMillis();
                        sendCmdToDevice("p");
                    }
                });
            }
        } else {
            if (shakeListener != null) {
                shakeListener.close();
                shakeListener = null;
            }
        }

        setPanel(pref.isGesturePanelDefault());
    }

    private void openJump2ToTimeDialog() {
        final View addView = getLayoutInflater().inflate(R.layout.dialog_jump_to_time, null);
        NumberPicker nHor = addView.findViewById(R.id.num_hor);
        NumberPicker nMin = addView.findViewById(R.id.num_min);
        NumberPicker nSec = addView.findViewById(R.id.num_sec);
        String[] totalTime = tc.secToStringArray(mLTotTime);
        int ht = Integer.parseInt(totalTime[0]);
        int mt = Integer.parseInt(totalTime[1]);
        int st = Integer.parseInt(totalTime[2]);
        String[] currentTime = tc.secToStringArray(mLCurTime);
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
                            mLCurTime = tc.stringToSec(jumpToPosition);
                            wdMediaService.setPlaybackPosition(jumpToPosition);
                            setTimeSeekUI(mLCurTime, mLTotTime);
                            setTimesTxtsUI(jumpToPosition,
                                    tc.secToString(mLTotTime));
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

    private void flash(View v) {
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

        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
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

    private void sumTime() {
        if (rewinding && mLCurTime == 0) {
            stopMediaPlaybackCheckerTask();
        } else if (rewinding) {
            mLCurTime--;
        } else if (mLCurTime > mLTotTime) {
            mLCurTime = 0;
            stopMediaPlaybackCheckerTask();
        } else {
            mLCurTime++;
        }

        setTimesTxtsUI(tc.secToString(mLCurTime), tc.secToString(mLTotTime));
    }

    private void startMediaPlaybackCheckerTask() {
        synchronized (mediaPlaybackStatusChecker) {
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

    private void startColorTogglerTask() {
        synchronized (colorToggler) {
            isColorTogglerRunning = true;
            colorToggler.run();
        }
    }

    private void stopColorTogglerTask() {
        synchronized (colorToggler) {
            isColorTogglerRunning = false;
            backgroundTaskHandler.removeCallbacks(colorToggler);
            toggleTimeTextviewColor(true);
        }
    }

    private void toggleTimeTextviewColor(final boolean forceBlueColor) {
        if (txt_time_current == null) {
            return;
        }

        runOnUiThread(() -> {
            if (forceBlueColor || isToggleBlueColor) {
                txt_time_current.setTextColor(-1);
                isToggleBlueColor = false;
            } else {
                txt_time_current.setTextColor(
                        getResources().getColor(R.color.accent2, getTheme()));
                isToggleBlueColor = true;
            }
        });
    }

    private void checkState(String playbackState) {
        switch (playbackState) {
            case WdMediaService.PLAYBACK_PLAYING:
            case WdMediaService.PLAYBACK_TRANSITIONING:
                stopColorTogglerTask();
                startMediaPlaybackCheckerTask();
                break;

            case WdMediaService.PLAYBACK_STOPPED:
            case WdMediaService.PLAYBACK_NO_MEDIA_PRESENT:
                stopMediaPlaybackCheckerTask();
                stopColorTogglerTask();
                setTitleTxtUI(getString(playbackState.equals(
                        WdMediaService.PLAYBACK_STOPPED) ? R.string.remote_stopped : R.string.no_media_present));
                mLTotTime = 0;
                mLCurTime = 0;
                setTimesTxtsUI("00:00:00", "00:00:00");
                break;

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

        if (serviceConnection != null) {
            getApplicationContext().unbindService(serviceConnection);
        }
        if (sendTxtWDlxTV != null) {
            sendTxtWDlxTV.stop();
        }
        if (mProgress != null) {
            mProgress.dismiss();
        }
        backgroundTaskHandler.removeCallbacks(mediaPlaybackStatusChecker);
        backgroundTaskHandler.removeCallbacks(colorToggler);

        if (shakeListener != null) {
            shakeListener.close();
        }
        try {
            AutomatedTelnetClient.disconnect();
        } catch (Exception ignored) {
        }
    }

    protected void onPause() {
        super.onPause();

        isActivityPaused = true;
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        getWindow().setFormat(PixelFormat.RGBA_8888);
    }

    protected void onStart() {
        super.onStart();
        new FirstRunTask().execute();
    }

    protected void onResume() {
        super.onResume();
        isActivityPaused = false;
    }

    private class FirstRunTask extends AsyncTask<Void, Integer, Void> {
        protected Void doInBackground(Void... params) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Log.w(TAG, e);
            }
            runOnUiThread(RemoteControllerActivity.this::positionAndResizeUI);
            return null;
        }
    }

    private class OpenTelnetTask extends AsyncTask<Void, Integer, Void> {
        protected Void doInBackground(Void... params) {
            try {
                new AutomatedTelnetClient(wdDevice.getIp());
            } catch (Exception e) {
                Log.w(TAG, e);
            }
            return null;
        }
    }

    private class ReadHubServicesTask extends AsyncTask<Void, Integer, Boolean> {
        protected Boolean doInBackground(Void... params) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(
                    "http://" + wdDevice.getIp() + ":3388/cgi-bin/toServerValue.cgi");
            try {
                StringEntity se = new StringEntity("{\"service\":-1}", "ISO-8859-1");
                se.setContentType(HTTP.PLAIN_TEXT_TYPE);
                httppost.setHeader("Content-Type", "text/plain;charset=ISO-8859-1");
                httppost.setEntity(se);
                HttpResponse response = httpclient.execute(httppost);
                if (response.getStatusLine().getStatusCode() > 201) {
                    return false;
                }
                BufferedReader rd = new BufferedReader(
                        new InputStreamReader(response.getEntity().getContent()));
                String receivedString = "";
                while (true) {
                    String line2 = rd.readLine();
                    if (line2 == null) {
                        serviceList = new GetServicesArray().getServicesArray(receivedString);
                        return true;
                    }
                    receivedString += line2;
                }
            } catch (IOException e2) {
                return false;
            }
        }

        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            ImageView img_pos = findViewById(R.id.img_pos);
            if (result && img_pos != null && serviceList != null) {
                createLayout(serviceList);
                if (isTablet) {
                    img_pos.setImageResource(R.drawable.one);
                } else {
                    img_pos.setImageResource(R.drawable.tone);
                }
            } else if (horizontal_pager != null) {
                if (isTablet) {
                    horizontal_pager.removeViewAt(1);
                } else {
                    horizontal_pager.removeViewAt(2);
                }
            }
        }
    }

    private class SendTextTask extends AsyncTask<String, Integer, Void> {
        protected void onPreExecute() {
            super.onPreExecute();
            mProgress = new ProgressDialog(RemoteControllerActivity.this);
            mProgress.setTitle(getString(R.string.rem_txt_sndtxt));
            mProgress.setMessage(getString(R.string.rem_txt_wait));
            mProgress.setIndeterminate(true);
            mProgress.setCancelable(true);
            mProgress.setButton(getString(R.string.rem_txt_cancel),
                    (dialog, whichButton) -> sendTxtWDlxTV.stop());
            mProgress.show();
        }

        protected Void doInBackground(String... keyboardText) {
            sendTxtWDlxTV = new SendTxtWDlxTV(wdDevice.getIp(), wdDevice.getModelID(),
                    keyboardText[0],
                    wdDevice.getUsername(), wdDevice.getPassword());
            sendTxtWDlxTV.run();
            return null;
        }

        protected void onPostExecute(Void unused) {
            mProgress.dismiss();
        }
    }

    private class CheckPingTask extends AsyncTask<Void, Void, Integer> {
        protected Integer doInBackground(Void... param) {
            try {
                if (InetAddress.getByName(wdDevice.getIp()).isReachable(500)) {
                    return 1;
                }
            } catch (IOException e) {
                Log.w(TAG, e);
            }
            return 0;
        }

        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            runOnUiThread(() -> {
                remoteNotAvailable();
                openErrorDialog(result == 1);
            });
        }
    }

    private class LiveOrigSendTask extends AsyncTask<String, Integer, Integer> {
        private String currentstr;
        private int countPing = 0;

        protected Integer doInBackground(String... str) {
            if (str[0].startsWith("p") || str[0].startsWith("E")) {
                return -1;
            }
            try {
                boolean connected = AutomatedTelnetClient.isConnected();
                if (!connected) {
                    return 0;
                }

                if (countPing++ >= 3) {
                    countPing = 0;
                    try {
                        connected = InetAddress.getByName(wdDevice.getIp()).isReachable(500);
                    } catch (IOException e) {
                        Log.w(TAG, e);
                        return 0;
                    }
                }

                if (!connected) {
                    return 0;
                }
                currentstr = str[0];
                return 1;
            } catch (Exception e) {
                Log.w(TAG, e);
                return 0;
            }
        }

        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            if (result == null) {
                return;
            }

            switch (result) {
                case 1:
                    errorCount = 0;
                    new Thread(new SendLiveOrig(currentstr)).start();
                    return;

                case 0:
                    if (++errorCount >= 3) {
                        new CheckPingTask().execute();
                    }
                    return;

                case -1:
                    openInvalidButtonDialog();
                    return;
            }
        }
    }

    private class MyGestureDetector extends GestureDetector.SimpleOnGestureListener {
        public boolean onDoubleTap(@NonNull MotionEvent ev) {
            flash(findViewById(R.id.btn_back));
            sendCmdToDevice(WdDevice.CMD_BACK);
            return false;
        }

        public boolean onSingleTapConfirmed(@NonNull MotionEvent ev) {
            flash(findViewById(R.id.btn_ok));
            sendCmdToDevice(WdDevice.CMD_OK);
            return false;
        }

        public void onLongPress(@NonNull MotionEvent ev) {
            flash(findViewById(R.id.btn_option));
            sendCmdToDevice(WdDevice.CMD_OPTION);
        }

        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (Math.abs(e2.getRawY() - e1.getRawY()) > Math.abs(e2.getRawX() - e1.getRawX())) {
                if (e2.getRawY() - e1.getRawY() > 0.0f) {
                    flash(findViewById(R.id.btn_down));
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
                flash(findViewById(R.id.btn_up));
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
            } else if (e2.getRawX() - e1.getRawX() > 0.0f) {
                flash(findViewById(R.id.btn_right));
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
            } else {
                flash(findViewById(R.id.btn_left));
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
    }

    private final Runnable mediaPlaybackStatusChecker = () -> {
        if (mediaPlaybackStatusLastCheck == -1) {
            mediaPlaybackStatusLastCheck = System.currentTimeMillis();
            wdMediaService.syncPlaybackPosition();
        } else {
            sumTime();
            if (mediaPlaybackStatusCheckerCount <= 5 || rewinding) {
                mediaPlaybackStatusCheckerCount++;
            } else {
                wdMediaService.syncPlaybackPosition();
                wdMediaService.syncPlayMode();
                mediaPlaybackStatusCheckerCount = 0;
            }
        }

        backgroundTaskHandler.postDelayed(this.mediaPlaybackStatusChecker, 1000);
    };


    private final Runnable colorToggler = () -> {
        toggleTimeTextviewColor(false);
        backgroundTaskHandler.postDelayed(this.colorToggler, 500);
    };

}
