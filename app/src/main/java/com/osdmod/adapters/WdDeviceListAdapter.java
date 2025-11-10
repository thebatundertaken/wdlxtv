package com.osdmod.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.osdmod.model.WdDevice;
import com.osdmod.remote.R;

import java.util.List;

public class WdDeviceListAdapter extends BaseAdapter implements View.OnClickListener {
    private final Context context;
    private final List<WdDevice> listOfDevices;

    public WdDeviceListAdapter(Context context2, List<WdDevice> devices) {
        this.context = context2;
        this.listOfDevices = devices;
    }

    public int getCount() {
        return listOfDevices.size();
    }

    public Object getItem(int position) {
        return listOfDevices.get(position);
    }

    public long getItemId(int position) {
        return listOfDevices.get(position).getDeviceId();
    }

    public View getView(int position, View convertView, ViewGroup viewGroup) {
        WdDevice wdDevice = listOfDevices.get(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_row, null);
        }

        TextView txt_name = convertView.findViewById(R.id.txt_name);
        TextView txt_model = convertView.findViewById(R.id.txt_model);
        TextView txt_ip = convertView.findViewById(R.id.txt_ip);
        ImageView icon = convertView.findViewById(R.id.icon);
        ImageView img_wifi = convertView.findViewById(R.id.img_wifi);

        txt_name.setText(wdDevice.getFriendlyName());
        txt_model.setText(wdDevice.getModelName());
        txt_ip.setText(wdDevice.getIp());
        icon.setImageResource(wdDevice.getDeviceDrawable(false));

        int colorPrimary = wdDevice.isConnected() ? ContextCompat.getColor(context, R.color.white) : ContextCompat.getColor(context, R.color.white3);
        int colorSecondary = ContextCompat.getColor(context, R.color.white2);
        txt_name.setTextColor(colorPrimary);
        txt_model.setTextColor(colorSecondary);
        txt_ip.setTextColor(colorSecondary);

        /*if (wdDevice.isConnected()) {
            if (wdDevice.isRemote()) {
                id_remote = R.drawable.iconb_remote;
            }
            if (wdDevice.isKeyboardAvailable()) {
                id_keyboard = R.drawable.iconb_keyboard;
            }
            if (wdDevice.isUpnp()) {
                id_upnp = R.drawable.iconb_upnp;
            }
        } else {
            if (wdDevice.isRemote()) {
                id_remote = R.drawable.icon_remote;
            }
            if (wdDevice.isKeyboardAvailable()) {
                id_keyboard = R.drawable.icon_keyboard;
            }
            if (wdDevice.isUpnp()) {
                id_upnp = R.drawable.icon_upnp;
            }
        }*/

        img_wifi.setImageResource(wdDevice.isConnected() ? R.drawable.wifi_24 : R.drawable.wifi_off_24);
        return convertView;
    }

    public void onClick(View view) {
    }

}
