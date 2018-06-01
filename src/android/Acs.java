package com.nuvopoint.cordova;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothAdapter.LeScanCallback;

import android.os.Handler;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;

import org.json.JSONArray;
import org.json.JSONException;

import com.acs.bluetooth.*;


public class Acs extends CordovaPlugin {
    private static final String CONNECT_READER = "connectReader";
    private static final String SCAN_FOR_DEVICES = "scanForDevices";
    private static final int REQUEST_ENABLE_BT = 1;


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


    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        this.mHandler = new Handler();
        this.pluginActivity = cordova.getActivity();
        mBluetoothManager = (BluetoothManager) pluginActivity.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            cordova.getActivity().startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }


    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
        if (action.equalsIgnoreCase(CONNECT_READER)) {
            connectReader(callbackContext, data);
        }
        if (action.equalsIgnoreCase(SCAN_FOR_DEVICES)) {
            scanForDevices(callbackContext);
        } else {
            return false;
        }

        return true;
    }

    private void scanForDevices(CallbackContext callbackContext) {
        this.callbackContext = callbackContext;
        this.scanLeDevice();
    }

    private void scanLeDevice() {
        // Stops scanning after a predefined scan period.
        mHandler.postDelayed(() -> {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }, SCAN_PERIOD);
        mScanning = true;
        mBluetoothAdapter.startLeScan(mLeScanCallback);
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
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, device.toString()));
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
