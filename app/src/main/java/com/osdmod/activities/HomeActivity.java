package com.osdmod.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.PixelFormat;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.osdmod.adapters.WdDeviceListAdapter;
import com.osdmod.model.WdDevice;
import com.osdmod.model.WdDeviceRepository;
import com.osdmod.prefs.WDPrefs;
import com.osdmod.remote.BrowserUpnpService;
import com.osdmod.remote.GetInfoFromLx;
import com.osdmod.remote.R;

import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.types.UDAServiceType;
import org.teleal.cling.registry.DefaultRegistryListener;
import org.teleal.cling.registry.Registry;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";
    private static final int EDIT_REQUEST_CODE = 2123;
    private static final int PREFERENCES_REQUEST_CODE = 1123;
    private final List<WdDevice> listOfNewDevices = new ArrayList<>();
    private final List<WdDevice> listOfSavedDevices = new ArrayList<>();
    private final BrowseRegistryListener registryListener = new BrowseRegistryListener();
    private long lastost = 0;
    private ListView listNew;
    private ListView listSaved;
    private ViewGroup ly_emptyfav;
    private ViewGroup ly_loadingfav;
    private TextView newlist_empty;
    private ViewGroup ly_searching;
    private MenuInflater menuInflater;
    private boolean searching = false;
    private WdDeviceRepository wdDeviceRepository;
    private AndroidUpnpService upnpService;
    private boolean firstRun = true;
    private WdDeviceListAdapter adapterNew;
    private WdDeviceListAdapter adapterSaved;
    private final View.OnClickListener btnClick = v -> {
        int viewId = v.getId();
        if (viewId == R.id.btn_hrefresh) {
            runDevicesDiscovery(false, true);
            return;
        }

        if (viewId == R.id.btn_hadd) {
            startCreateNewDeviceActivity();
            return;
        }
    };

    private void runDevicesDiscovery(boolean force, boolean interactive) {
        if (searching) {
            return;
        }

        boolean wifiConnected = force || checkNetworkStatus();
        if (!wifiConnected) {
            if (interactive) {
                openWifiErrorDialog(
                        getString(R.string.err_tit_nowifi),
                        getString(R.string.err_txt_nowifi));
            } else {
                showToastLong(getString(R.string.m_txt_nowifi));
            }
            return;
        }

        runOnUiThread(HomeActivity.this::discoverDevicesOnNetwork);
    }

    private void startCreateNewDeviceActivity() {
        startEditActivityHelper(false, -1);
    }

    private void startEditDeviceActivity(int deviceId) {
        startEditActivityHelper(true, deviceId);
    }

    private void startEditActivityHelper(boolean edit, int deviceId) {
        Intent i = new Intent(HomeActivity.this, EditDeviceActivity.class);
        i.putExtra("action", edit ? "edit" : "newCreate");
        if (edit) {
            i.putExtra("id", deviceId);
        }
        startActivityForResult(i, HomeActivity.EDIT_REQUEST_CODE);

    }

    private void clearAppCache() {
        try {
            File cacheDir = getApplicationContext().getCacheDir();

            if (cacheDir != null && cacheDir.isDirectory()) {
                File[] files = cacheDir.listFiles();
                assert files != null;
                for (File file : files) {
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                }
            }
        } catch (Exception e) {
            Log.d(TAG, e.getMessage(), e);
        }
    }

    private void loadFavoritesDevicesList() {
        listOfSavedDevices.clear();

        List<WdDevice> favoriteDevices = wdDeviceRepository.retrieveAll();
        //TODO SCF check if device online/connected
        listOfSavedDevices.addAll(favoriteDevices);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Objects
        wdDeviceRepository = new WdDeviceRepository(getApplicationContext());
        adapterNew = new WdDeviceListAdapter(this, listOfNewDevices);
        adapterSaved = new WdDeviceListAdapter(this, listOfSavedDevices);
        menuInflater = getMenuInflater();

        // Layout
        setContentView(R.layout.activity_home);
        getWindow().setFormat(PixelFormat.RGBA_8888);

        // Discovered devices view
        newlist_empty = findViewById(R.id.newlist_empty);
        listNew = findViewById(R.id.newlist);
        listNew.setClickable(true);
        listNew.setOnItemClickListener((adapterView, view, position, index) -> {
            openContextMenu(view);
        });
        listNew.setAdapter(adapterNew);
        registerForContextMenu(listNew);

        // Favorites devices view
        ly_searching = findViewById(R.id.ly_searching);
        listSaved = findViewById(R.id.savedlist);
        ly_emptyfav = findViewById(R.id.ly_emptyfav);
        ly_loadingfav = findViewById(R.id.ly_loadingfav);
        listSaved.setClickable(true);
        listSaved.setOnItemClickListener((adapterView, view, position, index) -> {
            WdDevice device = listOfSavedDevices.get(position);
            if (device.isConnected()) {
                connectToDevice(device);
            } else {
                openContextMenu(view);
            }
        });
        listSaved.setAdapter(adapterSaved);
        registerForContextMenu(listSaved);

        findViewById(R.id.btn_hrefresh).setOnClickListener(btnClick);
        findViewById(R.id.btn_hadd).setOnClickListener(btnClick);

        invalidateOptionsMenu();
    }

    private void connectToDevice(WdDevice device) {
        Intent intentConnect = new Intent(getBaseContext(), RemoteControllerActivity.class);
        intentConnect.putExtra(RemoteControllerActivity.INTENT_DEVICE, device);
        startActivity(intentConnect);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo());
        if (info == null) {
            return false;
        }

        ListView lv = (ListView) info.targetView.getParent();
        if (lv == null) {
            return false;
        }

        int position = info.position;
        if (position == -1) {
            return false;
        }

        WdDevice device = (WdDevice) lv.getAdapter().getItem(position);

        switch (item.getItemId()) {
            case R.id.home_new_contextm_info:
                openInfoDialog(device);
                return true;

            case R.id.home_new_contextm_connect:
            case R.id.home_saved_contextm_fconnect:
                connectToDevice(device);
                return true;

            case R.id.home_new_contextm_favorite:
                updateLists();
                new AddNewDeviceToFavoritesTask().execute(new WdDevice[]{device});
                return true;

            case R.id.home_saved_contextm_edit:
                startEditDeviceActivity(device.getDeviceId());
                return true;

            case R.id.home_saved_contextm_delete:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.m_txt_delete)
                        .setPositiveButton(R.string.m_txt_ok, (dialog, id) -> {
                            new RemoveDeviceFromFavoritesTask().execute(device);
                        })
                        .setNegativeButton(R.string.m_txt_cancel, (dialog, id) -> {
                        });
                builder.show();
                return true;

            default:
                return false;
        }


    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case EDIT_REQUEST_CODE:
                if (resultCode == 0) {
                    adapterSaved.notifyDataSetChanged();
                } else if (resultCode == -1) {
                    new LoadFavouritesTask().execute();
                }
                break;

            case PREFERENCES_REQUEST_CODE:
                break;
        }
    }

    private void openWifiErrorDialog(String title2, String msg) {
        new AlertDialog.Builder(this).setIcon(R.drawable.appicon).setTitle(title2).setMessage(msg)
                .setPositiveButton(getString(R.string.err_bacp_nowifi),
                        (dialog, which) -> runDevicesDiscovery(true, false))
                .setNeutralButton(getString(R.string.err_bopt_nowifi),
                        (dialog, which) -> startActivity(
                                new Intent("android.settings.WIFI_SETTINGS")))
                .setNegativeButton(getString(R.string.err_bcan_nowifi),
                        (dialog, which) -> {
                        })
                .show();
    }

    public void openInfoDialog(WdDevice device) {
        View addView = LayoutInflater.from(this).inflate(R.layout.dialog_dinfo, null);
        AlertDialog alertInfo = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.m_txt_devinf))
                .setView(addView)
                .setPositiveButton(getString(R.string.m_txt_ok), (dialog, whichButton) -> {
                })
                .create();
        alertInfo.setIcon(R.drawable.ic_menu_info_details);
        ((ImageView) addView.findViewById(R.id.img_logo)).setImageResource(
                device.getDeviceDrawable(true));
        ((TextView) addView.findViewById(R.id.txt_model)).setText(device.getModelName());
        ((TextView) addView.findViewById(R.id.txt_name)).setText(device.getFriendlyName());
        ((TextView) addView.findViewById(R.id.txt_ip)).setText(device.getIp());
        ((TextView) addView.findViewById(R.id.txt_uuid)).setText(device.getUuid());
        TableRow row = addView.findViewById(R.id.row_wdlxtv);
        TableRow row2 = addView.findViewById(R.id.row_login);
        if (device.getModelID() < 4) {
            ImageView img = addView.findViewById(R.id.img_wdlxtv);
            if (device.iswDlxTVFirmware()) {
                img.setImageResource(R.drawable.ic_bok);
            } else {
                img.setImageResource(R.drawable.ic_cancel);
            }
            ((ImageView) addView.findViewById(R.id.img_login)).setImageResource(
                    device.isConnected() ? R.drawable.ic_bok : R.drawable.ic_cancel);
            row.setVisibility(View.VISIBLE);
            row2.setVisibility(View.VISIBLE);
        } else {
            row.setVisibility(View.GONE);
            row2.setVisibility(View.GONE);
        }
        ImageView img3 = addView.findViewById(R.id.img_remote);
        if (device.isRemoteControlAvailable()) {
            img3.setImageResource(R.drawable.ic_bok);
        } else {
            img3.setImageResource(R.drawable.ic_cancel);
        }
        ImageView img4 = addView.findViewById(R.id.img_keyboard);
        if (device.isKeyboardAvailable()) {
            img4.setImageResource(R.drawable.ic_bok);
        } else {
            img4.setImageResource(R.drawable.ic_cancel);
        }
        ImageView img5 = addView.findViewById(R.id.img_upnp);
        if (device.isUpnp()) {
            img5.setImageResource(R.drawable.ic_bok);
        } else {
            img5.setImageResource(R.drawable.ic_cancel);
        }
        alertInfo.show();
    }

    private void openHelpDialog() {
        new AlertDialog.Builder(this).setIcon(R.drawable.ic_menu_help)
                .setTitle(getString(R.string.edia_txt_help))
                .setView(linkifyText(getString(R.string.d_help)))
                .setPositiveButton(getString(R.string.rem_txt_ok),
                        (dialog, which) -> {
                        }).show();
    }

    private ScrollView linkifyText(String message) {
        ScrollView svMessage = new ScrollView(this);
        TextView tvMessage = new TextView(this);
        tvMessage.setText(Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY));
        tvMessage.setMovementMethod(LinkMovementMethod.getInstance());
        tvMessage.setTextColor(-1);
        svMessage.setPadding(14, 2, 10, 12);
        svMessage.addView(tvMessage);
        return svMessage;
    }

    private boolean checkNetworkStatus() {
        return ((ConnectivityManager) getSystemService(
                Context.CONNECTIVITY_SERVICE)).getNetworkInfo(1).isConnected();
    }

    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        ListView lv = (ListView) info.targetView.getParent();
        menuInflater.inflate(
                (lv.getId() == R.id.newlist) ? R.menu.home_activity_new_contextmenu : R.menu.home_activity_saved_contextmenu,
                menu);
    }

    @SuppressLint("NonConstantResourceId")
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            case R.id.home_activity_menu_refresh:
                runDevicesDiscovery(false, true);
                return true;

            case R.id.home_activity_menu_add:
                startCreateNewDeviceActivity();
                return true;

            case R.id.home_activity_menu_prefs:
                startActivityForResult(
                        new Intent(HomeActivity.this, SettingsActivity.class),
                        HomeActivity.PREFERENCES_REQUEST_CODE);
                return true;

            case R.id.home_activity_menu_testapp:
                startActivity(new Intent(getBaseContext(), DummyControllerActivity.class));
                return true;

            case R.id.home_activity_menu_help:
                openHelpDialog();
                return true;

            case R.id.home_activity_menu_about:
                openAboutDialog();
                return true;

            default:
                return false;
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        menuInflater.inflate(R.menu.home_activity_menu, menu);
        return true;
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        getWindow().setFormat(1);
    }

    private void openAboutDialog() {
        new AlertDialog.Builder(this).setIcon(R.drawable.ic_menu_info_details)
                .setTitle(getString(R.string.m_about_tit))
                .setView(linkifyText(getString(R.string.m_about_txt)))
                .setPositiveButton(getString(R.string.b_pref_ok),
                        (dialog, which) -> {
                        }).show();
    }

    private void discoverDevicesOnNetwork() {
        if (upnpService == null) {
            runOnUiThread(() -> Toast.makeText(this,
                    getString(R.string.rem_txt_upnpserviceerror), Toast.LENGTH_LONG).show());
            return;
        }

        searching = true;
        ly_searching.setVisibility(View.VISIBLE);
        try {
            upnpService.getRegistry().removeAllRemoteDevices();
            upnpService.getControlPoint().search();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            try {
                upnpService.getRegistry().shutdown();
            } catch (Exception ignored) {
            } finally {
                upnpService = null;
                runOnUiThread(() -> Toast.makeText(this,
                        getString(R.string.rem_txt_upnpserviceerror), Toast.LENGTH_LONG).show());
            }
        }

        new CountDownTimer(
                (new WDPrefs(getApplicationContext()).getDiscoveryTimeoutSeconds()) * 1000L, 1000) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                searching = false;
                ly_searching.setVisibility(View.GONE);
                updateLists();
            }
        }.start();
    }

    private void updateLists() {
        adapterNew.notifyDataSetChanged();
        adapterSaved.notifyDataSetChanged();
        listNew.setVisibility(listOfNewDevices.isEmpty() ? View.GONE : View.VISIBLE);
        ly_loadingfav.setVisibility(View.GONE);

        if (listOfSavedDevices.isEmpty()) {
            listSaved.setVisibility(View.GONE);
            ly_emptyfav.setVisibility(View.VISIBLE);
        } else {
            listSaved.setVisibility(View.VISIBLE);
            ly_emptyfav.setVisibility(View.GONE);
        }

        if (!listOfNewDevices.isEmpty()) {
            newlist_empty.setVisibility(View.GONE);
        } else {
            newlist_empty.setVisibility(View.VISIBLE);
        }

        runOnUiThread(() -> {
            listNew.invalidateViews();
            listSaved.invalidateViews();
        });
    }

    private void showToastLong(final String val) {
        if (System.currentTimeMillis() - this.lastost > 3500) {
            runOnUiThread(() -> Toast.makeText(HomeActivity.this, val, Toast.LENGTH_LONG).show());
            this.lastost = System.currentTimeMillis();
        }
    }

    private int checkIfOnSavedList(String uuid) {
        for (int i = 0; i <= listOfSavedDevices.size() - 1; i++) {
            if (listOfSavedDevices.get(i).getUuid().equals(uuid)) {
                return i;
            }
        }
        return -1;
    }

    private int checkIfOnNewList(String uuid) {
        for (int i = 0; i <= listOfNewDevices.size() - 1; i++) {
            if (listOfNewDevices.get(i).getUuid().equals(uuid)) {
                return i;
            }
        }
        return -1;
    }

    protected void onStart() {
        if (firstRun) {
            firstRun = false;
            new UpnpServiceRunTask().execute();
            new LoadFavouritesTask().execute();
        }
        super.onStart();
    }

    protected void onStop() {
        super.onStop();
        listSaved.setVisibility(View.GONE);
    }

    protected void onRestart() {
        super.onRestart();
        listSaved.setVisibility(listOfSavedDevices.isEmpty() ? View.GONE : View.VISIBLE);
    }

    protected void onResume() {
        super.onResume();
        listSaved.setVisibility(listOfSavedDevices.isEmpty() ? View.GONE : View.VISIBLE);
    }

    protected void onDestroy() {
        super.onDestroy();

        if (upnpService != null) {
            try {
                upnpService.getRegistry().shutdown();
            } catch (Exception ignored) {
            }
        }

        clearAppCache();
    }

    private class UpnpServiceRunTask extends AsyncTask<Void, Integer, Void> {
        protected Void doInBackground(Void... target) {
            ServiceConnection serviceConnection = new ServiceConnection() {
                public void onServiceConnected(ComponentName className, IBinder service) {
                    upnpService = (AndroidUpnpService) service;
                    upnpService.getRegistry().addListener(registryListener);
                }

                public void onServiceDisconnected(ComponentName className) {
                    upnpService = null;
                }
            };

            getApplicationContext()
                    .bindService(new Intent(HomeActivity.this, BrowserUpnpService.class),
                            serviceConnection, 1);
            return null;
        }
    }

    protected class BrowseRegistryListener extends DefaultRegistryListener {
        public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {
            deviceAdded(device);
        }

        public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
            deviceAdded(device);
        }

        private void deviceAdded(RemoteDevice device) {
            Service service = device.findService(new UDAServiceType("RenderingControl"));
            if (service == null || service.getAction("GetVolume") == null || service.getAction(
                    "SetVolume") == null || service.getAction("GetMute") == null) {
                return;
            }

            if (searching) {
                // Discoverable device found
                new NewDeviceFoundTask().execute(device);
                return;
            }

            String uuid = device.getIdentity().getUdn().getIdentifierString();
            int positionInSavedList = checkIfOnSavedList(uuid);
            if (positionInSavedList != -1 && !listOfSavedDevices.get(positionInSavedList)
                    .isConnected()) {
                positionInSavedList = -1;
            }

            int positionInNewList = checkIfOnNewList(uuid);
            if (positionInSavedList == -1 && positionInNewList == -1) {
                new NewDeviceFoundTask().execute(device);
            }
        }
    }

    private class NewDeviceFoundTask extends AsyncTask<RemoteDevice, Void, Void> {

        protected Void doInBackground(RemoteDevice... device) {
            if (!device[0].getDetails().getModelDetails().getModelName()
                    .contains("WD TV") && !device[0].getIdentity().getUdn()
                    .getIdentifierString().equals(WdDevice.DEFAULT_UUID)) {
                return null;
            }

            showToastLong(getString(R.string.m_txt_wdtvfound));

            boolean wdlxtv;
            String user;
            String password;
            boolean connected;
            boolean remoteControlAvailable;
            boolean keyboard;
            boolean upnp;
            String friendlyName = device[0].getDetails().getFriendlyName();
            String ip = device[0].getIdentity().getDescriptorURL().toString()
                    .replace("http://", "");
            ip = ip.substring(0, ip.indexOf(":"));
            String modelName = device[0].getDetails().getModelDetails()
                    .getModelName() + " " + device[0].getDetails().getModelDetails()
                    .getModelDescription();

            if (modelName.contains("WD TV")) {
                int modelID = WdDevice.getModelIDFromString(modelName);
                modelName = WdDevice.gentModelStringFromModelID(modelID);
                if (modelID == WdDevice.MODELID_HUB || modelID == WdDevice.MODELID_STREAMING) {
                    String[] respHub = new GetInfoFromLx().getHubConfig(ip);
                    wdlxtv = false;
                    user = WdDevice.DEFAULT_USERNAME;
                    password = WdDevice.DEFAULT_PASSWORD;
                    connected = true;
                    upnp = true;
                    remoteControlAvailable = Boolean.parseBoolean(respHub[0]);
                    keyboard = Boolean.parseBoolean(respHub[1]);
                } else {
                    String[] resp = new GetInfoFromLx().getLiveConfig(ip, WdDevice.DEFAULT_USERNAME,
                            WdDevice.DEFAULT_PASSWORD);
                    wdlxtv = Boolean.parseBoolean(resp[0]);
                    user = resp[1];
                    password = resp[2];
                    //login = Boolean.parseBoolean(resp[3]);
                    connected = Boolean.parseBoolean(resp[4]);
                    remoteControlAvailable = Boolean.parseBoolean(resp[5]);
                    keyboard = Boolean.parseBoolean(resp[6]);
                    upnp = Boolean.parseBoolean(resp[7]);
                }
            } else {
                String[] resp2 = new GetInfoFromLx().getGen12Config(ip);
                modelName = resp2[0];
                wdlxtv = Boolean.parseBoolean(resp2[1]);
                user = resp2[2];
                password = resp2[3];
                //login = Boolean.parseBoolean(resp2[4]);
                connected = Boolean.parseBoolean(resp2[5]);
                remoteControlAvailable = Boolean.parseBoolean(resp2[6]);
                keyboard = Boolean.parseBoolean(resp2[7]);
                upnp = Boolean.parseBoolean(resp2[8]);
            }

            WdDevice wdDevice = new WdDevice(modelName, friendlyName, ip, device[0].getIdentity()
                    .getUdn().getIdentifierString(), wdlxtv, user, password, remoteControlAvailable,
                    keyboard, upnp,
                    connected);

            // Add discovered device to UI
            int positionInSavedList = checkIfOnSavedList(wdDevice.getUuid());
            if (positionInSavedList != -1) {
                // Favorited, keep friendlyName given by user and deviceId to avoid ListView change of position
                WdDevice savedDevice = listOfSavedDevices.get(positionInSavedList);
                wdDevice.setDeviceId(savedDevice.getDeviceId());
                wdDevice.setFriendlyName(savedDevice.getFriendlyName());
                listOfSavedDevices.set(positionInSavedList, wdDevice);
                runOnUiThread(() -> new UpdateFavoriteNoListRefreshTask().execute(
                        new WdDevice[]{wdDevice}));
            }

            int positionInNewList = checkIfOnNewList(wdDevice.getUuid());
            if (positionInNewList == -1) {
                listOfNewDevices.add(wdDevice);
            } else {
                listOfNewDevices.set(positionInNewList, wdDevice);
            }
            return null;
        }

        protected void onPostExecute(Void result) {
            updateLists();
        }
    }

    private class LoadFavouritesTask extends AsyncTask<Void, Integer, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ly_emptyfav.setVisibility(View.GONE);
            ly_loadingfav.setVisibility(View.VISIBLE);
        }

        protected Void doInBackground(Void... filename) {
            loadFavoritesDevicesList();
            return null;
        }

        protected void onPostExecute(Void result) {
            updateLists();
        }
    }

    private class AddNewDeviceToFavoritesTask extends AsyncTask<WdDevice[], Void, WdDevice> {

        protected WdDevice doInBackground(WdDevice[]... param) {
            WdDevice wdDevice = param[0][0];

            int positionInSavedList = checkIfOnSavedList(wdDevice.getUuid());
            if (positionInSavedList != -1) {
                // Favorited, keep friendlyName given by user and deviceId to avoid ListView change of position
                WdDevice savedDevice = listOfSavedDevices.get(positionInSavedList);
                wdDevice.setDeviceId(savedDevice.getDeviceId());
                wdDevice.setFriendlyName(savedDevice.getFriendlyName());
            }

            wdDeviceRepository.add(wdDevice);
            loadFavoritesDevicesList();

            return wdDevice;
        }

        protected void onPostExecute(WdDevice device) {
            //TODO SCF esta movida se puede quitar si al cargar favoritos compruebo si está online o no
            int pos = listOfSavedDevices.indexOf(device);
            if (pos != -1) {
                WdDevice retrieved = listOfSavedDevices.get(pos);
                retrieved.setConnected(true);
                listOfSavedDevices.set(pos, retrieved);
            }
            updateLists();
        }
    }

    private class UpdateFavoriteNoListRefreshTask extends AsyncTask<WdDevice[], Void, Void> {

        protected Void doInBackground(WdDevice[]... param) {
            WdDevice wdDevice = param[0][0];
            wdDeviceRepository.update(wdDevice);
            return null;
        }
    }

    private class RemoveDeviceFromFavoritesTask extends AsyncTask<WdDevice, Void, Void> {

        protected Void doInBackground(WdDevice... deviceId) {
            wdDeviceRepository.delete(deviceId[0].getDeviceId());

            loadFavoritesDevicesList();
            return null;
        }

        protected void onPostExecute(Void result) {
            updateLists();
        }
    }
}
