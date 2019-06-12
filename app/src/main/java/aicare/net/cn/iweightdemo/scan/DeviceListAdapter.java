/*******************************************************************************
 * Copyright (c) 2013 Nordic Semiconductor. All Rights Reserved.
 * <p>
 * The information contained herein is property of Nordic Semiconductor ASA. Terms and conditions of usage are described in detail in NORDIC SEMICONDUCTOR STANDARD SOFTWARE LICENSE AGREEMENT.
 * Licensees are granted free, non-transferable use of the information. NO WARRANTY of ANY KIND is provided. This heading must NOT be removed from the file.
 ******************************************************************************/
package aicare.net.cn.iweightdemo.scan;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import aicare.net.cn.iweightdemo.R;
import aicare.net.cn.iweightlibrary.entity.BroadData;

/**
 * DeviceListAdapter class is list adapter for showing scanned Devices name, address and RSSI image based on RSSI values.
 */
public class DeviceListAdapter extends BaseAdapter {
    private static final int NO_RSSI = -1000;

    private final List<BroadData> listValues;
    private final Context context;

    public DeviceListAdapter(Context context, List<BroadData> listValues) {
        this.context = context;
        this.listValues = listValues;
    }

    @Override
    public int getCount() {
        return listValues.size();
    }

    @Override
    public Object getItem(int position) {
        return listValues.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        ViewHolder holder;
        if (view == null) {
            holder = new ViewHolder();
            view = LayoutInflater.from(context).inflate(R.layout.device_list_row, null);
            holder.name = (TextView) view.findViewById(R.id.name);
            holder.address = (TextView) view.findViewById(R.id.address);
            holder.rssi = (ImageView) view.findViewById(R.id.rssi);
            holder.flag = (TextView) view.findViewById(R.id.flag);
            view.setTag(holder);
        }
        holder = (ViewHolder) view.getTag();
        BroadData device = (BroadData) getItem(position);
        String name = device.getName();
        holder.name.setText(name != null ? name : context.getString(R.string.not_available));
        holder.address.setText(device.getAddress());
        if (device.getRssi() != NO_RSSI) {
            int rssiPercent = (int) (100.0f * (127.0f + device.getRssi()) / (127.0f + 20.0f));
            holder.rssi.setImageLevel(rssiPercent);
            holder.rssi.setVisibility(View.VISIBLE);
        }
        holder.flag.setVisibility(device.isBright() ? View.VISIBLE : View.GONE);
        return view;
    }

    private class ViewHolder {
        private TextView name;
        private TextView address;
        private ImageView rssi;
        private TextView flag;
    }
}
