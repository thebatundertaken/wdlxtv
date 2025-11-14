package com.osdmod.android.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.PixelFormat;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
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
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.osdmod.android.adapters.WdDeviceListAdapter;
import com.osdmod.android.prefs.WDPrefs;
import com.osdmod.model.WdDevice;
import com.osdmod.model.WdDeviceRepository;
import com.osdmod.remote.R;
import com.osdmod.remote.WDTVHDGen1;
import com.osdmod.remote.WDTVLiveHub;
import com.osdmod.remote.WDTVLivePlus;
import com.osdmod.remote.WdRemoteController;
import com.osdmod.service.UpnpDiscoveryService;
import com.osdmod.utils.TextUtils;

import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.types.UDAServiceType;
import org.teleal.cling.registry.DefaultRegistryListener;
import org.teleal.cling.registry.Registry;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
                showToastShort(getString(R.string.m_txt_nowifi));
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
                new Thread(new AddNewDeviceToFavoritesTask(device)).start();
                return true;

            case R.id.home_saved_contextm_edit:
                startEditDeviceActivity(device.getDeviceId());
                return true;

            case R.id.home_saved_contextm_delete:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.m_txt_delete)
                        .setPositiveButton(R.string.m_txt_ok, (dialog, id) -> {
                            new Thread(new RemoveDeviceFromFavoritesTask(device)).start();
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
                    new Thread(new LoadFavouritesTask()).start();
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
                .setView(TextUtils.linkifyText(getString(R.string.d_help), this))
                .setPositiveButton(getString(R.string.rem_txt_ok),
                        (dialog, which) -> {
                        }).show();
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
                .setView(TextUtils.linkifyText(getString(R.string.m_about_txt), this))
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

    private void showToastShort(final String val) {
        if (System.currentTimeMillis() - lastost > 3500) {
            runOnUiThread(() -> Toast.makeText(HomeActivity.this, val, Toast.LENGTH_SHORT).show());
            lastost = System.currentTimeMillis();
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
        super.onStart();

        if (firstRun) {
            firstRun = false;
            new Thread(new UpnpServiceRunTask()).start();
            new Thread(new LoadFavouritesTask()).start();
            new Thread(new RunDevicesDiscoveryTask()).start();
        }
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

    private class UpnpServiceRunTask implements Runnable {

        @Override
        public void run() {
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
                    .bindService(new Intent(HomeActivity.this, UpnpDiscoveryService.class),
                            serviceConnection, 1);
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
            Service<?, ?> service = device.findService(new UDAServiceType("RenderingControl"));
            if (service == null || service.getAction("GetVolume") == null || service.getAction(
                    "SetVolume") == null || service.getAction("GetMute") == null) {
                return;
            }

            if (searching) {
                // Discoverable device found
                new Thread(new NewDeviceFoundTask(device)).start();
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
                new Thread(new NewDeviceFoundTask(device)).start();
            }
        }
    }

    private class NewDeviceFoundTask implements Runnable {
        private final RemoteDevice device;

        private NewDeviceFoundTask(RemoteDevice device) {
            this.device = device;
        }

        @Override
        public void run() {
            if (!device.getDetails().getModelDetails().getModelName()
                    .contains("WD TV") && !device.getIdentity().getUdn()
                    .getIdentifierString().equals(WdDevice.DEFAULT_UUID)) {
                return;
            }

            showToastShort(getString(R.string.m_txt_wdtvfound));

            String friendlyName = device.getDetails().getFriendlyName();
            String ip = device.getIdentity().getDescriptorURL().toString()
                    .replace("http://", "");
            ip = ip.substring(0, ip.indexOf(":"));
            String rawModelName = device.getDetails().getModelDetails()
                    .getModelName() + " " + device.getDetails().getModelDetails()
                    .getModelDescription();

            WdRemoteController wdRemoteController;
            int modelID = WdDevice.getModelIDFromString(rawModelName);
            if (modelID == WdDevice.MODELID_HUB || modelID == WdDevice.MODELID_STREAMING) {
                wdRemoteController = new WDTVLiveHub(ip);
            } else if (modelID == WdDevice.MODELID_PLUS) {
                wdRemoteController = new WDTVLivePlus(ip);
            } else {
                wdRemoteController = new WDTVHDGen1(ip);
            }
            String modelName = WdDevice.gentModelStringFromModelID(modelID);
            Map<String, Object> respHub = wdRemoteController.getInfo();
            //noinspection DataFlowIssue
            boolean remoteControlAvailable = (boolean) respHub.get(
                    WdRemoteController.INFO_REMOTECONTROL_AVAILABLE);
            //noinspection DataFlowIssue
            boolean keyboard = (boolean) respHub.get(WdRemoteController.INFO_KEYBOARD_AVAILABLE);
            //noinspection DataFlowIssue
            boolean wdlxtv = (boolean) respHub.get(WdRemoteController.INFO_WDLXTV_FIRMWARE);
            String user = (String) respHub.get(WdRemoteController.INFO_USERNAME);
            String password = (String) respHub.get(WdRemoteController.INFO_PASSWORD);
            //noinspection DataFlowIssue
            boolean connected = (boolean) respHub.get(WdRemoteController.INFO_CONNECTED);
            //noinspection DataFlowIssue
            boolean upnp = (boolean) respHub.get(WdRemoteController.INFO_UPNP);

            WdDevice wdDevice = new WdDevice(modelName, friendlyName, ip, device.getIdentity()
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
                new Thread(new UpdateFavoriteNoListRefreshTask(wdDevice)).start();
            }

            int positionInNewList = checkIfOnNewList(wdDevice.getUuid());
            if (positionInNewList == -1) {
                listOfNewDevices.add(wdDevice);
            } else {
                listOfNewDevices.set(positionInNewList, wdDevice);
            }

            runOnUiThread(HomeActivity.this::updateLists);
        }
    }

    private class LoadFavouritesTask implements Runnable {
        @Override
        public void run() {
            runOnUiThread(
                    () -> {
                        ly_emptyfav.setVisibility(View.GONE);
                        ly_loadingfav.setVisibility(View.VISIBLE);
                    }
            );
            loadFavoritesDevicesList();
            runOnUiThread(HomeActivity.this::updateLists);
        }
    }

    private class RunDevicesDiscoveryTask implements Runnable {
        @Override
        public void run() {
            int retries = 0;
            while (upnpService == null && retries++ < 3) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                }
            }
            runDevicesDiscovery(false, false);
        }
    }

    private class AddNewDeviceToFavoritesTask implements Runnable {
        private final WdDevice device;

        private AddNewDeviceToFavoritesTask(WdDevice device) {
            this.device = device;
        }

        @Override
        public void run() {
            int positionInSavedList = checkIfOnSavedList(device.getUuid());
            if (positionInSavedList != -1) {
                // Favorited, keep friendlyName given by user and deviceId to avoid ListView change of position
                WdDevice savedDevice = listOfSavedDevices.get(positionInSavedList);
                device.setDeviceId(savedDevice.getDeviceId());
                device.setFriendlyName(savedDevice.getFriendlyName());
            }

            wdDeviceRepository.add(device);
            loadFavoritesDevicesList();

            //TODO SCF esta movida se puede quitar si al cargar favoritos compruebo si está online o no
            int pos = listOfSavedDevices.indexOf(device);
            if (pos != -1) {
                WdDevice retrieved = listOfSavedDevices.get(pos);
                retrieved.setConnected(true);
                listOfSavedDevices.set(pos, retrieved);
            }
            runOnUiThread(HomeActivity.this::updateLists);
        }
    }

    private class UpdateFavoriteNoListRefreshTask implements Runnable {
        private final WdDevice device;

        private UpdateFavoriteNoListRefreshTask(WdDevice device) {
            this.device = device;
        }

        @Override
        public void run() {
            wdDeviceRepository.update(device);
        }
    }

    private class RemoveDeviceFromFavoritesTask implements Runnable {
        private final WdDevice device;

        private RemoveDeviceFromFavoritesTask(WdDevice device) {
            this.device = device;
        }

        @Override
        public void run() {
            wdDeviceRepository.delete(device.getDeviceId());
            loadFavoritesDevicesList();
            runOnUiThread(HomeActivity.this::updateLists);
        }
    }
}
