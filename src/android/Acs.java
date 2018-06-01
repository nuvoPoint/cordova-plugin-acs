package com.nuvopoint.cordova;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.util.Log;


import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v4.app.ActivityCompat;
import android.content.pm.PackageManager;
import android.Manifest;
import 	android.bluetooth.le.BluetoothLeScanner;


import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;

import org.json.JSONArray;
import org.json.JSONException;

import com.acs.bluetooth.*;


public class Acs extends CordovaPlugin {
    private static final String TAG = "NFCReader";
    private static final String CONNECT_READER = "connectReader";
    private static final String SCAN_FOR_DEVICES = "scanForDevices";
    private static final int REQUEST_ENABLE_BT = 1;
    private final int REQUEST_PERMISSION_ACCESS_FINE_LOCATION = 1;
    private final int REQUEST_PERMISSION_ACCESS_COARSE_LOCATION = 1;


    private Activity pluginActivity;
    /* Bluetooth GATT client. */
    private BluetoothGatt mBluetoothGatt;
    private BluetoothReaderGattCallback mGattCallback;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothReaderManager mBluetoothReaderManager = new BluetoothReaderManager();

    //For Scanning
    private Context mContext;
    private boolean mScanning;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;

    private CallbackContext callbackContext;


    // Idk
    private CallbackContext startScanCallbackContext;


    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        // What does handler do ???
        this.mHandler = new Handler();

        // Initialize bluetooth manager and adapter and enable bluetooth if it's disabled.
        this.pluginActivity = cordova.getActivity();
        this.mBluetoothManager = (BluetoothManager) pluginActivity.getSystemService(Context.BLUETOOTH_SERVICE);
        this.mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            cordova.getActivity().startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        if (ContextCompat.checkSelfPermission(cordova.getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION};
            ActivityCompat.requestPermissions(cordova.getActivity(), permissions, REQUEST_PERMISSION_ACCESS_COARSE_LOCATION);
        }
    }


    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
        if (action.equalsIgnoreCase(CONNECT_READER)) {
            cordova.getThreadPool().execute(() -> {
                connectReader(callbackContext, data);
            });
        }
        if (action.equalsIgnoreCase(SCAN_FOR_DEVICES)) {
            cordova.getThreadPool().execute(() -> {
                scanForDevices(callbackContext);
            });
        } else {
            return false;
        }

        return true;
    }

//  private void scanLeDevice() {
//    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//    cordova.getActivity().startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//    // Stops scanning after a predefined scan period.
//    mHandler.postDelayed(() -> {
//      mScanning = false;
//      mBluetoothAdapter.stopLeScan(mLeScanCallback);
//    }, SCAN_PERIOD);
//    mScanning = true;
//    mBluetoothAdapter.startLeScan(mLeScanCallback);
//  }


    public void startScan(CallbackContext startScanCallbackContext) {
        if (this.mBluetoothManager == null || this.mBluetoothAdapter == null || !this.mBluetoothAdapter.isEnabled()) {
            startScanCallbackContext.error("Bluetooth error, please check your bluetooth setting!");
            return;
        }

        if (this.startScanCallbackContext != null) {
            Log.d(TAG, "Already in Scanning!");
            this.startScanCallbackContext.error("Already in Scanning!");
            this.startScanCallbackContext = startScanCallbackContext;
            return;
        }

        Log.d(TAG, "startScan!!!");
        this.scanLeDevice(true, startScanCallbackContext);
    }

    public void stopScan() {
        if (mBluetoothManager == null || mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            this.startScanCallbackContext.error("Bluetooth error, please check your bluetooth setting!");
            return;
        }

        if (this.startScanCallbackContext != null) {
            this.scanLeDevice(false, null);
            return;
        }
        Log.d(TAG, "Already not in Scanning!");
    }

    private synchronized void scanLeDevice(boolean enable, final CallbackContext callbackContext) {
        if (enable) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            cordova.getActivity().startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);


            BluetoothLeScanner mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner()
            // Stops scanning after a predefined scan period.
            Log.d(TAG, "Scanning!!!");
            this.startScanCallbackContext = callbackContext;
            mHandler.postDelayed(() -> {
                if (mScanning) {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
                Log.d("BTReader", "Scan Reader Complete!!!");
                this.startScanCallbackContext.success("Scan complete!");
                this.startScanCallbackContext = null;
            }, SCAN_PERIOD);
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mHandler.removeCallbacksAndMessages(null);
            this.startScanCallbackContext.success("Scan complete");
            this.startScanCallbackContext = null;
        }
    }


//  private void scanLeDevice(final CallbackContext callbackContext) {
//    if (enable) {
//      // Stops scanning after a predefined scan period.
//      mHandler.postDelayed(new Runnable() {
//        @Override
//        public void run() {
//          mScanning = false;
//          mBluetoothAdapter.stopLeScan(mLeScanCallback);
//        }
//      }, SCAN_PERIOD);
//      mScanning = true;
//      mBluetoothAdapter.startLeScan(mLeScanCallback);
//    } else {
//      mScanning = false;
//      mBluetoothAdapter.stopLeScan(mLeScanCallback);
//    }
//  }

    private LeScanCallback mLeScanCallback = ((final BluetoothDevice device, int rssi, byte[] scanRecord) -> {

        PluginResult result = new PluginResult(PluginResult.Status.OK, device.toString());
        result.setKeepCallback(true);
        callbackContext.sendPluginResult(result);
    });


    private void connectReader(final CallbackContext callbackContext, JSONArray data) {
        try {
            String myDeviceAddress = data.getString(0);
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, myDeviceAddress));
        } catch (Exception e) {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "Can't parse currency"));
        }


        // BLUETOOTH_SERVICE
        // BluetoothManager bluetoothManager = (BluetoothManager) this.getSystemService(Context.);
        // if (bluetoothManager == null) {
        //     return false;
        // }

        // BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        // if (bluetoothAdapter == null) {
        //     return false;
        // }

        // /*
        //  * Connect Device.
        //  */
        // /* Clear old GATT connection. */
        //     mBluetoothGatt.disconnect();
        //     mBluetoothGatt.close();
        //     mBluetoothGatt = null;

        // /* Create a new connection. */
        // final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(mDeviceAddress);

        // if (device == null) {
        //     return false;
        // }

        // /* Connect to GATT server. */
        // mBluetoothGatt = device.connect(mDeviceAddress, callbackContext);
        // return true;
    }

}
