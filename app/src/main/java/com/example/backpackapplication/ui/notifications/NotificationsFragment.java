package com.example.backpackapplication.ui.notifications;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.backpackapplication.BLEDevice;
import com.example.backpackapplication.BLEInfoActivity;
import com.example.backpackapplication.R;
import com.example.backpackapplication.adapter.LVDevicesAdapter;
import com.example.backpackapplication.ble.BLEManager;
import com.example.backpackapplication.ble.OnBleConnectListener;
import com.example.backpackapplication.ble.OnDeviceSearchListener;
import com.example.backpackapplication.databinding.FragmentNotificationsBinding;

import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private ListView lvDevices;
    private LVDevicesAdapter adapter;
    private BLEManager bleManager;
    private TextView tvScanStatus;
    // 新增成员变量
    private EditText etFilter;
    private Button btnFilter;
    private TextView tvEmpty;
    private static final int REQUEST_ENABLE_BT = 1001;
    private volatile boolean isFragmentActive = false;
    private static final int PERMISSIONS_REQUEST_CODE_ACCESS = 101;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 初始化视图
        lvDevices = root.findViewById(R.id.lv_devices);
        tvScanStatus = root.findViewById(R.id.tv_scan_status);
        adapter = new LVDevicesAdapter(requireContext());
        lvDevices.setAdapter(adapter);
// 在initView中添加
        tvEmpty = root.findViewById(R.id.tv_empty);
// 初始化视图后添加
        etFilter = root.findViewById(R.id.et_filter);
        btnFilter = root.findViewById(R.id.btn_filter);

        // 筛选按钮点击事件
        btnFilter.setOnClickListener(v -> performFilter());

        // 键盘搜索按钮监听
        etFilter.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performFilter();
                return true;
            }
            return false;
        });

        etFilter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 2) { // 输入3个字符后自动触发
                    performFilter();
                }
            }
        });

        // 替换原来的connectDevice方法
        lvDevices.setOnItemClickListener((parent, view, position, id) -> {
            BLEDevice device = (BLEDevice) adapter.getItem(position);
            Intent intent = new Intent(getActivity(), BLEInfoActivity.class);
            intent.putExtra("BLE_DEVICE", device);
            startActivity(intent);
        });

        // 初始化BLE管理器
        bleManager = new BLEManager();
        if (!bleManager.initBle(requireContext())) {
            Toast.makeText(requireContext(), "BLE不可用", Toast.LENGTH_SHORT).show();
        }

        // 请求权限
        requestPermissions();

        // 列表点击连接
      /*  lvDevices.setOnItemClickListener((parent, view, position, id) -> {
            BLEDevice device = (BLEDevice) adapter.getItem(position);
            connectDevice(device.getBluetoothDevice());
        });*/

        return root;
    }

    // 新增筛选方法
    private void performFilter() {
        String filterText = etFilter.getText().toString().trim().toLowerCase();

        List<BLEDevice> filtered = new ArrayList<>();
        try{
        for (int i = 0; i < adapter.getCount(); i++) {
            BLEDevice device = (BLEDevice) adapter.getItem(i);
            String name = device.getBluetoothDevice().getName();
            if (name != null && name.toLowerCase().contains(filterText)) {
                filtered.add(device);
            }
        }}catch (SecurityException e){}

        adapter.clear();
        adapter.addAllDevice(filtered);
        adapter.notifyDataSetChanged();
        updateDeviceVisibility();

        // 隐藏键盘
        InputMethodManager imm = (InputMethodManager) requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(etFilter.getWindowToken(), 0);
    }

    private void updateDeviceVisibility() {
        boolean isEmpty = adapter.getCount() == 0;
        lvDevices.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

        if (isEmpty) {
            String currentFilter = etFilter.getText().toString().trim();
            String message = TextUtils.isEmpty(currentFilter)
                    ? "未发现蓝牙设备"
                    : "未找到包含 '" + currentFilter + "' 的设备";
            tvEmpty.setText(message);
        }

        // 调试日志
        Log.d("UI_UPDATE", "当前设备数: " + adapter.getCount());
    }



    @Override
    public void onResume() {
        super.onResume();
        isFragmentActive = true;
        checkSystemRequirements(); // 每次进入页面都检查
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    @Override
    public void onPause() {
        super.onPause();
        isFragmentActive = false;
        if (bleManager != null) {
            bleManager.stopDiscoveryDevice();
        }
    }

    private final BroadcastReceiver classicBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // 经典蓝牙设备可能没有RSSI值，使用默认值-1
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short)-1);
                try {
                    adapter.addDevice(new BLEDevice(device, rssi)); // 正确使用int类型参数
                    updateDeviceList();
                } catch(SecurityException e) {
                    e.printStackTrace();
                }
            }
        }
    };


    // 修改后的权限请求方法
    private void requestPermissions() {
        List<String> permissions = new ArrayList<>();
        List<String> alreadyGranted = new ArrayList<>();

        // 检查每个权限状态
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkAndAddPermission(Manifest.permission.BLUETOOTH_SCAN, permissions, alreadyGranted);
            checkAndAddPermission(Manifest.permission.BLUETOOTH_CONNECT, permissions, alreadyGranted);
        } else {
            checkAndAddPermission(Manifest.permission.ACCESS_FINE_LOCATION, permissions, alreadyGranted);
            checkAndAddPermission(Manifest.permission.ACCESS_COARSE_LOCATION, permissions, alreadyGranted);
        }
        checkAndAddPermission(Manifest.permission.BLUETOOTH, permissions, alreadyGranted);
        checkAndAddPermission(Manifest.permission.BLUETOOTH_ADMIN, permissions, alreadyGranted);

        // 记录权限状态
        Log.d("BLE-Permission", "已授予权限: " + alreadyGranted);
        Log.d("BLE-Permission", "需要请求权限: " + permissions);

        if (!permissions.isEmpty()) {
            Log.i("BLE-Permission", "开始请求权限...");
            requestPermissions(permissions.toArray(new String[0]), 100);
        } else {
            Log.i("BLE-Permission", "所有权限已具备，直接开始扫描");
            startBleScan();
        }
    }

    // 新增系统服务检查
    private void checkSystemRequirements() {
        // 1. 检查蓝牙开关
        if (!bleManager.isEnable()) {
            showBluetoothEnableDialog();
            return;
        }

        // 2. 检查定位服务（Android 6.0+需要）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isLocationEnabled()) {
                showLocationEnableDialog();
                return;
            }
        }
    }


    private void showBluetoothEnableDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("蓝牙未开启")
                .setMessage("需要开启蓝牙以扫描设备")
                .setPositiveButton("去开启", (d, w) -> {
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent, REQUEST_ENABLE_BT);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showLocationEnableDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("需要位置权限")
                .setMessage("需要位置权限扫描蓝牙设备")
                .setPositiveButton("去设置", (d, w) -> {
                    // 跳转位置设置
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private boolean isLocationEnabled() {
        LocationManager lm = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }



    // 辅助方法：检查并添加需要请求的权限
    private void checkAndAddPermission(String perm, List<String> requestList, List<String> grantedList) {
        if (requireActivity().checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED) {
            grantedList.add(perm);
        } else {
            requestList.add(perm);
        }
    }



    // 修改startBleScan方法，确保设备发现回调在后台线程处理
    private void startBleScan() {
        Log.d("BLE-Scan", "启动蓝牙扫描...");

        // 立即清空设备列表
        requireActivity().runOnUiThread(() -> {
            adapter.clear();
            tvScanStatus.setText("正在扫描蓝牙设备...");
            lvDevices.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
        });

        if (bleManager != null) {
            new Thread(() -> { // 在后台线程执行扫描
                try {
                    Log.d("BLE-Scan", "开始后台扫描线程");
                    bleManager.startDiscoveryDevice(new OnDeviceSearchListener() {
                        @Override
                        public void onDeviceFound(BLEDevice bleDevice) {
                            // 添加设备类型过滤（仅BLE）
                            if (bleDevice.getBluetoothDevice().getType() == BluetoothDevice.DEVICE_TYPE_LE
                                    ||bleDevice.getBluetoothDevice().getType() == BluetoothDevice.DEVICE_TYPE_DUAL) {
                                requireActivity().runOnUiThread(() -> {
                                    adapter.addOrUpdateDevice(bleDevice);
                                    adapter.notifyDataSetChanged();
                                });
                            }
                           // Log.d("BLE-Scan", "发现设备: " + bleDevice.getBluetoothDevice().getAddress());
                        }

                        @Override
                        public void onDiscoveryOutTime() {
                            Log.d("BLE-Scan", "扫描超时");
                           updateDeviceList();
                        }
                    }, 15000);
                } catch (SecurityException e) {
                    Log.e("BLE", "扫描权限异常", e);
                }finally {
                    requireActivity().runOnUiThread(() ->
                            tvScanStatus.setText("扫描完成")); // 正常结束
                }
            }).start();
        }
    }



    // 使用延迟批量更新（500ms内多次更新合并为一次）
    private final Handler updateHandler = new Handler(Looper.getMainLooper());
    private final Runnable updateRunnable = () -> {
        adapter.notifyDataSetChanged();
        lvDevices.invalidateViews();
    };

    private void updateDeviceListDelayed() {
        updateHandler.removeCallbacks(updateRunnable);
        updateHandler.postDelayed(updateRunnable, 500);
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == 100) {
            List<String> deniedPermissions = new ArrayList<>();
            boolean allGranted = true;

            // 详细检查每个权限结果
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    deniedPermissions.add(permissions[i]);
                    allGranted = false;
                }
            }

            Log.d("BLE-Permission", "权限请求结果 - 全部授予: " + allGranted);
            if (!deniedPermissions.isEmpty()) {
                Log.w("BLE-Permission", "被拒绝的权限: " + deniedPermissions);
            }

            if (allGranted) {
                Log.i("BLE-Permission", "用户授予全部权限，开始扫描");
                startBleScan();
            } else {
                // 显示更明确的提示
                showPermissionDeniedWarning(deniedPermissions);
            }
        }
    }

    // 新增方法：显示更详细的权限拒绝提示
    private void showPermissionDeniedWarning(List<String> deniedPermissions) {
        String message;
        if (deniedPermissions.contains(Manifest.permission.BLUETOOTH_SCAN) ||
                deniedPermissions.contains(Manifest.permission.ACCESS_FINE_LOCATION)) {
            message = "蓝牙扫描需要位置权限才能发现设备";
        } else {
            message = "部分必要权限未授予，功能受限";
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("权限不足")
                .setMessage(message + "\n被拒绝的权限: " + deniedPermissions)
                .setPositiveButton("去设置", (d, w) -> openAppSettings())
                .setNegativeButton("取消", null)
                .show();
    }


    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
        startActivity(intent);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // 释放所有BLE资源
        if (bleManager != null) {
            new Thread(() -> { // 在后台线程执行释放
                try {
                    bleManager.stopDiscoveryDevice();
                } catch(SecurityException e) {
                    Log.e("BLE", "资源释放异常", e);
                }
            }).start();
        }

        // 清理Handler
        updateHandler.removeCallbacksAndMessages(null);

        // 解注册广播接收器
        try {
            requireActivity().unregisterReceiver(classicBluetoothReceiver);
        } catch (IllegalArgumentException e) {
            Log.e("BLE", "广播接收器未注册", e);
        }

        binding = null;
    }


    // 在设备列表更新时
    // 更新设备列表可见性
    // 修改扫描完成后的UI更新
    private void updateDeviceList() {
        requireActivity().runOnUiThread(() -> {
            boolean isEmpty = adapter.getCount() == 0;
            lvDevices.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

            if (isEmpty) {
                String currentFilter = etFilter.getText().toString().trim();
                String message = TextUtils.isEmpty(currentFilter) ?
                        "未发现蓝牙设备" :
                        "未找到包含\"" + currentFilter + "\"的设备";
                tvEmpty.setText(message);
            }
        });
    }

}