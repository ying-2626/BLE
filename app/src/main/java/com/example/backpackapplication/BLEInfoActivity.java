package com.example.backpackapplication;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.backpackapplication.ble.BLEManager;
import com.example.backpackapplication.ble.OnBleConnectListener;

import java.util.List;
import java.util.UUID;

public class BLEInfoActivity extends AppCompatActivity {

    private TextView tvDeviceName, tvMacAddress, tvRssi, tvUuid, tvDeviceType;
    private Button btnConnect;
    private BLEManager bleManager;
    private BLEDevice bleDevice;
    private BluetoothGatt currentGatt;

    // 定义全局监听器实例
    private final OnBleConnectListener globalConnectListener = new OnBleConnectListener() {
        @Override
        public void onDisConnectSuccess(BluetoothGatt gatt, BluetoothDevice device, int status) {
            runOnUiThread(() -> {
                Toast.makeText(BLEInfoActivity.this, "断开成功", Toast.LENGTH_SHORT).show();
                updateConnectionState(false);
            });
        }

        @Override
        public void onConnectSuccess(BluetoothGatt gatt, BluetoothDevice device, int status) {
            currentGatt = gatt;
            runOnUiThread(() -> {
                Toast.makeText(BLEInfoActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                updateConnectionState(true);
            });
        }

        @Override
        public void onConnectFailure(BluetoothGatt gatt, BluetoothDevice device, String exception, int status) {
            runOnUiThread(() -> {
                Toast.makeText(BLEInfoActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                updateConnectionState(false);
            });
        }

        // 其他需要处理的方法保持空实现
        @Override public void onConnecting(BluetoothGatt gatt, BluetoothDevice device) {}
        @Override public void onDisConnecting(BluetoothGatt gatt, BluetoothDevice device) {}
        @Override public void onServiceDiscoverySucceed(BluetoothGatt gatt, BluetoothDevice device, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            for (BluetoothGattService service : services) {
                UUID serviceUuid = service.getUuid(); // 获取服务UUID
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                for (BluetoothGattCharacteristic ch : characteristics) {
                    UUID charUuid = ch.getUuid(); // 获取特征UUID
                    int properties = ch.getProperties(); // 读写权限
                }
            }
        }
        @Override public void onServiceDiscoveryFailed(BluetoothGatt gatt, BluetoothDevice device, String failMsg) {}
        @Override public void onReceiveMessage(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattCharacteristic characteristic, byte[] msg) {}
        @Override public void onReceiveError(String errorMsg) {}
        @Override public void onWriteSuccess(BluetoothGatt gatt, BluetoothDevice device, byte[] msg) {}
        @Override public void onWriteFailure(BluetoothGatt gatt, BluetoothDevice device, byte[] msg, String errorMsg) {}
        @Override public void onReadRssi(BluetoothGatt gatt, int Rssi, int status) {}
        @Override public void onMTUSetSuccess(String successMTU, int newMtu) {}
        @Override public void onMTUSetFailure(String failMTU) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_info);

        initViews();
        setupBLEManager();
        parseIntentData();
        updateUI();
        //初始化时检查连接状态
        refreshButtonState();
        setupConnectButton();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 每次返回页面时检查连接状态
        refreshButtonState();
    }

    private void refreshButtonState() {
        // 获取当前页面对应的设备
        BluetoothDevice currentDevice = bleDevice.getBluetoothDevice();

        // 通过BLEManager现有方法判断
        boolean isConnected = bleManager.isConnected() &&
                currentDevice.equals(bleManager.getCurConnDevice());

        updateConnectionState(isConnected);
    }


    private void initViews() {
        tvDeviceName = findViewById(R.id.tv_device_name);
        tvMacAddress = findViewById(R.id.tv_mac_address);
        tvRssi = findViewById(R.id.tv_rssi);
        tvUuid = findViewById(R.id.tv_uuid);
        tvDeviceType = findViewById(R.id.tv_device_type);
        btnConnect = findViewById(R.id.btn_connect);
    }

    private void setupBLEManager() {
        bleManager = BLEManager.getInstance();
        bleManager.addConnectListener(globalConnectListener); // 设置监听器
        if (!bleManager.initBle(this)) {
            Toast.makeText(this, "BLE不可用", Toast.LENGTH_SHORT).show();
            finish();
        }
    }


    private void parseIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            bleDevice = intent.getParcelableExtra("BLE_DEVICE");
        }
    }

    private void updateUI() {
        if (bleDevice == null) {
            Toast.makeText(this, "设备信息错误", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        BluetoothDevice device = bleDevice.getBluetoothDevice();
        try {
            tvDeviceName.setText(device.getName() != null ? device.getName() : "未知设备");
            tvMacAddress.setText("MAC地址:\n       " + device.getAddress());
            tvRssi.setText("信号强度:\n       " + bleDevice.getRSSI() + " dBm");
            tvUuid.setText("服务UUID:\n       " + bleDevice.getServiceUuids());
            tvDeviceType.setText("设备类型:\n       " + parseDeviceType(device.getType()));
        }catch (SecurityException e)
        {}
    }

    private String getUuidInfo() {
        // 需要根据实际获取UUID的逻辑调整
        return bleDevice.getServiceUuids() != null ?
                bleDevice.getServiceUuids().toString() :
                "无可用UUID信息";
    }

    private String parseDeviceType(int type) {
        switch (type) {
            case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                return "经典蓝牙";
            case BluetoothDevice.DEVICE_TYPE_LE:
                return "低功耗蓝牙";
            case BluetoothDevice.DEVICE_TYPE_DUAL:
                return "双模设备";
            default:
                return "未知类型";
        }
    }

    private void setupConnectButton() {
        btnConnect.setOnClickListener(v -> {
            if (bleManager.isConnected()) {
                showDisconnectDialog();
            } else {
                connectDevice();
            }
        });
    }

    private void connectDevice() {
        new Thread(() -> {
            try {
                // 简化连接调用，不再需要传递监听器（核心改动点3）
                bleManager.connectBleDevice(
                        BLEInfoActivity.this,
                        bleDevice.getBluetoothDevice(),
                        15000
                );
            } catch (SecurityException e) {
                // 异常处理
            }
        }).start();
    }

    private void updateConnectionState(boolean isConnected) {
        runOnUiThread(() -> {
            btnConnect.setEnabled(true);
            btnConnect.setText(isConnected ? "断开连接" : "连接设备");
            // 可选：更新其他UI元素，如状态指示灯
            if (isConnected) {
                btnConnect.setBackgroundColor(Color.RED);
            } else {
                btnConnect.setBackgroundColor(Color.GREEN);
            }
        });
    }

    private void showDisconnectDialog() {
        new AlertDialog.Builder(this)
                .setTitle("断开连接")
                .setMessage("确定要断开当前设备吗？")
                .setPositiveButton("确定", (d, w) -> disconnectDevice())
                .setNegativeButton("取消", null)
                .show();
    }

    private void disconnectDevice() {
        new Thread(() -> {
            try {
                if (currentGatt != null) {
                    bleManager.disconnect();
                     }
            }catch (SecurityException e)
            {}
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
            bleManager.removeConnectListener(globalConnectListener); // 移除监听
    }
}