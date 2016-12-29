package com.example.hanks.mybeacon;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.Tag;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.WindowDecorActionBar;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    ListView listView;
    TextView tvBytesToHex,tvName,tvMAC,tvUUID,tvMajor,tvMinor,tvTxpower,tvRssi,tvDistance;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private static final int REQUEST_ENABLE_BT = 1;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 1000; // 掃描頻率10 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findviews();

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
            //Toast.makeText(this,"請開啟藍芽功能",Toast.LENGTH_SHORT).show();
            //若沒有開啟則顯示允許用戶打開藍牙的系統活動
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, REQUEST_ENABLE_BT);
        }

        //scanLeDevice(true);
        mBluetoothAdapter.startLeScan(mLeScanCallBack);

    }

    void findviews(){
        listView = (ListView)findViewById(R.id.listView);
        tvBytesToHex = (TextView)findViewById(R.id.tv1);
        tvName = (TextView)findViewById(R.id.tv2);
        tvMAC = (TextView)findViewById(R.id.tv3);
        tvUUID = (TextView)findViewById(R.id.tv4);
        tvMajor = (TextView)findViewById(R.id.tv5);
        tvMinor = (TextView)findViewById(R.id.tv6);
        tvTxpower = (TextView)findViewById(R.id.tv7);
        tvRssi = (TextView)findViewById(R.id.tv8);
        tvDistance = (TextView)findViewById(R.id.tv9);
    }

    // App在onPause生命週期時，停止掃瞄
    @Override
    protected void onPause() {
        super.onPause();
        //scanLeDevice(false);
        mBluetoothAdapter.stopLeScan(mLeScanCallBack);
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
            //Log.d("TAG", "BLE device: " + device.getName());
            int startByte = 2;
            boolean patternFound = false;
            // 尋找iBeacon
            // 先依序找第2到第8陣列的元素
            while (startByte <= 5){
                // Identifies an iBeacon
                if (((int)scanRecord[startByte + 2] & 0xff) == 0x02 &&
                        // Indentifies correct data length
                        ((int)scanRecord[startByte + 3] & 0xff) == 0x15){
                    patternFound = true;
                    break;
                }
                startByte++;
            }
            // 如果找到的話
            if (patternFound){
                mBluetoothAdapter.stopLeScan(mLeScanCallBack);
                // 轉換16進制
                byte[] uuidBytes = new byte[16];
                // 來源，起始位置
                System.arraycopy(scanRecord, startByte + 4, uuidBytes, 0, 16);
                String hexString = bytesToHex(uuidBytes);

                // UUID
                String uuid = hexString.substring(0, 8) + "-"
                        + hexString.substring(8, 12) + "-"
                        + hexString.substring(12, 16) + "-"
                        + hexString.substring(16, 20) + "-"
                        + hexString.substring(20, 32);

                // Major
                int major = (scanRecord[startByte + 20] & 0xff) * 0x100
                        + (scanRecord[startByte + 21] & 0xff);

                // Minor
                int minor = (scanRecord[startByte + 22] & 0xff) * 0x100
                        + (scanRecord[startByte + 23] & 0xff);

                // txpower
                int txPower = (scanRecord[startByte + 24]);
                double distance = calculateAccuracy(txPower, rssi);

                Log.d("BLE",bytesToHex(scanRecord));
                tvBytesToHex.setText("ByteToHex: " + bytesToHex(scanRecord));

                Log.d("BLE", "Name：" + device.getName() + "\nMac：" + device.getAddress()
                        + " \nUUID：" + uuid + "\nMajor：" + major + "\nMinor："
                        + minor + "\nTxPower：" + txPower + "\nrssi：" + rssi);
                tvName.setText("Device Name: " + device.getName());
                tvMAC.setText("Device MAC: " + device.getAddress());
                tvUUID.setText("UUID: " + uuid);
                tvMajor.setText("Major: " + major);
                tvMinor.setText("Minor: " + minor);
                tvTxpower.setText("TxPower: " + txPower);
                tvRssi.setText("Rssi: " + rssi);

                Log.d("BLE","distance："+calculateAccuracy(txPower, rssi));
                tvDistance.setText("Distance: " + calculateAccuracy(txPower, rssi));
            }
        }
    };

    // 將來源轉換為16進制
    public String bytesToHex(byte[] bytes){
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++){
            int v = bytes[j] & 0xff;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    //計算距離 :
    //此方法是"即時"計算，所以很容易有大幅度的波動
    //建議是 : 累加後均分這樣穩定度相對的高( 收集約 15 次以上後均分 )
    public double calculateAccuracy(int txPower, double rssi) {
        if (rssi == 0) {
            return -1.0;
        }

        double ratio = rssi * 1.0 / txPower;

        if (ratio < 1.0) {
            return Math.pow(ratio, 10);
        } else {
            double accuracy = (0.89976) * Math.pow(ratio, 7.7095) + 0.111;
            return accuracy;
        }
    }
}
