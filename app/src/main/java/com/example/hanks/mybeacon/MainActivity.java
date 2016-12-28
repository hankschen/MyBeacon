package com.example.hanks.mybeacon;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 1;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 1000; // 掃描頻率10 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler();

        bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        //檢查手機硬體是否為BLE裝置
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
            Toast.makeText(this,"手機不支援BLE",Toast.LENGTH_SHORT).show();
            finish();
        }

        //檢查手機是否開啟藍芽功能
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()){
            Toast.makeText(this,"請開啟藍芽功能",Toast.LENGTH_SHORT).show();
            //若沒有開啟則顯示允許用戶打開藍牙的系統活動
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, REQUEST_ENABLE_BT);
        }else{
            scanLeDevice(true);
        }
    }

    // App在onPause生命週期時，停止掃瞄
    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
    }

    // 掃瞄藍牙裝置自訂方法
    private void scanLeDevice(final boolean enable){
        if (enable){
            // postDelayed(Runnable r, long delayMillis)
            // Causes the Runnable r to be added to the message queue, to be run after the specified amount of time elapses.
            // 在經過指定的delayMillis時間後，才將 "r" 加到消息佇列中
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothAdapter.stopLeScan(mLeScanCallBack);
                }
            }, SCAN_PERIOD);
            mBluetoothAdapter.startLeScan(mLeScanCallBack);
        }else {
            mBluetoothAdapter.stopLeScan(mLeScanCallBack);
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallBack = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            //搜尋回饋
            Log.d("TAG", "BLE device: " + device.getName());
        }
    };
}
