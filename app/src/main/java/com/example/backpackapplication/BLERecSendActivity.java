package com.example.backpackapplication;
//BLERecSendActivity

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import com.example.backpackapplication.adapter.LVDevicesAdapter;
import com.example.backpackapplication.ble.BLEManager;
import com.example.backpackapplication.ble.OnBleConnectListener;
import com.example.backpackapplication.ble.OnDeviceSearchListener;
import com.example.backpackapplication.permisson.PermissionListener;
import com.example.backpackapplication.permisson.PermissionRequest;
import com.example.backpackapplication.util.TypeConversion;

/**
 * BLE开发
 */
public class BLERecSendActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "BLEMain";

    //bt_patch(mtu).bin
    public static String SERVICE_UUID;  //蓝牙通讯服务
    public static BluetoothGattCharacteristic readCharacteristic;  //读特征
    public static BluetoothGattCharacteristic writeCharacteristic;  //写特征

    //动态申请权限
    private String[] requestPermissionArray = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    // 声明一个集合，在后面的代码中用来存储用户拒绝授权的权限
    private List<String> deniedPermissionList = new ArrayList<>();

    private static final int CONNECT_SUCCESS = 0x01;
    private static final int CONNECT_FAILURE = 0x02;
    private static final int DISCONNECT_SUCCESS = 0x03;
    private static final int SEND_SUCCESS = 0x04;
    private static final int SEND_FAILURE= 0x05;
    private static final int RECEIVE_SUCCESS= 0x06;
    private static final int RECEIVE_FAILURE =0x07;
    private static final int START_DISCOVERY = 0x08;
    private static final int STOP_DISCOVERY = 0x09;
    private static final int DISCOVERY_DEVICE = 0x0A;
    private static final int DISCOVERY_OUT_TIME = 0x0B;
    private static final int SELECT_DEVICE = 0x0C;
    private static final int BT_OPENED = 0x0D;
    private static final int BT_CLOSED = 0x0E;

    private Button btSearch;
    private TextView tvCurConState;
    private TextView tvName;
    private TextView tvAddress;
    private Button btConnect;
    private Button btDisconnect;
    private EditText etSendMsg;
    private Button btSend;
    private TextView tvSendResult;
    private TextView tvReceive;
    private LinearLayout llDeviceList;
    private LinearLayout llDataSendReceive;
    private ListView lvDevices;
    private LVDevicesAdapter lvDevicesAdapter;

    private Context mContext;
    private BLEManager bleManager;
    private BLEBroadcastReceiver bleBroadcastReceiver;
    private BluetoothDevice curBluetoothDevice;  //当前连接的设备
    //当前设备连接状态
    private boolean curConnState = false;


    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            switch(msg.what){
                case START_DISCOVERY:
                    Log.d(TAG, "开始搜索设备...");
                    break;

                case STOP_DISCOVERY:
                    Log.d(TAG, "停止搜索设备...");
                    break;

                case DISCOVERY_DEVICE:  //扫描到设备
                    BLEDevice bleDevice = (BLEDevice) msg.obj;
                    lvDevicesAdapter.addDevice(bleDevice);

                    break;

                case SELECT_DEVICE:
                    BluetoothDevice bluetoothDevice = (BluetoothDevice) msg.obj;
                    try {
                        tvName.setText(bluetoothDevice.getName());
                        tvAddress.setText(bluetoothDevice.getAddress());
                        curBluetoothDevice = bluetoothDevice;
                    }catch(SecurityException e)
                    {
                    }
                    break;

                case CONNECT_FAILURE: //连接失败
                    Log.d(TAG, "连接失败");
                    tvCurConState.setText("连接失败");
                    curConnState = false;
                    break;

                case CONNECT_SUCCESS:  //连接成功
                    Log.d(TAG, "连接成功！");
                    tvCurConState.setText("连接成功！");
                    curConnState = true;
                    llDataSendReceive.setVisibility(View.VISIBLE);
                    llDeviceList.setVisibility(View.GONE);
                    break;

                case DISCONNECT_SUCCESS:
                    Log.d(TAG, "断开成功");
                    tvCurConState.setText("断开成功");
                    curConnState = false;

                    break;

                case SEND_FAILURE: //发送失败
                    byte[] sendBufFail = (byte[]) msg.obj;
                    String sendFail = TypeConversion.bytes2HexString(sendBufFail,sendBufFail.length);
                    tvSendResult.setText("发送数据失败，长度" + sendBufFail.length + "--> " + sendFail);
                    break;

                case SEND_SUCCESS:  //发送成功
                    byte[] sendBufSuc = (byte[]) msg.obj;
                    String sendResult = TypeConversion.bytes2HexString(sendBufSuc,sendBufSuc.length);
                    tvSendResult.setText("发送数据成功，长度" + sendBufSuc.length + "--> " + sendResult);
                    break;

                case RECEIVE_FAILURE: //接收失败
                    String receiveError = (String) msg.obj;
                    tvReceive.setText(receiveError);
                    break;

                case RECEIVE_SUCCESS:  //接收成功
                    byte[] recBufSuc = (byte[]) msg.obj;
                    String receiveResult = TypeConversion.bytes2HexString(recBufSuc,recBufSuc.length);
                    tvReceive.setText("接收数据成功，长度" + recBufSuc.length + "--> " + receiveResult);
                    break;

                case BT_CLOSED:
                    Log.d(TAG, "系统蓝牙已关闭");
                    break;

                case BT_OPENED:
                    Log.d(TAG, "系统蓝牙已打开");
                    break;
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_rec_send);

        mContext = BLERecSendActivity.this;

        //动态申请权限（Android 6.0）

        //初始化视图
        initView();
        //初始化监听
        iniListener();
        //初始化数据
        initData();
        //注册广播
        initBLEBroadcastReceiver();
        //初始化权限
        initPermissions();

    }

    /**
     * 初始化视图
     */
    private void initView() {
        btSearch = findViewById(R.id.bt_search);
        tvCurConState = findViewById(R.id.tv_cur_con_state);
        btConnect = findViewById(R.id.bt_connect);
        btDisconnect = findViewById(R.id.bt_disconnect);
        tvName = findViewById(R.id.tv_name);
        tvAddress = findViewById(R.id.tv_address);
        etSendMsg = findViewById(R.id.et_send_msg);
        btSend = findViewById(R.id.bt_to_send);
        tvSendResult = findViewById(R.id.tv_send_result);
        tvReceive = findViewById(R.id.tv_receive_result);
        llDeviceList = findViewById(R.id.ll_device_list);
        llDataSendReceive  = findViewById(R.id.ll_data_send_receive);
        lvDevices = findViewById(R.id.lv_devices);
    }


    /**
     * 初始化监听
     */
    private void iniListener() {
        btSearch.setOnClickListener(this);
        btConnect.setOnClickListener(this);
        btDisconnect.setOnClickListener(this);
        btSend.setOnClickListener(this);

        lvDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                BLEDevice bleDevice = (BLEDevice) lvDevicesAdapter.getItem(i);
                BluetoothDevice bluetoothDevice = bleDevice.getBluetoothDevice();
                if(bleManager != null){
                    try{
                    bleManager.stopDiscoveryDevice();
                    }catch(SecurityException e)
                    {}
                }
                Message message = new Message();
                message.what = SELECT_DEVICE;
                message.obj = bluetoothDevice;
                mHandler.sendMessage(message);
            }
        });
    }

    /**
     * 初始化数据
     */
    private void initData() {
        //列表适配器
        lvDevicesAdapter = new LVDevicesAdapter(BLERecSendActivity.this);
        lvDevices.setAdapter(lvDevicesAdapter);

        //初始化ble管理器
        bleManager = new BLEManager();
        if(!bleManager.initBle(mContext)) {
            Log.d(TAG, "该设备不支持低功耗蓝牙");
            Toast.makeText(mContext, "该设备不支持低功耗蓝牙", Toast.LENGTH_SHORT).show();
        }else{
            if(!bleManager.isEnable()){
                //去打开蓝牙
                try {
                    bleManager.openBluetooth(mContext, false);
                }catch(SecurityException e)
                {}
            }
        }
    }


    /**
     * 注册广播
     */
    private void initBLEBroadcastReceiver() {
        //注册广播接收
        bleBroadcastReceiver = new BLEBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED); //开始扫描
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);//扫描结束
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);//手机蓝牙状态监听
        registerReceiver(bleBroadcastReceiver,intentFilter);
    }

    /**
     * 初始化权限
     */
    private void initPermissions() {
        //Android 6.0以上动态申请权限
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            final PermissionRequest permissionRequest = new PermissionRequest();
            permissionRequest.requestRuntimePermission(BLERecSendActivity.this, requestPermissionArray, new PermissionListener() {
                @Override
                public void onGranted() {
                    Log.d(TAG,"所有权限已被授予");
                }

                //用户勾选“不再提醒”拒绝权限后，关闭程序再打开程序只进入该方法！
                @Override
                public void onDenied(List<String> deniedPermissions) {
                    deniedPermissionList = deniedPermissions;
                    for (String deniedPermission : deniedPermissionList) {
                        Log.e(TAG,"被拒绝权限：" + deniedPermission);
                    }
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //注销广播接收
        unregisterReceiver(bleBroadcastReceiver);
    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == R.id.bt_search) {
            // 处理搜索蓝牙逻辑
            llDataSendReceive.setVisibility(View.GONE);
            llDeviceList.setVisibility(View.VISIBLE);
            searchBtDevice();
        } else if (viewId == R.id.bt_connect) {
            // 处理连接蓝牙逻辑
            if (!curConnState) {
                if (bleManager != null) {
                    try {
                        bleManager.connectBleDevice(mContext, curBluetoothDevice, 500);
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                Toast.makeText(this, "当前设备已连接", Toast.LENGTH_SHORT).show();
            }
        } else if (viewId == R.id.bt_disconnect) {
            // 处理断开连接逻辑
            if (curConnState) {
                if (bleManager != null) {
                    try {
                        bleManager.disConnectDevice();
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                Toast.makeText(this, "当前设备未连接", Toast.LENGTH_SHORT).show();
            }
        } else if (viewId == R.id.bt_to_send) {
            // 处理发送数据逻辑
            if (curConnState) {
                String sendMsg = etSendMsg.getText().toString();
                if (sendMsg.isEmpty()) {
                    Toast.makeText(this, "发送数据为空！", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (bleManager != null) {
                    try {
                        bleManager.sendMessage(sendMsg);
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                Toast.makeText(this, "请先连接当前设备", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //////////////////////////////////  搜索设备  /////////////////////////////////////////////////
    private void searchBtDevice() {
        if(bleManager == null){
            Log.d(TAG, "searchBtDevice()-->bleManager == null");
            return;
        }
        try{
    if (bleManager.isDiscovery()) { //当前正在搜索设备...
        bleManager.stopDiscoveryDevice();
    }
        }catch(SecurityException e)
            {}

        if(lvDevicesAdapter != null){
            lvDevicesAdapter.clear();  //清空列表
        }
try {
    //开始搜索
    bleManager.startDiscoveryDevice(onDeviceSearchListener, 15000);
}catch(SecurityException e)
{}
    }

    //扫描结果回调
    private OnDeviceSearchListener onDeviceSearchListener = new OnDeviceSearchListener() {

        @Override
        public void onDeviceFound(BLEDevice bleDevice) {
            Message message = new Message();
            message.what = DISCOVERY_DEVICE;
            message.obj = bleDevice;
            mHandler.sendMessage(message);
        }

        @Override
        public void onDiscoveryOutTime() {
            Message message = new Message();
            message.what = DISCOVERY_OUT_TIME;
            mHandler.sendMessage(message);
        }
    };

    //连接回调
    private OnBleConnectListener onBleConnectListener = new OnBleConnectListener() {
        @Override
        public void onConnecting(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice) {

        }

        @Override
        public void onConnectSuccess(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice, int status) {
            //因为服务发现成功之后，才能通讯，所以在成功发现服务的地方表示连接成功
        }

        @Override
        public void onConnectFailure(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice, String exception, int status) {
            Message message = new Message();
            message.what = CONNECT_FAILURE;
            mHandler.sendMessage(message);
        }

        @Override
        public void onDisConnecting(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice) {

        }

        @Override
        public void onDisConnectSuccess(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice, int status) {
            Message message = new Message();
            message.what = DISCONNECT_SUCCESS;
            message.obj = status;
            mHandler.sendMessage(message);
        }


        /*public void onServiceDiscoverySucceed(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice, int status) {
            //因为服务发现成功之后，才能通讯，所以在成功发现服务的地方表示连接成功
            Message message = new Message();
            message.what = CONNECT_SUCCESS;
            mHandler.sendMessage(message);
        }*/
        @Override
        public void onServiceDiscoverySucceed(BluetoothGatt gatt, BluetoothDevice device, int status) {
            Message message = new Message();
            message.what = CONNECT_SUCCESS;
            mHandler.sendMessage(message);

            // 动态获取特征（类似 BLEInfoActivity 的实现）
            List<BluetoothGattService> services = gatt.getServices();
            for (BluetoothGattService service : services) {
                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                    int properties = characteristic.getProperties();
                    if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                        readCharacteristic = characteristic;
                    }
                    if ((properties & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0) {
                        writeCharacteristic = characteristic;
                    }
                }
            }

            // 确认特征有效后启用通知
            if (readCharacteristic != null) {
                try {
                    bleManager.enableNotification(true, gatt, readCharacteristic);
                }catch(SecurityException e)
                {}
            }
            mHandler.sendEmptyMessage(CONNECT_SUCCESS);
        }


        @Override
        public void onServiceDiscoveryFailed(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice, String failMsg) {
            Message message = new Message();
            message.what = CONNECT_FAILURE;
            mHandler.sendMessage(message);
        }

        @Override
        public void onReceiveMessage(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice, BluetoothGattCharacteristic characteristic, byte[] msg) {
            Message message = new Message();
            message.what = RECEIVE_SUCCESS;
            message.obj = msg;
            mHandler.sendMessage(message);
        }

        @Override
        public void onReceiveError(String errorMsg) {
            Message message = new Message();
            message.what = RECEIVE_FAILURE;
            mHandler.sendMessage(message);
        }

        @Override
        public void onWriteSuccess(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice, byte[] msg) {
            Message message = new Message();
            message.what = SEND_SUCCESS;
            message.obj = msg;
            mHandler.sendMessage(message);
        }

        @Override
        public void onWriteFailure(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice, byte[] msg, String errorMsg) {
            Message message = new Message();
            message.what = SEND_FAILURE;
            message.obj = msg;
            mHandler.sendMessage(message);
        }

        @Override
        public void onReadRssi(BluetoothGatt bluetoothGatt, int Rssi, int status) {

        }

        @Override
        public void onMTUSetSuccess(String successMTU, int newMtu) {

        }

        @Override
        public void onMTUSetFailure(String failMTU) {

        }
    };


    /**
     * 蓝牙广播接收器
     */
    private class BLEBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.equals(action, BluetoothAdapter.ACTION_DISCOVERY_STARTED)) { //开启搜索
                Message message = new Message();
                message.what = START_DISCOVERY;
                mHandler.sendMessage(message);

            } else if (TextUtils.equals(action, BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {//完成搜素
                Message message = new Message();
                message.what = STOP_DISCOVERY;
                mHandler.sendMessage(message);

            } else if(TextUtils.equals(action,BluetoothAdapter.ACTION_STATE_CHANGED)){   //系统蓝牙状态监听

                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,0);
                if(state == BluetoothAdapter.STATE_OFF){
                    Message message = new Message();
                    message.what = BT_CLOSED;
                    mHandler.sendMessage(message);

                }else if(state == BluetoothAdapter.STATE_ON){
                    Message message = new Message();
                    message.what = BT_OPENED;
                    mHandler.sendMessage(message);

                }
            }
        }
    }
}

