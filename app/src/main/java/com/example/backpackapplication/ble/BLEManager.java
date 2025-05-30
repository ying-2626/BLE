package com.example.backpackapplication.ble;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;

import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.example.backpackapplication.BLEDevice;
import com.example.backpackapplication.util.ClsUtils;
import com.example.backpackapplication.util.TypeConversion;

/**
 * 1、扫描设备
 * 2、配对设备
 * 3、解除设备配对
 * 4、连接设备
 * 6、发现服务
 * 7、打开读写功能
 * 8、数据通讯（发送数据、接收数据）
 * 9、断开连接
 */
public class BLEManager  {
    private static final String TAG = "BLEManager";
    // 新增Nordic UART服务和特征UUID
    //private static final UUID SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID CHARACTERISTIC_UUID_RX = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e"); // 写
    //private static final UUID CHARACTERISTIC_UUID_TX = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e"); // 读/notify
    // 添加单例模式
    private static BLEManager instance;

    // 添加连接状态跟踪
    private boolean isConnected = false;

    private static final long MAX_CONNECT_TIME = 10000;  //连接超时时间10s

    private Context mContext;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetooth4Adapter;
    private BluetoothGatt mBluetoothGatt;  //当前连接的gatt
    //获取硬件传来的信息
    private BluetoothGattService bluetoothGattService;   //服务
    private BluetoothGattCharacteristic readCharacteristic;  //读特征
    private BluetoothGattCharacteristic writeCharacteristic; //写特征
    private OnDeviceSearchListener onDeviceSearchListener;  //设备扫描结果监听
    private OnBleConnectListener onBleConnectListener;   //连接监听
    private BluetoothDevice curConnDevice;  //当前连接的设备
    private boolean isConnectIng = false;  //是否正在连接中
    private Handler mHandler = new Handler();
    private BluetoothLeScanner bluetoothLeScanner;
    private ScanSettings scanSettings;
    private boolean isScanning = false;

    private List<OnBleConnectListener> listeners = new ArrayList<>();
    public BLEManager() {
    }

    /**
     * 初始化
     * @param context
     */
    public boolean initBle(Context context){
        mContext = context;
        if(!checkBle(context)){
            return false;
        }else {
            // 确保初始化bluetooth4Adapter
            bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager != null) {
                bluetooth4Adapter = bluetoothManager.getAdapter();
            }
            return bluetooth4Adapter != null;
        }
    }

    public BluetoothDevice getCurConnDevice() {
        return curConnDevice; // 使用现有curConnDevice字段
    }

    // 单例获取方法
    public static synchronized BLEManager getInstance() {
        if (instance == null) {
            instance = new BLEManager();
        }
        return instance;
    }

    public void addConnectListener(OnBleConnectListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeConnectListener(OnBleConnectListener listener) {
        listeners.remove(listener);
    }

    private void notifyDisconnectSuccess(BluetoothGatt gatt, BluetoothDevice device, int status) {
        for (OnBleConnectListener listener : listeners) {
            listener.onDisConnectSuccess(gatt, device, status);
        }
    }



    ////////////////////////////////////  扫描设备  ///////////////////////////////////////////////
    //扫描设备回调
    // 新增设备类型校验

    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();

            int rssi = result.getRssi();
            ScanRecord scanRecord = result.getScanRecord();
            List<ParcelUuid> serviceUuids = scanRecord.getServiceUuids();
try {
    // 添加详细日志
    /* Log.d("BLE-Scan", "发现设备: "
            + "\n名称: " + device.getName()
            + "\n地址: " + device.getAddress()
            + "\nUUID: " + (uuids != null ? uuids : "无")
            + "\nRSSI: " + result.getRssi());
*/
    if (device.getType() == BluetoothDevice.DEVICE_TYPE_LE ||
            device.getType() == BluetoothDevice.DEVICE_TYPE_DUAL) {
        BLEDevice bleDevice = new BLEDevice(device, rssi);
        parseServiceUuids(result, bleDevice);

        if (scanRecord != null) {
            bleDevice.setServiceUuids(serviceUuids);// 存储UUID到设备对象
        }
        if (onDeviceSearchListener != null) {
            onDeviceSearchListener.onDeviceFound(bleDevice);
        }
    }
}catch(SecurityException ignored){}
        }

        private String parseScanRecord(ScanResult result) {
            ScanRecord record = result.getScanRecord();
            return record != null ? record.toString() : "无广播数据";
        }

        private void parseServiceUuids(ScanResult result, BLEDevice device) {
            ScanRecord record = result.getScanRecord();
            if (record != null) {
                List<ParcelUuid> uuids = record.getServiceUuids();
                if (uuids != null) {
                    List<ParcelUuid> serviceUuids = new ArrayList<>();
                    device.setServiceUuids(serviceUuids);
                }
            }
        }
        
    
        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "扫描失败，错误码: " + errorCode);
            isScanning = false;
        }
    };

    /**
     * 设置时间段 扫描设备
     * @param onDeviceSearchListener  设备扫描监听
     * @param scanTime  扫描时间
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    public void startDiscoveryDevice(OnDeviceSearchListener onDeviceSearchListener, long scanTime) {
        if (bluetooth4Adapter == null) {
            Log.e(TAG, "startDiscoveryDevice: BluetoothAdapter未初始化");
            return;
        }


        this.onDeviceSearchListener = onDeviceSearchListener;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bluetoothLeScanner = bluetooth4Adapter.getBluetoothLeScanner();
            if (bluetoothLeScanner == null) {
                Log.e(TAG, "无法获取BluetoothLeScanner实例");
                return;
            }

            // 配置扫描参数
            scanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .build();

            Log.d(TAG, "开始BLE扫描，时长: " + scanTime + "ms");
            isScanning = true;
            bluetoothLeScanner.startScan(null, scanSettings, leScanCallback);

            // 设置扫描超时
            mHandler.postDelayed(stopScanRunnable, scanTime);
        } else {
            Log.w(TAG, "当前设备不支持BLE扫描");
        }
    }



    private Runnable stopScanRunnable = new Runnable() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void run() {
            if(onDeviceSearchListener != null){
                onDeviceSearchListener.onDiscoveryOutTime();  //扫描超时回调
            }
            //scanTime之后还没有扫描到设备，就停止扫描。
            stopDiscoveryDevice();
        }
    };

    // 新增简化的连接方法
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void connect(BluetoothDevice device) {
        connectBleDevice(mContext, device, MAX_CONNECT_TIME);
    }

    // 添加连接状态检查
    public boolean isConnected() {
        return isConnected && mBluetoothGatt != null;
    }

    // 修改现有的断开连接方法（原disConnectDevice）
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void disconnect() {
        if (mBluetoothGatt == null) return;
        // 更新连接状态
        isConnected = false;
        // 原有断开逻辑
        mBluetoothGatt.disconnect();
        //mBluetoothGatt.close();
        //mBluetoothGatt = null;
    }



    //////////////////////////////////////  停止扫描  /////////////////////////////////////////////
    /**
     * 停止扫描
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    public void stopDiscoveryDevice() {
        if (bluetoothLeScanner != null && isScanning) {
            Log.d(TAG, "停止BLE扫描");
            bluetoothLeScanner.stopScan(leScanCallback);
            isScanning = false;
        }
        mHandler.removeCallbacks(stopScanRunnable);
    }



    /////////////////////////////////////  执行连接  //////////////////////////////////////////////
    //连接/通讯结果回调
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyUpdate(gatt, txPhy, rxPhy, status);
        }

        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyRead(gatt, txPhy, rxPhy, status);
        }

        //连接状态回调-连接成功/断开连接
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            isConnected = (newState == BluetoothGatt.STATE_CONNECTED);

            Log.d(TAG,"status:" + status);
            Log.d(TAG,"newState:" + newState);

            switch(status){
                case BluetoothGatt.GATT_SUCCESS:
                    Log.w(TAG,"BluetoothGatt.GATT_SUCCESS");
                    break;
                case BluetoothGatt.GATT_FAILURE:
                    Log.w(TAG,"BluetoothGatt.GATT_FAILURE");
                    break;
                case BluetoothGatt.GATT_CONNECTION_CONGESTED:
                    Log.w(TAG,"BluetoothGatt.GATT_CONNECTION_CONGESTED");
                    break;
                case BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION:
                    Log.w(TAG,"BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION");
                    break;
                case BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION:
                    Log.w(TAG,"BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION");
                    break;
                case BluetoothGatt.GATT_INVALID_OFFSET:
                    Log.w(TAG,"BluetoothGatt.GATT_INVALID_OFFSET");
                    break;
                case BluetoothGatt.GATT_READ_NOT_PERMITTED:
                    Log.w(TAG,"BluetoothGatt.GATT_READ_NOT_PERMITTED");
                    break;
                case BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED:
                    Log.w(TAG,"BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED");
                    break;
            }

            BluetoothDevice bluetoothDevice = gatt.getDevice();
            Log.d(TAG,"连接的设备：" + bluetoothDevice.getName() + "  " + bluetoothDevice.getAddress());

            isConnectIng = false;
            //移除连接超时
            mHandler.removeCallbacks(connectOutTimeRunnable);

            if(newState == BluetoothGatt.STATE_CONNECTED){
                Log.w(TAG,"连接成功");
                //连接成功去发现服务
                gatt.discoverServices();
                //设置发现服务超时时间
                mHandler.postDelayed(serviceDiscoverOutTimeRunnable,MAX_CONNECT_TIME);

                    for (OnBleConnectListener listener : listeners) {
                   listener.onConnectSuccess(gatt,bluetoothDevice,status);   //连接成功回调
                 }

            }else if(newState == BluetoothGatt.STATE_DISCONNECTED) {
                isConnected = false; // 更新连接状态
                //清空系统缓存
                ClsUtils.refreshDeviceCache(gatt);
                Log.e(TAG, "断开连接status:" + status);
                gatt.close();  //断开连接释放连接
                mBluetoothGatt = null;
                // 通知所有监听器
                notifyDisconnectSuccess(gatt, bluetoothDevice, status);

                if(status == 133){
                    //无法连接
                    if(onBleConnectListener != null){
                        gatt.close();
                        onBleConnectListener.onConnectFailure(gatt,bluetoothDevice,"连接异常！",status);  //133连接异常 异常断开
                        Log.e(TAG,"连接失败status：" + status + "  " + bluetoothDevice.getAddress());
                    }
                }else if(status == 62){
                    //成功连接没有发现服务断开
                    if(onBleConnectListener != null){
                        gatt.close();
                        onBleConnectListener.onConnectFailure(gatt,bluetoothDevice,"连接成功服务未发现断开！",status); //62没有发现服务 异常断开
                        Log.e(TAG,"连接成功服务未发现断开status:" + status);
                    }

                }else if(status == 0){
                    if(onBleConnectListener != null){

                        onBleConnectListener.onDisConnectSuccess(gatt,bluetoothDevice,status); //0正常断开 回调
                    }
                }else if(status == 8){
                    //因为距离远或者电池无法供电断开连接
                    // 已经成功发现服务
                    if(onBleConnectListener != null){
                        for (OnBleConnectListener listener : listeners) {
                        listener.onDisConnectSuccess(gatt,bluetoothDevice,status); //8断电断开  回调
                      }
                    }
                }else if(status == 34){
                    if(onBleConnectListener != null){
                        onBleConnectListener.onDisConnectSuccess(gatt,bluetoothDevice,status); //34断开
                    }
                }else {
                    //其它断开连接
                    if(onBleConnectListener != null){
                        onBleConnectListener.onDisConnectSuccess(gatt,bluetoothDevice,status); //其它断开
                    }
                }
            }else if(newState == BluetoothGatt.STATE_CONNECTING){
                Log.d(TAG,"正在连接...");
                if(onBleConnectListener != null){
                    onBleConnectListener.onConnecting(gatt,bluetoothDevice);  //正在连接回调
                }
            }else if(newState == BluetoothGatt.STATE_DISCONNECTING){
                Log.d(TAG,"正在断开...");
                if(onBleConnectListener != null){
                    onBleConnectListener.onDisConnecting(gatt,bluetoothDevice); //正在断开回调
                }
                if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    notifyDisconnectSuccess(gatt, bluetoothDevice, status);
                }
            }
        }

        //发现服务
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            //移除发现服务超时
            mHandler.removeCallbacks(serviceDiscoverOutTimeRunnable);
            Log.d(TAG,"移除发现服务超时");

            Log.d(TAG,"发现服务");

            //配置服务信息
            if(setupService(gatt)){
                if(onBleConnectListener != null){
                    onBleConnectListener.onServiceDiscoverySucceed(gatt,gatt.getDevice(),status);  //成功发现服务回调
                }
            }else{
                if(onBleConnectListener != null){
                    onBleConnectListener.onServiceDiscoveryFailed(gatt,gatt.getDevice(),"获取服务特征异常");  //发现服务失败回调
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.d(TAG,"读status: " + status);
        }

        //向蓝牙设备写入数据结果回调
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            // 2. 处理可能的空值
            byte[] value = characteristic.getValue();
            if(value == null){
                Log.e(TAG,"characteristic.getValue() == null，可能原因：setValue未调用或被系统清空");
                // 这里建议直接 return，避免后续空指针
                return;
            }
            // 将收到的字节数组转换成十六进制字符串
            String msg = TypeConversion.bytes2HexString(value, value.length);
            if(status == BluetoothGatt.GATT_SUCCESS){
                //写入成功
                Log.w(TAG,"写入成功：" + msg);
                if(onBleConnectListener != null){
                    onBleConnectListener.onWriteSuccess(gatt,gatt.getDevice(),value);  //写入成功回调
                }
            }else if(status == BluetoothGatt.GATT_FAILURE){
                //写入失败
                Log.e(TAG,"写入失败：" + msg);
                if(onBleConnectListener != null){
                    onBleConnectListener.onWriteFailure(gatt,gatt.getDevice(),value,"写入失败");  //写入失败回调
                }
            }else if(status == BluetoothGatt.GATT_WRITE_NOT_PERMITTED){
                //没有权限
                Log.e(TAG,"没有权限！");
            }
        }

        //读取蓝牙设备发出来的数据回调
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            //接收数据
            byte[] bytes = characteristic.getValue();
            // 直接以字符串显示收到的数据
            String strMsg = new String(bytes);
            Log.w("BLE-Receive", "收到蓝牙消息: " + strMsg);

            if(onBleConnectListener != null){
                onBleConnectListener.onReceiveMessage(gatt,gatt.getDevice(),characteristic,characteristic.getValue());  //接收数据回调
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
            Log.d(TAG,"onReliableWriteCompleted");
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            if(status == BluetoothGatt.GATT_SUCCESS){
                Log.w(TAG,"读取RSSI值成功，RSSI值：" + rssi + ",status" + status);
                if(onBleConnectListener != null){
                    onBleConnectListener.onReadRssi(gatt,rssi,status);  //成功读取连接的信号强度回调
                }
            }else if(status == BluetoothGatt.GATT_FAILURE){
                Log.w(TAG,"读取RSSI值失败，status：" + status);
            }
        }

        //修改MTU值结果回调
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            ///设置mtu值，即bluetoothGatt.requestMtu()时触发，提示该操作是否成功
            if(status == BluetoothGatt.GATT_SUCCESS){  //设置MTU成功
                //MTU默认取的是23，当收到 onMtuChanged 后，会根据传递的值修改MTU，注意由于传输用掉3字节，因此传递的值需要减3。
                //mtu - 3
                Log.w(TAG,"设置MTU成功，新的MTU值：" + (mtu-3) + ",status" + status);
                if(onBleConnectListener != null){
                    onBleConnectListener.onMTUSetSuccess("设置后新的MTU值 = " + (mtu-3) + "   status = " + status,mtu - 3);  //MTU设置成功
                }

            }else if(status == BluetoothGatt.GATT_FAILURE){  //设置MTU失败
                Log.e(TAG,"设置MTU值失败：" + (mtu-3) + ",status" + status);
                if(onBleConnectListener != null){
                    onBleConnectListener.onMTUSetFailure("设置MTU值失败：" + (mtu-3) + "   status：" + status);  //MTU设置失败
                }
            }

        }
    };

    /**
     * 通过蓝牙设备连接
     * @param context  上下文
     * @param bluetoothDevice  蓝牙设备
     * @param outTime          连接超时时间
     * @return
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public BluetoothGatt connectBleDevice(Context context, BluetoothDevice bluetoothDevice, long outTime){
        if(bluetoothDevice == null){
            Log.e(TAG,"connectBleDevice()-->bluetoothDevice == null");
            return null;
        }
        if(isConnectIng){
            Log.e(TAG,"connectBleDevice()-->isConnectIng = true");
            return null;
        }

        this.curConnDevice = bluetoothDevice;
        // 添加连接状态跟踪
        isConnected = false;

        // 移除UUID相关参数
        mBluetoothGatt = bluetoothDevice.connectGatt(context, false, bluetoothGattCallback);
        mBluetoothGatt.connect();

        Log.d(TAG,"开始准备连接：" + bluetoothDevice.getName() + "-->" + bluetoothDevice.getAddress());
        //出现 BluetoothGatt.android.os.DeadObjectException 蓝牙没有打开
        try{
            mBluetoothGatt = bluetoothDevice.connectGatt(context,false,bluetoothGattCallback);
            mBluetoothGatt.connect();
            isConnectIng = true;

        }catch(Exception e){
            Log.e(TAG,"e:" + e.getMessage());
        }

        //设置连接超时时间10s
        mHandler.postDelayed(connectOutTimeRunnable,outTime);

        return mBluetoothGatt;
    }

    //连接超时
    private Runnable connectOutTimeRunnable = new Runnable() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void run() {
            if(mBluetoothGatt == null){
                Log.e(TAG,"connectOuttimeRunnable-->mBluetoothGatt == null");
                return;
            }

            isConnectIng = false;
            mBluetoothGatt.disconnect();

            //连接超时当作连接失败回调
            if(onBleConnectListener != null){
                onBleConnectListener.onConnectFailure(mBluetoothGatt,curConnDevice,"连接超时！",-1);  //连接失败回调
            }
        }
    };

    //发现服务超时
    private Runnable serviceDiscoverOutTimeRunnable = new Runnable() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void run() {
            if(mBluetoothGatt == null){
                Log.e(TAG,"connectOuttimeRunnable-->mBluetoothGatt == null");
                return;
            }

            isConnectIng = false;
            mBluetoothGatt.disconnect();

            //发现服务超时当作连接失败回调
            if(onBleConnectListener != null){
                onBleConnectListener.onConnectFailure(mBluetoothGatt,curConnDevice,"发现服务超时！",-1);  //连接失败回调
            }
        }
    };


    /**
     * 获取特定服务及特征
     * 1个serviceUUID -- 1个readUUID -- 1个writeUUID
     * @param bluetoothGatt
     * @return
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private boolean setupService(BluetoothGatt bluetoothGatt) {
        if (bluetoothGatt == null) {
            Log.e(TAG, "setupService()-->bluetoothGatt == null");
            return false;
        }

        // 打印所有服务UUID
        List<BluetoothGattService> services = bluetoothGatt.getServices();
        Log.d(TAG, "发现服务数量: " + services.size());
        for (BluetoothGattService service : services) {
            Log.d(TAG, "Service UUID: " + service.getUuid().toString());

            // 遍历特征
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                Log.d(TAG, "  Characteristic UUID: " + characteristic.getUuid()
                        + ", 属性: " + characteristic.getProperties());
            }
        }
        //清空特征缓存
        readCharacteristic = null;
        writeCharacteristic = null;

        // 优先根据UUID精确匹配Nordic UART服务和特征
        /*BluetoothGattService uartService = bluetoothGatt.getService(SERVICE_UUID);
        if (uartService != null) {
            BluetoothGattCharacteristic txChar = uartService.getCharacteristic(CHARACTERISTIC_UUID_TX);
            BluetoothGattCharacteristic rxChar = uartService.getCharacteristic(CHARACTERISTIC_UUID_RX);
            if (txChar != null) {
                readCharacteristic = txChar;
                enableNotification(true, bluetoothGatt, readCharacteristic);
            }
            if (rxChar != null) {
                writeCharacteristic = rxChar;
            }
        }*/

        if (readCharacteristic == null || writeCharacteristic == null) {
            for (BluetoothGattService service : bluetoothGatt.getServices()) {
                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                    int properties = characteristic.getProperties();

                    // 识别读特征（支持NOTIFY或READ）
                    if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                            readCharacteristic = characteristic;
                            enableNotification(true, bluetoothGatt, readCharacteristic); // 启用通知
                    }
                    if ((properties & BluetoothGattCharacteristic.PROPERTY_READ) != 0) {
                            readCharacteristic = characteristic;
                    }

                    // 识别写特征（支持WRITE或WRITE_NO_RESPONSE）
                    if ((properties & (BluetoothGattCharacteristic.PROPERTY_WRITE |
                            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0) {
                        if (writeCharacteristic == null) {
                            writeCharacteristic = characteristic;
                        }
                    }
                }
            }
        }


        // 检查特征是否找到
        if (readCharacteristic == null) {
            Log.e(TAG, "读特征未找到");
            return false;
        }
        if (writeCharacteristic == null) {
            Log.e(TAG, "写特征未找到");
            return false;
        }

        Log.d(TAG, "动态发现特征成功: \n读特征=" + readCharacteristic.getUuid() +
                "\n写特征=" + writeCharacteristic.getUuid());

        //重点中重点，需要重新设置
        List<BluetoothGattDescriptor> descriptors = writeCharacteristic.getDescriptors();
        for (BluetoothGattDescriptor descriptor : descriptors) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            bluetoothGatt.writeDescriptor(descriptor);
        }

        //延迟2s，保证所有通知都能及时打开
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
            }
        }, 2000);
        return true;

    }

    /////////////////////////////////////////  打开通知  //////////////////////////////////////////

    /**
     * 设置读特征接收通知
     * @param enable  为true打开通知
     * @param gatt    连接
     * @param characteristic  特征
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void enableNotification(boolean enable, BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){
        if(gatt == null){
            Log.e(TAG,"enableNotification-->gatt == null");
            return;
        }
        if(characteristic == null){
            Log.e(TAG,"enableNotification-->characteristic == null");
            return;
        }
        // 1. 启用特征通知（系统层）
        gatt.setCharacteristicNotification(characteristic,enable);
        // 2. 配置描述符
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb") // 标准CCCD UUID
        );

        if (descriptor != null) {
            // 设置通知类型：NOTIFY 或 INDICATE
            byte[] value = enable ?
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE :
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;

            descriptor.setValue(value);
            gatt.writeDescriptor(descriptor); // 异步操作，结果在onDescriptorWrite回调
        } else {
            Log.e(TAG, "无法找到通知描述符（CCCD）");
        }
}


    ///////////////////////////////////  发送数据  ///////////////////////////////////////////////

    /**
     * 发送消息  byte[]数组
     * @param msg  消息
     * @return  true  false
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public boolean sendMessage(String msg){
        // 使用ASCII编码发送字符串
        byte[] asciiBytes = com.example.backpackapplication.util.TypeConversion.stringToAsciiBytes(msg);
        boolean b = writeCharacteristic.setValue(asciiBytes);
        Log.d(TAG, "写特征设置值结果：" + b + "，原始内容: " + msg);

        if(writeCharacteristic == null){
            Log.e(TAG,"sendMessage(byte[])-->writeGattCharacteristic == null");
            return false;
        }
        if(mBluetoothGatt == null){
            Log.e(TAG,"sendMessage(byte[])-->mBluetoothGatt == null");
            return false;
        }

        return mBluetoothGatt.writeCharacteristic(writeCharacteristic);
    }


    ///////////////////////////////////  断开连接  ///////////////////////////////////////////////
    /**
     * 断开连接
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void disConnectDevice(){
        if(mBluetoothGatt == null){
            Log.e(TAG,"disConnectDevice-->bluetoothGatt == null");
            return;
        }

        //系统断开
        mBluetoothGatt.disconnect();
        //close()方法应该放在断开回调处，放在此处，会没有回调信息
//        mBluetoothGatt.close();
    }



    /**
     * 检测手机是否支持4.0蓝牙
     * @param context  上下文
     * @return true--支持4.0  false--不支持4.0
     */
    private boolean checkBle(Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {  //API 18 Android 4.3
            bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if(bluetoothManager == null){
                return false;
            }
            bluetooth4Adapter = bluetoothManager.getAdapter();  //BLUETOOTH权限
            if(bluetooth4Adapter == null){
                return false;
            }else{
                Log.d(TAG,"该设备支持蓝牙4.0");
                return true;
            }
        }else{
            return false;
        }
    }

    /**
     * 获取蓝牙状态
     */
    public boolean isEnable(){
        if(bluetooth4Adapter == null){
            return false;
        }
        return bluetooth4Adapter.isEnabled();
    }

    /**
     * 打开蓝牙
     * @param isFast  true 直接打开蓝牙  false 提示用户打开
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void openBluetooth(Context context, boolean isFast){
        if(!isEnable()){
            if(isFast){
                Log.d(TAG,"直接打开手机蓝牙");
                bluetooth4Adapter.enable();  //BLUETOOTH_ADMIN权限
            }else{
                Log.d(TAG,"提示用户去打开手机蓝牙");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                context.startActivity(enableBtIntent);
            }
        }else{
            Log.d(TAG,"手机蓝牙状态已开");
        }
    }

    /**
     * 直接关闭蓝牙
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public void closeBluetooth(){
        if(bluetooth4Adapter == null)
            return;

        bluetooth4Adapter.disable();
    }


    /**
     * 本地蓝牙是否处于正在扫描状态
     * @return true false
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    public boolean isDiscovery(){
        if(bluetooth4Adapter ==null){
            return false;
        }
        return bluetooth4Adapter.isDiscovering();
    }
}
