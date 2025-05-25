package com.example.backpackapplication.adapter;


import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.backpackapplication.BLEDevice;
import com.example.backpackapplication.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索到的设备列表适配器
 */
public class LVDevicesAdapter extends BaseAdapter {

    private Context context;
    private List<BLEDevice> list;

    public LVDevicesAdapter(Context context) {
        this.context = context;
        list = new ArrayList<>();
    }

    @Override
    public int getCount() {
        return list == null ?  0 : list.size();
    }

    @Override
    public Object getItem(int i) {
        if(list == null){
            return null;
        }
        return list.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        DeviceViewHolder viewHolder;
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.layout_lv_devices_item, null);
            viewHolder = new DeviceViewHolder();
            viewHolder.tvDeviceName = view.findViewById(R.id.tv_device_name);
            viewHolder.tvDeviceAddress = view.findViewById(R.id.tv_device_address);
            viewHolder.tvDeviceRSSI = view.findViewById(R.id.tv_device_rssi);
            viewHolder.tvDeviceUuid = view.findViewById(R.id.tv_device_uuid); // 绑定UUID视图
            view.setTag(viewHolder);
        } else {
            viewHolder = (DeviceViewHolder) view.getTag();
        }
        try {
                BLEDevice device = list.get(i);
            BluetoothDevice btDevice = device.getBluetoothDevice();
                // 设置UUID信息
                /*String uuidInfo = (device.getServiceUuids() != null && !device.getServiceUuids().isEmpty())
                        ? device.getServiceUuids().toString()
                        : "No UUID Found";
                viewHolder.tvDeviceUuid.setText("UUIDs: " + uuidInfo);

            if (list.get(i).getBluetoothDevice().getName() == null) {
                viewHolder.tvDeviceName.setText("NULL");
            } else {
                viewHolder.tvDeviceName.setText(list.get(i).getBluetoothDevice().getName());
            }

            viewHolder.tvDeviceAddress.setText(list.get(i).getBluetoothDevice().getAddress());
            viewHolder.tvDeviceRSSI.setText("RSSI：" + list.get(i).getRSSI());*/
            // 只显示名称和RSSI
            viewHolder.tvDeviceName.setText(btDevice.getName() != null ?
                    btDevice.getName() : "未知设备");
            viewHolder.tvDeviceRSSI.setText("信号强度: " + device.getRSSI() + " dBm");

        }catch(SecurityException e)
        {

        }
        return view;
    }
    /**
     * 初始化所有设备列表
     * @param bluetoothDevices
     */
    public void addAllDevice(List<BLEDevice> bluetoothDevices){
        if(list != null){
            list.clear();
            list.addAll(bluetoothDevices);
            notifyDataSetChanged();
        }

    }

    /**
     * 添加列表子项
     * @param bleDevice
     */
    public void addDevice(BLEDevice bleDevice){
        if(list == null){
            return;
        }
        if(!list.contains(bleDevice)){
            list.add(bleDevice);
        }
        notifyDataSetChanged();   //刷新
    }

    /**
     * 智能添加/更新设备
     */
    public void addOrUpdateDevice(BLEDevice newDevice) {
        if (list == null) return;

        // 查找现有设备
        int existingIndex = -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equals(newDevice)) {
                existingIndex = i;
                break;
            }
        }
try {
    if (existingIndex != -1) {
        // 更新已有设备信息
        BLEDevice existing = list.get(existingIndex);
        existing.setRSSI(newDevice.getRSSI());

        // 更新缓存名称（关键修改）
        String newName = newDevice.getBluetoothDevice().getName();
        if (newName != null) {
            existing.updateName(newName);
        }
    } else {
        list.add(newDevice);
    }
}catch (SecurityException e){}
        notifyDataSetChanged();
    }

    /**
     * 清空列表
     */
    public void clear(){
        if(list != null){
            list.clear();
        }
        notifyDataSetChanged(); //刷新
    }

    class DeviceViewHolder {

        TextView tvDeviceUuid;
        TextView tvDeviceName;
        TextView tvDeviceAddress;
        TextView tvDeviceRSSI;
    }

}
