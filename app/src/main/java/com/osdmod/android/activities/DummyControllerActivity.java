package com.osdmod.android.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.TransitionDrawable;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.accessibility.AccessibilityEventCompat;

import com.osdmod.android.customviews.HorizontalPager;
import com.osdmod.android.customviews.NumberPicker;
import com.osdmod.android.customviews.NumberPickerChangeListener;
import com.osdmod.android.customviews.OnScreenSwitchListener;
import com.osdmod.android.sensor.ShakeListener;
import com.osdmod.formatter.PlaybackTimeFormatter;
import com.osdmod.remote.R;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Objects;

public class DummyControllerActivity extends AppCompatActivity {
    private static final int PREFERENCES_REQUEST_CODE = 1123;
    private static final int VOICE_RECOGNITION_REQUEST_CODE = 83;
    private final OnScreenSwitchListener onSwitch = screen -> {
        ImageView img_pos = findViewById(R.id.img_pos);
        int tot = ((HorizontalPager) findViewById(R.id.horizontal_pager)).getChildCount();
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
                } else {
                    img_pos.setImageResource(R.drawable.ttwo);
                }
                break;
            case 2:
                img_pos.setImageResource(R.drawable.three);
                break;
        }
        if (tot == 1) {
            img_pos.setVisibility(View.GONE);
        }
    };
    private final Integer[] remoteButtons = {R.id.btn_whome, R.id.btn_weject, R.id.btn_wsearch, R.id.btn_wpower, R.id.btn_rew, R.id.btn_pplay, R.id.btn_ff, R.id.btn_prev, R.id.btn_stop, R.id.btn_next, R.id.btn_home, R.id.btn_eject, R.id.btn_search, R.id.btn_power, R.id.btn_pgb, R.id.btn_config, R.id.btn_pgn, R.id.btn_audio, R.id.btn_subs, R.id.btn_mute, R.id.btn_a, R.id.btn_b, R.id.btn_c, R.id.btn_d, R.id.btn_up, R.id.btn_left, R.id.btn_ok, R.id.btn_right, R.id.btn_down, R.id.btn_back, R.id.btn_option};
    private final String[][] serviceList = ((String[][]) Array.newInstance(String.class,
            new int[]{50, 5}));
    long counterTime;
    private Boolean conf_shakecontrol;
    private Boolean running = false;
    private String sIP = "192.168.1.11";
    private ShakeListener shakeListener;
    private int actVol = 100;
    private AlertDialog alertKeyboard;
    private Boolean blue = false;
    private Boolean conf_buttons;
    private Boolean conf_trackball;
    private Boolean conf_vibrate;
    private Boolean conf_volumebuttons;    private final View.OnClickListener btnClick = new View.OnClickListener() {
        @SuppressLint("NonConstantResourceId")
        public void onClick(View v) {
            SeekBar sk_vol = findViewById(R.id.sk_vol);
            TextView txt_vol = findViewById(R.id.txt_vol);
            TextView txt_media = findViewById(R.id.txt_media);
            ImageButton btn_mode = findViewById(R.id.btn_mode);
            ImageButton btn_cvol = findViewById(R.id.btn_cvol);
            String tag = (String) v.getTag();
            if (tag != null && tag.startsWith("service_")) {
                sendCmdToDevice("s_" + serviceList[Integer.parseInt(tag.substring(8))][3]);
            }

            int viewId = v.getId();

            if (viewId == R.id.txt_time_current) {
                openTimeDialog();
                return;
            }

            if (viewId == R.id.btn_cvol) {
                sk_vol.setVisibility(View.GONE);
                txt_vol.setVisibility(View.GONE);
                btn_cvol.setVisibility(View.GONE);
                txt_media.setVisibility(View.VISIBLE);
                btn_mode.setVisibility(View.VISIBLE);
                return;
            }

            if (viewId == R.id.btn_mode) {
                switch (mPlaymode) {
                    case "NORMAL":
                        btn_mode.setImageResource(R.drawable.rone);
                        mPlaymode = "REPEAT_ONE";
                        return;
                    case "REPEAT_ONE":
                        btn_mode.setImageResource(R.drawable.rall);
                        mPlaymode = "REPEAT_ALL";
                        return;
                    case "REPEAT_ALL":
                        btn_mode.setImageResource(R.drawable.rrandom);
                        mPlaymode = "RANDOM";
                        return;
                    case "RANDOM":
                        btn_mode.setImageResource(R.drawable.rnormal);
                        mPlaymode = "NORMAL";
                        return;
                }
                btn_mode.setImageResource(R.drawable.rnormal);
                mPlaymode = "NORMAL";
                return;
            }

            if (viewId == R.id.btn_vol) {
                if (sk_vol.getVisibility() == View.GONE) {
                    sk_vol.setVisibility(View.VISIBLE);
                    txt_vol.setVisibility(View.VISIBLE);
                    btn_cvol.setVisibility(View.VISIBLE);
                    txt_media.setVisibility(View.GONE);
                    btn_mode.setVisibility(View.GONE);
                    return;
                }
                return;
            }

            if (viewId == R.id.btn_rew) {
                sendCmdToDevice("H");
                return;
            }

            if (viewId == R.id.btn_pplay) {
                sendCmdToDevice("p");
                if (!playing) {
                    stopRepeatingPauseTask();
                    if (!running) {
                        startRepeatingTask();
                        running = true;
                    }
                    playing = true;
                    return;
                }
                stopRepeatingTask();
                if (!paused) {
                    startRepeatingPauseTask();
                    paused = true;
                }
                playing = false;
                running = false;
                return;
            }

            if (viewId == R.id.btn_ff) {
                sendCmdToDevice("I");
                return;
            }

            if (viewId == R.id.btn_prev) {
                sendCmdToDevice("[");
                return;
            }

            if (viewId == R.id.btn_stop) {
                sendCmdToDevice("t");
                return;
            }

            if (viewId == R.id.btn_next) {
                sendCmdToDevice("]");
                return;
            }

            if (viewId == R.id.btn_wpower) {
                sendCmdToDevice("w");
                return;
            }

            if (viewId == R.id.btn_weject) {
                sendCmdToDevice("X");
                return;
            }

            if (viewId == R.id.btn_wsearch) {
                sendCmdToDevice("E");
                return;
            }

            if (viewId == R.id.btn_whome) {
                return;
            }

            if (viewId == R.id.btn_power) {
                sendCmdToDevice("w");
                return;
            }

            if (viewId == R.id.btn_eject) {
                sendCmdToDevice("X");
                return;
            }

            if (viewId == R.id.btn_search) {
                sendCmdToDevice("E");
                return;
            }

            if (viewId == R.id.btn_home) {
                sendCmdToDevice("o");
                return;
            }

            if (viewId == R.id.btn_pgb) {
                sendCmdToDevice("U");
                return;
            }

            if (viewId == R.id.btn_config) {
                sendCmdToDevice("s");
                return;
            }

            if (viewId == R.id.btn_pgn) {
                sendCmdToDevice("D");
                return;
            }

            if (viewId == R.id.btn_audio) {
                sendCmdToDevice(",");
                return;
            }

            if (viewId == R.id.btn_subs) {
                sendCmdToDevice("\\\\");
                return;
            }

            if (viewId == R.id.btn_mute) {
                sendCmdToDevice("M");
                return;
            }

            if (viewId == R.id.btn_a) {
                sendCmdToDevice("x");
                return;
            }

            if (viewId == R.id.btn_b) {
                sendCmdToDevice("y");
                return;
            }

            if (viewId == R.id.btn_c) {
                sendCmdToDevice("z");
                return;
            }

            if (viewId == R.id.btn_d) {
                sendCmdToDevice("A");
                return;
            }

            if (viewId == R.id.btn_up) {
                sendCmdToDevice("u");
                return;
            }

            if (viewId == R.id.btn_ok) {
                sendCmdToDevice("n");
                return;
            }

            if (viewId == R.id.btn_left) {
                sendCmdToDevice("l");
                return;
            }

            if (viewId == R.id.btn_right) {
                sendCmdToDevice("r");
                return;
            }

            if (viewId == R.id.btn_down) {
                sendCmdToDevice("d");
                return;
            }

            if (viewId == R.id.btn_back) {
                sendCmdToDevice("T");
                return;
            }

            if (viewId == R.id.btn_option) {
                sendCmdToDevice("G");
                return;
            }

            if (viewId == R.id.btn_touch) {
                ImageView img_gespa = findViewById(R.id.img_gespa);
                if (img_gespa.getVisibility() == View.INVISIBLE || img_gespa.getVisibility() == View.GONE) {
                    setPanel(true);
                    return;
                }
                setPanel(false);
                return;
            }

            if (viewId == R.id.btn_tinfo) {
                openGesHelpDialog();
            }

        }
    };
    private Integer cont = 0;
    private Boolean first = true;
    private GestureDetector gestureDetector;
    private final View.OnTouchListener GesDetected = (v, event) -> gestureDetector.onTouchEvent(
            event);
    private Boolean isTablet = false;
    private long lastplay;
    private Long lastshake = 0L;
    private long mLCurTime;
    private long mLTotTime;
    private final NumberPickerChangeListener nChangeListener = (picker, oldVal, newVal) -> {
        View v = picker.getRootView();
        String[] t = new PlaybackTimeFormatter().secToStringArray(mLTotTime);
        int h = Integer.parseInt(t[0]);
        int m = Integer.parseInt(t[1]);
        int s = Integer.parseInt(t[2]);
        NumberPicker nHor = v.findViewById(R.id.num_hor);
        NumberPicker nMin = v.findViewById(R.id.num_min);
        NumberPicker nSec = v.findViewById(R.id.num_sec);
        int pickerId = picker.getId();

        if (pickerId == R.id.num_hor) {
            int act = nMin.getCurrent();
            int niu = 59;
            if (nHor.getCurrent() >= h) {
                niu = m;
            }
            nMin.setRange(0, niu);
            if (act > niu) {
                nMin.setCurrent(niu);
            } else {
                nMin.setCurrent(act);
            }
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
            return;
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
                return;
            }
            nSec.setCurrent(act3);
        }
    };
    private final SeekBar.OnSeekBarChangeListener onSeek = new SeekBar.OnSeekBarChangeListener() {
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            int seekBarId = seekBar.getId();
            if (seekBarId == R.id.sk_play) {
                if (fromUser && mLTotTime != 0) {
                    String sTot = ((TextView) findViewById(R.id.txt_time_total)).getText()
                            .toString();
                    long lCur = (((long) progress) * new PlaybackTimeFormatter().stringToSec(sTot)) / 255;
                    mLCurTime = lCur;
                    setTimesTxtsUI(new PlaybackTimeFormatter().secToString(lCur), sTot);
                    return;
                }
                return;
            }
            if (seekBarId == R.id.sk_vol) {
                if (fromUser) {
                    setVolTxtIconIU(progress);
                    return;
                }
            }
        }

        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        public void onStopTrackingTouch(SeekBar seekBar) {
            if (seekBar.getId() == R.id.sk_play && mLTotTime != 0) {
                new PlaybackTimeFormatter().secToString(mLCurTime);
            }
        }
    };
    private String mPlaymode = "NORMAL";
    /* access modifiers changed from: private */
    private Handler m_handler;
    private Integer m_interval = 1000;
    private Handler mpause_handler;
    private Boolean onPause = false;
    private Boolean paused = false;
    private Boolean playing = false;
    private Boolean sKeyboard = false;
    private Integer sModelID = 4;
    private String sName = "WD TV";
    private Boolean sRemote = false;
    private Boolean sUpnp = false;
    private Boolean sizeds = true;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remotecontroller);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);


        this.counterTime = System.currentTimeMillis();
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        if (((float) metrics.widthPixels) / metrics.density >= 600.0f) {
            this.isTablet = true;
        }
        this.m_handler = new Handler();
        this.mpause_handler = new Handler();
        ((HorizontalPager) findViewById(R.id.horizontal_pager)).setOnScreenSwitchListener(
                this.onSwitch);
        this.sIP = "192.168.1.10";
        this.sModelID = 5;
        this.sName = "Test Device";
        this.sRemote = true;
        this.sKeyboard = true;
        this.sUpnp = true;
        for (Integer intValue : this.remoteButtons) {
            ImageButton btnC = findViewById(intValue);
            if (btnC != null) {
                btnC.setOnClickListener(this.btnClick);
            }
        }
        findViewById(R.id.btn_vol).setOnClickListener(this.btnClick);
        if (!this.isTablet) {
            findViewById(R.id.btn_cvol).setOnClickListener(this.btnClick);
        }
        findViewById(R.id.btn_mode).setOnClickListener(this.btnClick);
        findViewById(R.id.btn_touch).setOnClickListener(this.btnClick);
        findViewById(R.id.btn_tinfo).setOnClickListener(this.btnClick);
        findViewById(R.id.img_gespa).setVisibility(View.GONE);
        ((SeekBar) findViewById(R.id.sk_play)).setOnSeekBarChangeListener(this.onSeek);
        ((SeekBar) findViewById(R.id.sk_vol)).setOnSeekBarChangeListener(this.onSeek);
        findViewById(R.id.txt_time_current).setOnClickListener(this.btnClick);
        this.mLCurTime = 3600;
        this.mLTotTime = 9000;
        setTimesTxtsUI("01:00:00", "02:30:00");
        setTimeSeekUI(3600, 9000);
        setTitleTxtUI("Example media.avi");
        this.gestureDetector = new GestureDetector(new MyGestureDetector());
        findViewById(R.id.img_gespa).setOnTouchListener(this.GesDetected);
        setDeviceOptions(this.sModelID);
        ((ImageView) findViewById(R.id.img_pos)).setImageResource(R.drawable.tone);
        createLayout();
        getSupportActionBar().setTitle(this.sName);
        getSupportActionBar().setSubtitle(this.sIP);
        invalidateOptionsMenu();
        getPreferences();
        if (!getSharedPreferences("defaultUser", 0).getBoolean("uihelp_norepeat", false)) {
            openUiHelpDialog();
        }
    }

    /* access modifiers changed from: private */
    public void sizes() {
        HorizontalPager horizontalPager;
        if (this.sizeds) {
            this.sizeds = null;
            getWindowManager().getDefaultDisplay().getMetrics(new DisplayMetrics());
            if (!this.isTablet && getResources().getConfiguration().orientation != 1) {
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
            } else if (!this.isTablet) {
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
            } else if (this.isTablet && (horizontalPager = findViewById(
                    R.id.horizontal_pager)) != null) {
                LinearLayout ly_dots2 = findViewById(R.id.ly_dots);
                RelativeLayout.LayoutParams params4 = (RelativeLayout.LayoutParams) ly_dots2.getLayoutParams();
                params4.width = horizontalPager.getWidth();
                ly_dots2.setLayoutParams(params4);
            }
        }
    }

    private void setDeviceOptions(int modelID) {
        HorizontalPager horizontalPager = findViewById(R.id.horizontal_pager);
        if (modelID < 4) {
            return;
        }
        if (this.isTablet) {
            horizontalPager.removeViewAt(0);
        } else {
            horizontalPager.removeViewAt(1);
        }
    }    private Runnable m_pause = () -> {
        setTimePauseUI(false);
        DummyControllerActivity.this.mpause_handler.postDelayed(
                DummyControllerActivity.this.m_pause, 500);
    };

    private void createLayout() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout parent = findViewById(R.id.ly_one_swfour);
        int i = 0;
        while (i <= 14) {
            View custom = inflater.inflate(R.layout.row_service, null);
            ImageButton btn1 = custom.findViewById(R.id.btn_s_1);
            ImageButton btn2 = custom.findViewById(R.id.btn_s_2);
            ImageButton btn3 = custom.findViewById(R.id.btn_s_3);
            btn1.setTag("service_" + i);
            btn1.setImageResource(R.drawable.service);
            btn1.setOnClickListener(btnClick);
            btn2.setTag("service_" + (i + 1));
            btn2.setImageResource(R.drawable.service);
            btn2.setOnClickListener(btnClick);
            btn3.setTag("service_" + (i + 2));
            btn3.setImageResource(R.drawable.service);
            btn3.setOnClickListener(btnClick);
            int i2 = i + 2;
            if (parent != null) {
                parent.addView(custom);
            }
            System.gc();
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
                openTimeDialog();
                return true;

            case R.id.remotecontroller_activity_menu_keyboard:
                openKeyboardDialog();
                return true;

            case R.id.remotecontroller_activity_menu_webin:
                String url = "http://" + sIP;
                startActivity(new Intent("android.intent.action.VIEW", Uri.parse(url)));
                return false;

            case R.id.remotecontroller_activity_menu_preferences:
                startActivityForResult(
                        new Intent(DummyControllerActivity.this, SettingsActivity.class),
                        PREFERENCES_REQUEST_CODE);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.remotecontroller_activity_menu, menu);

        if (!sUpnp) {
            menu.removeItem(R.id.remotecontroller_activity_menu_jumpto);
        }
        if (!sKeyboard) {
            menu.removeItem(R.id.remotecontroller_activity_menu_keyboard);
        }
        if (!sRemote || !sKeyboard) {
            menu.removeItem(R.id.remotecontroller_activity_menu_webin);
        }

        return true;
    }

    public void sendCmdToDevice(String s) {
        if (!this.onPause) {
            ledflash();
        }
    }

    private void setPanel(Boolean setpanel) {
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
        if (setpanel) {
            btn_touch.setImageResource(R.drawable.arr);
            img_gespa.setVisibility(View.VISIBLE);
            btn_tinfo.setVisibility(View.VISIBLE);
            vis = View.INVISIBLE;
        } else {
            btn_touch.setImageResource(R.drawable.ges);
            img_gespa.setVisibility(View.INVISIBLE);
            btn_tinfo.setVisibility(android.view.View.INVISIBLE);
            vis = View.VISIBLE;
        }
        btn_up.setVisibility(vis);
        btn_left.setVisibility(vis);
        btn_ok.setVisibility(vis);
        btn_right.setVisibility(vis);
        btn_down.setVisibility(vis);
        btn_back.setVisibility(vis);
        btn_option.setVisibility(vis);
        System.gc();
    }

    private void ledflash() {
        runOnUiThread(() -> {
            TransitionDrawable transition = (TransitionDrawable) getResources().getDrawable(
                    R.drawable.ledflash);
            transition.startTransition(100);
            ((ImageView) findViewById(R.id.img_led)).setImageDrawable(transition);
        });
        if (conf_vibrate) {
            ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(50);
        }
    }

    private void setVolTxtIconIU(final int vol) {
        runOnUiThread(() -> {
            String svol = Integer.toString(vol);
            TextView txt_vol = findViewById(R.id.txt_vol);
            if (txt_vol != null) {
                txt_vol.setText(svol);
                int d = R.drawable.volh;
                if (vol > 66) {
                    d = R.drawable.volh;
                } else if (vol > 33) {
                    d = R.drawable.volm;
                } else if (vol > 0) {
                    d = R.drawable.voll;
                } else if (vol == 0) {
                    d = R.drawable.volz;
                }
                ((ImageButton) findViewById(R.id.btn_vol)).setImageResource(d);
            }
        });
    }

    private void setVolSeekUI(final int vol) {
        runOnUiThread(() -> {
            SeekBar sk_vol = findViewById(R.id.sk_vol);
            if (sk_vol != null) {
                sk_vol.setProgress(vol);
            }
        });
    }

    public void setTimesTxtsUI(final String cur, final String tot) {
        runOnUiThread(() -> {
            TextView txt_time_current = findViewById(R.id.txt_time_current);
            TextView txt_time_total = findViewById(R.id.txt_time_total);
            if (txt_time_current != null) {
                txt_time_current.setText(cur);
                txt_time_total.setText(tot);
            }
        });
    }

    /* access modifiers changed from: private */
    public void setTimeSeekUI(long cur, long tot) {
        final long j = tot;
        final long j2 = cur;
        runOnUiThread(() -> {
            int progress = 0;
            if (j != 0) {
                progress = (int) ((j2 * 255) / j);
                mLCurTime = j2;
            }
            SeekBar sk_play = findViewById(R.id.sk_play);
            if (sk_play != null) {
                sk_play.setProgress(progress);
            }
        });
    }

    public void setTitleTxtUI(final String tit) {
        runOnUiThread(() -> {
            TextView txt_media = findViewById(R.id.txt_media);
            if (txt_media != null) {
                txt_media.setText(tit);
            }
        });
    }    private final Runnable m_statusChecker = () -> {
        long var = System.currentTimeMillis() - m_interval;
        if (first || var < lastplay) {
            first = false;
        } else {
            lastplay = System.currentTimeMillis();
            if (cont <= 5 || m_interval != 1000) {
                sumTime();
                cont++;
            } else {
                sumTime();
                cont = 0;
            }
        }
        DummyControllerActivity.this.m_handler.postDelayed(
                DummyControllerActivity.this.m_statusChecker,
                DummyControllerActivity.this.m_interval);
    };

    public void openGesHelpDialog() {
        AlertDialog alertHelp = new AlertDialog.Builder(this).setTitle(
                        getString(R.string.rem_txt_gespan))
                .setView(LayoutInflater.from(this).inflate(R.layout.dialog_help_ges,
                        null)).setPositiveButton(getString(R.string.rem_txt_ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        }).create();
        alertHelp.setIcon(R.drawable.ic_menu_info_details);
        alertHelp.show();
    }

    public void openUiHelpDialog() {
        final View addView = LayoutInflater.from(this).inflate(R.layout.dialog_help_ui, null);
        AlertDialog alertHelp = new AlertDialog.Builder(this).setTitle(
                        getString(R.string.m_txt_help)).setView(addView)
                .setPositiveButton(getString(R.string.rem_txt_ok),
                        (dialog, whichButton) -> {
                        }).create();
        alertHelp.setIcon(R.drawable.ic_menu_info_details);
        alertHelp.show();
    }

    public void openKeyboardDialog() {
        final View addView = LayoutInflater.from(this).inflate(R.layout.dialog_keyboard, null);
        this.alertKeyboard = new AlertDialog.Builder(this).setTitle(
                        getString(R.string.rem_txt_keyboard)).setView(addView)
                .setPositiveButton(getString(R.string.rem_txt_send),
                        (dialog, whichButton) -> sendCmdToDevice(
                                "t_" + ((EditText) addView.findViewById(
                                        R.id.etx_text)).getText()))
                .setNegativeButton(getString(R.string.rem_txt_cancel),
                        (dialog, whichButton) -> {
                        }).create();
        this.alertKeyboard.setIcon(R.drawable.ic_keyboard);
        ImageButton btn_mic = addView.findViewById(R.id.btn_mic);
        if (this.sModelID >= 4) {
            addView.findViewById(R.id.txt_help).setVisibility(View.GONE);
        }
        if (!getPackageManager().queryIntentActivities(
                new Intent("android.speech.action.RECOGNIZE_SPEECH"), 0).isEmpty()) {
            btn_mic.setOnClickListener(v -> startVoiceRecognitionActivity());
        } else {
            btn_mic.setVisibility(View.GONE);
        }
        this.alertKeyboard.show();
    }

    /* access modifiers changed from: private */
    public void startVoiceRecognitionActivity() {
        Intent intent = new Intent("android.speech.action.RECOGNIZE_SPEECH");
        intent.putExtra("calling_package", getClass().getPackage().getName());
        intent.putExtra("android.speech.extra.LANGUAGE_MODEL", "free_form");
        intent.putExtra("android.speech.extra.MAX_RESULTS", 5);
        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
    }

    /* access modifiers changed from: protected */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == -1) {
            ArrayList<String> matches = data.getStringArrayListExtra(
                    "android.speech.extra.RESULTS");
            EditText etx_text = this.alertKeyboard.findViewById(R.id.etx_text);
            if (etx_text != null) {
                etx_text.setText(matches.get(0));
            }
        } else if (requestCode == PREFERENCES_REQUEST_CODE) {
            getPreferences();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void getPreferences() {
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean conf_screen = pref.getBoolean("conf_screen", false);
        this.conf_shakecontrol = pref.getBoolean("conf_shakecontrol", false);
        Boolean conf_deftouchpanel = pref.getBoolean("conf_deftouchpanel", false);
        this.conf_vibrate = pref.getBoolean("conf_vibrate", true);
        this.conf_trackball = pref.getBoolean("conf_trackball", true);
        this.conf_buttons = pref.getBoolean("conf_buttons", false);
        this.conf_volumebuttons = pref.getBoolean("conf_volumebuttons", true);
        //this.conf_bolbrig = Boolean.valueOf(pref.getBoolean("conf_bolbrig", true));
        if (conf_screen) {
            getWindow().addFlags(AccessibilityEventCompat.TYPE_VIEW_HOVER_ENTER);
        } else {
            getWindow().clearFlags(AccessibilityEventCompat.TYPE_VIEW_HOVER_ENTER);
        }
        if (!this.conf_shakecontrol && this.shakeListener != null) {
            shakeListener.shutdown();
            shakeListener = null;
        } else if (this.conf_shakecontrol && this.shakeListener == null) {
            shakeListener = new ShakeListener((SensorManager) getSystemService(Context.SENSOR_SERVICE));
            shakeListener.setForceThreshHold(1.9d);
            shakeListener.setShakeListenerCallback(() -> {
                if (lastshake != null) {
                    long var = System.currentTimeMillis() - lastshake;
                    if (conf_shakecontrol && var > 500) {
                        lastshake = System.currentTimeMillis();
                        sendCmdToDevice("p");
                    }
                }
            });
        }
        setPanel(conf_deftouchpanel);
    }

    public void openTimeDialog() {
        final View addView = LayoutInflater.from(this).inflate(R.layout.dialog_jump_to_time, null);
        NumberPicker nHor = addView.findViewById(R.id.num_hor);
        NumberPicker nMin = addView.findViewById(R.id.num_min);
        NumberPicker nSec = addView.findViewById(R.id.num_sec);
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
                            String a = time[0] + ":" + time[1] + ":" + time[2];
                            mLCurTime = new PlaybackTimeFormatter().stringToSec(a);
                            setTimeSeekUI(new PlaybackTimeFormatter().stringToSec(a), mLTotTime);
                            setTimesTxtsUI(a, new PlaybackTimeFormatter().secToString(mLTotTime));
                        }).setNegativeButton(getString(R.string.rem_txt_cancel),
                        (dialog, whichButton) -> {
                        }).create();
        alert.setIcon(R.drawable.ic_menu_time);
        String[] tt = new PlaybackTimeFormatter().secToStringArray(this.mLTotTime);
        int ht = Integer.parseInt(tt[0]);
        int mt = Integer.parseInt(tt[1]);
        int st = Integer.parseInt(tt[2]);
        String[] tc = new PlaybackTimeFormatter().secToStringArray(this.mLCurTime);
        int hc = Integer.parseInt(tc[0]);
        int mc = Integer.parseInt(tc[1]);
        int sc = Integer.parseInt(tc[2]);
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
        nHor.setOnChangeListener(this.nChangeListener);
        nMin.setOnChangeListener(this.nChangeListener);
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

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 19) {
            return this.conf_trackball;
        } else if (keyCode == 21) {
            if (!this.conf_trackball) {
                return false;
            }
            sendCmdToDevice("l");
            return true;
        } else if (keyCode == 23) {
            if (!this.conf_trackball) {
                return false;
            }
            sendCmdToDevice("n");
            return true;
        } else if (keyCode == 22) {
            if (!this.conf_trackball) {
                return false;
            }
            sendCmdToDevice("r");
            return true;
        } else if (keyCode == 20) {
            if (!this.conf_trackball) {
                return false;
            }
            sendCmdToDevice("d");
            return true;
        } else if (keyCode == 4) {
            if (!this.conf_buttons) {
                return super.onKeyDown(keyCode, event);
            }
            sendCmdToDevice("T");
            return true;
        } else if (keyCode == 84) {
            if (!this.conf_buttons) {
                return false;
            }
            sendCmdToDevice("G");
            return true;
        } else if (keyCode == 24) {
            if (this.conf_volumebuttons && this.actVol != 100) {
                this.actVol += 10;
                if (this.actVol > 100) {
                    this.actVol = 100;
                }
                setVolTxtIconIU(this.actVol);
                setVolSeekUI(this.actVol);
                ledflash();
                return true;
            } else return this.conf_volumebuttons;
        } else if (keyCode != 25) {
            return super.onKeyDown(keyCode, event);
        } else {
            if (this.conf_volumebuttons && this.actVol >= 0) {
                this.actVol -= 10;
                if (this.actVol < 0) {
                    this.actVol = 0;
                }
                setVolTxtIconIU(this.actVol);
                setVolSeekUI(this.actVol);
                ledflash();
                return true;
            } else return this.conf_volumebuttons;
        }
    }

    /* access modifiers changed from: private */
    public void sumTime() {
        Boolean rev = false;
        if (rev && this.mLCurTime == 0) {
            stopRepeatingTask();
        } else if (rev) {
            this.mLCurTime--;
        } else if (this.mLCurTime > this.mLTotTime) {
            this.mLCurTime = 0;
            stopRepeatingTask();
        } else {
            this.mLCurTime++;
        }
        setTimesTxtsUI(new PlaybackTimeFormatter().secToString(this.mLCurTime),
                new PlaybackTimeFormatter().secToString(this.mLTotTime));
    }

    /* access modifiers changed from: package-private */
    public void startRepeatingTask() {
        if (!this.running) {
            this.m_statusChecker.run();
            this.running = true;
        }
    }

    /* access modifiers changed from: package-private */
    public void stopRepeatingTask() {
        this.running = false;
        this.m_handler.removeCallbacks(this.m_statusChecker);
    }

    /* access modifiers changed from: package-private */
    public void startRepeatingPauseTask() {
        this.m_pause.run();
        this.paused = true;
    }

    /* access modifiers changed from: package-private */
    public void stopRepeatingPauseTask() {
        if (this.mpause_handler != null) {
            this.mpause_handler.removeCallbacks(this.m_pause);
            this.paused = false;
            setTimePauseUI(true);
        }
    }

    private void setTimePauseUI(final boolean isFinal) {
        runOnUiThread(() -> {
            TextView txt_time_current = findViewById(R.id.txt_time_current);
            if (txt_time_current != null) {
                if (!blue) {
                    txt_time_current.setTextColor(Color.parseColor("#33b5e5"));
                    blue = true;
                } else {
                    txt_time_current.setTextColor(-1);
                    blue = false;
                }
                if (isFinal) {
                    txt_time_current.setTextColor(-1);
                    blue = false;
                }
            }
        });
    }

    protected void onDestroy() {
        super.onDestroy();
        m_handler.removeCallbacks(m_statusChecker);
        mpause_handler.removeCallbacks(m_pause);
        if (shakeListener != null) {
            shakeListener.shutdown();
        }
    }

    protected void onPause() {
        super.onPause();
        onPause = true;
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        getWindow().setFormat(1);
    }

    protected void onStart() {
        new FirstRunTask().execute();
        super.onStart();
    }

    protected void onResume() {
        this.onPause = false;
        super.onResume();
    }

    class FirstRunTask extends AsyncTask<String, Integer, Void> {
        protected Void doInBackground(String... target) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            runOnUiThread(DummyControllerActivity.this::sizes);
            return null;
        }
    }

    class MyGestureDetector extends GestureDetector.SimpleOnGestureListener {
        MyGestureDetector() {
        }

        public boolean onDoubleTap(MotionEvent ev) {
            flash(findViewById(R.id.btn_back));
            sendCmdToDevice("T");
            return false;
        }

        public boolean onSingleTapConfirmed(MotionEvent ev) {
            flash(findViewById(R.id.btn_ok));
            sendCmdToDevice("n");
            return false;
        }

        public void onLongPress(MotionEvent ev) {
            flash(findViewById(R.id.btn_option));
            sendCmdToDevice("G");
        }

        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (Math.abs(e2.getRawY() - e1.getRawY()) > Math.abs(e2.getRawX() - e1.getRawX())) {
                if (e2.getRawY() - e1.getRawY() > 0.0f) {
                    flash(findViewById(R.id.btn_down));
                    sendCmdToDevice("d");
                    if (Math.abs(velocityY) <= 1000.0f) {
                        return false;
                    }
                    sendCmdToDevice("d");
                    if (Math.abs(velocityY) <= 2000.0f) {
                        return false;
                    }
                    sendCmdToDevice("d");
                    return false;
                }
                flash(findViewById(R.id.btn_up));
                if (Math.abs(velocityY) <= 1000.0f) {
                    return false;
                }
                if (Math.abs(velocityY) <= 2000.0f) {
                    return false;
                }
                return false;
            } else if (e2.getRawX() - e1.getRawX() > 0.0f) {
                flash(findViewById(R.id.btn_right));
                sendCmdToDevice("r");
                if (Math.abs(velocityX) <= 1000.0f) {
                    return false;
                }
                sendCmdToDevice("r");
                if (Math.abs(velocityX) <= 2000.0f) {
                    return false;
                }
                sendCmdToDevice("r");
                return false;
            } else {
                flash(findViewById(R.id.btn_left));
                sendCmdToDevice("l");
                if (Math.abs(velocityX) <= 1000.0f) {
                    return false;
                }
                sendCmdToDevice("l");
                if (Math.abs(velocityX) <= 2000.0f) {
                    return false;
                }
                sendCmdToDevice("l");
                return false;
            }
        }
    }

}
