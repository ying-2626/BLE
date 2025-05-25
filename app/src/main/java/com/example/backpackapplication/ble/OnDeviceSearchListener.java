package com.example.backpackapplication.ble;


import com.example.backpackapplication.BLEDevice;

/**
 * 蓝牙设备搜索监听者
 * 1、开启搜索
 * 2、完成搜索
 * 3、搜索到设备
 */
public interface OnDeviceSearchListener {

    void onDeviceFound(BLEDevice bleDevice);  //搜索到设备
    void onDiscoveryOutTime(); //扫描超时


}
