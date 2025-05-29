package com.example.backpackapplication.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Looper;

import com.example.backpackapplication.ui.dashboard.BackpackFragment;


/**
 * BLE消息后台接收与用户确认弹窗工具
 */
public class BLEMessageReceiver {

    /**
     * 处理收到的蓝牙消息，弹窗请求用户确认
     * @param activity 当前Activity（用于弹窗）
     * @param device   蓝牙设备
     * @param backpackId  背包序列号（业务唯一标识）
     * @param message  蓝牙消息内容
     * @param backpackFragment  业务处理Fragment
     */
    public static void handleBleMessage(Activity activity, BluetoothDevice device, String backpackId, String message, BackpackFragment backpackFragment) {
        // 主线程弹窗
        Handler handler = new Handler(Looper.getMainLooper());
        try {
            handler.post(() -> {
                new AlertDialog.Builder(activity)
                        .setTitle("蓝牙操作确认")
                        .setMessage("收到来自背包[" + (device.getName() != null ? device.getName() : device.getAddress())
                                + "]的消息：\n" + message + "\n\n是否确认执行此操作？")
                        .setPositiveButton("确定", (d, w) -> {
                            // 回调Fragment进行业务处理
                            if (backpackFragment != null) {
                                backpackFragment.onUserConfirmedBluetoothOperation(device, backpackId, message);
                            }
                        })
                        .setNegativeButton("取消", null)
                        .setCancelable(false)
                        .show();
            });
        } catch (SecurityException e) {
        }
    }
}
