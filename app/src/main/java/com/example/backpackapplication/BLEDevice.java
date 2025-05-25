package com.example.backpackapplication;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.ArrayList;

public class BLEDevice implements Parcelable {
    private BluetoothDevice bluetoothDevice;  //蓝牙设备
    private int RSSI;  //蓝牙信号
    private String cachedName; // 新增缓存名称字段
    private List<ParcelUuid> serviceUuids; //新增：存储服务UUID列表

    // 实现 writeToParcel、describeContents 等方法
    public static final Creator<BLEDevice> CREATOR = new Creator<BLEDevice>() {
        @Override
        public BLEDevice createFromParcel(Parcel in) {
            return new BLEDevice(in);
        }

        @Override
        public BLEDevice[] newArray(int size) {
            return new BLEDevice[size];
        }
    };
    // 新增 Parcel 构造函数
    protected BLEDevice(Parcel in) {
        // 读取 BluetoothDevice (需要特殊处理)
        bluetoothDevice = in.readParcelable(BluetoothDevice.class.getClassLoader());
        RSSI = in.readInt();
        cachedName = in.readString();
        // 读取 ParcelUuid 列表
        serviceUuids = new ArrayList<>();
        in.readList(serviceUuids, ParcelUuid.class.getClassLoader());
    }

    public BLEDevice(BluetoothDevice bluetoothDevice, int RSSI) {
        this.bluetoothDevice = bluetoothDevice;
        this.RSSI = RSSI;
        try {
            this.cachedName = bluetoothDevice.getName(); // 初始化时存储名称
        }catch (SecurityException e){}
    }

    // 新增名称更新方法
    public void updateName(String newName) {
        if (newName != null && !newName.isEmpty()) {
            this.cachedName = newName;
        }
    }

    public String getName() {
        return cachedName;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    public int getRSSI() {
        return RSSI;
    }

    public void setRSSI(int RSSI) {
        this.RSSI = RSSI;
    }

    public List<ParcelUuid> getServiceUuids() {
        return serviceUuids;
    }

    public void setServiceUuids(List<ParcelUuid> serviceUuids) {
        this.serviceUuids = serviceUuids;
    }
    public String getUuidsString() {
        if (serviceUuids == null || serviceUuids.isEmpty()) {
            return "No Service UUID";
        }
        StringBuilder sb = new StringBuilder();
        for (ParcelUuid uuid : serviceUuids) {
            sb.append(uuid.toString()).append("\n");
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BLEDevice that = (BLEDevice) o;
        return bluetoothDevice.getAddress().equals(that.bluetoothDevice.getAddress());
    }

    @Override
    public int hashCode() {
        return bluetoothDevice.getAddress().hashCode();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        // 写入 BluetoothDevice
        dest.writeParcelable(bluetoothDevice, flags);
        dest.writeInt(RSSI);
        dest.writeString(cachedName);
        // 写入 ParcelUuid 列表
        dest.writeList(serviceUuids);
    }
}

