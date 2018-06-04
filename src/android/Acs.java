package com.nuvopoint.cordova;

import android.app.Activity;
import android.bluetooth.le.ScanResult;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.util.Log;


import android.os.Handler;
import android.bluetooth.BluetoothProfile;
import android.support.v4.content.ContextCompat;
import android.support.v4.app.ActivityCompat;
import android.content.pm.PackageManager;
import android.Manifest;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;


import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;

import org.json.JSONArray;
import org.json.JSONException;

import com.acs.bluetooth.*;

import com.google.gson.Gson;

import java.util.List;


public class Acs extends CordovaPlugin {
    private static final String TAG = "NFCReader";
    private static final String CONNECT_READER = "connectReader";
    private static final String START_SCAN = "startScan";
    private static final String STOP_SCAN = "stopScan";
    private static final String GET_CONNECTION_STATUS = "getConnectionStatus";
    private static final String GET_CARD_STATUS = "getCardStatus";
    private static final String LISTEN_FOR_ADPU = "listenForADPU";
    private static final String STOP_LISTENING_FOR_ADPU = "stopListeningForADPU";
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
    private BluetoothReader mBluetoothReader;

    //For Scanning
    private Context mContext;
    private boolean mScanning;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;

    private CallbackContext callbackContext;
    private CallbackContext adpuContext;


    private int connectionState;

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

        this.mBluetoothReaderManager.setOnReaderDetectionListener((BluetoothReader bluetoothReader) -> {
            this.mBluetoothReader = bluetoothReader;
            this.mBluetoothReader.enableNotification(true);
            return;
        });
    }

    void enableListeners() {
        mBluetoothReader.setOnCardPowerOffCompleteListener((BluetoothReader bluetoothReader, int result) -> {
            // TODO: Show the power off card response.
        });

        mBluetoothReader.setOnCardStatusChangeListener((BluetoothReader bluetoothReader, int cardStatus) -> {
            // TODO: Show the card status.
        });
    }


    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
        if (action.equalsIgnoreCase(CONNECT_READER)) {
            cordova.getThreadPool().execute(() -> this.connectReader(callbackContext, data));
            return true;
        }
        if (action.equalsIgnoreCase(START_SCAN)) {
            cordova.getThreadPool().execute(() -> this.startScan(callbackContext));
            return true;
        }
        if (action.equalsIgnoreCase(STOP_SCAN)) {
            cordova.getThreadPool().execute(() -> this.stopScan());
            return true;
        }
        if (action.equalsIgnoreCase(GET_CONNECTION_STATUS)) {
            cordova.getThreadPool().execute(() -> this.getConnectionStatus(callbackContext));
            return true;
        }
        if (action.equalsIgnoreCase(LISTEN_FOR_ADPU)) {
            cordova.getThreadPool().execute(() -> this.listenForADPU(callbackContext));
            return true;
        }
        if (action.equalsIgnoreCase(STOP_LISTENING_FOR_ADPU)) {
            cordova.getThreadPool().execute(() -> this.stopListeningForADPU());
            return true;
        }
        if (action.equalsIgnoreCase(GET_CARD_STATUS)) {
            cordova.getThreadPool().execute(() -> this.getCardStatus(callbackContext));
            return true;
        } else {
            return false;
        }
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
        BluetoothLeScanner mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (enable) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            cordova.getActivity().startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

            // Stops scanning after a predefined scan period.
            Log.d(TAG, "Scanning!!!");
            this.startScanCallbackContext = callbackContext;
            mHandler.postDelayed(() -> {
                if (mScanning) {
                    mScanning = false;
                    mBluetoothLeScanner.stopScan(mScanCallback);
                }
                Log.d("BTReader", "Scan Reader Complete!!!");
                this.startScanCallbackContext.success("Scan complete!");
                this.startScanCallbackContext = null;
            }, SCAN_PERIOD);
            mScanning = true;
            mBluetoothLeScanner.startScan(mScanCallback);
        } else {
            mScanning = false;
            mBluetoothLeScanner.stopScan(mScanCallback);
            mHandler.removeCallbacksAndMessages(null);
            this.startScanCallbackContext.success("Scan complete");
            this.startScanCallbackContext = null;
        }
    }


    ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Gson gson = new Gson();
            String resultStr;
            try {
                resultStr = gson.toJson(result);
            } catch (Throwable e) {
                if (result != null) {
                    resultStr = result.toString();
                } else {
                    resultStr = e.getMessage();
                }
            }
            PluginResult pluginRes = new PluginResult(PluginResult.Status.OK, resultStr);
            pluginRes.setKeepCallback(true);
            startScanCallbackContext.sendPluginResult(pluginRes);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Gson gson = new Gson();
            for (ScanResult sr : results) {
                String resultStr;
                try {
                    resultStr = gson.toJson(sr);
                } catch (Throwable e) {
                    if (sr != null) {
                        resultStr = sr.toString();
                    } else {
                        resultStr = e.getMessage();
                    }
                }


                PluginResult pluginRes = new PluginResult(PluginResult.Status.OK, resultStr);
                pluginRes.setKeepCallback(true);
                startScanCallbackContext.sendPluginResult(pluginRes);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            startScanCallbackContext.error(errorCode);
        }
    };


    private void connectReader(final CallbackContext callbackContext, JSONArray data) {
        try {
            String myDeviceAddress = data.getString(0);

            if (this.mBluetoothGatt != null) {
                this.mBluetoothGatt.disconnect();
                this.mBluetoothGatt.close();
                this.mBluetoothGatt = null;
            }

            this.mGattCallback = new BluetoothReaderGattCallback();
            mGattCallback.setOnConnectionStateChangeListener((final BluetoothGatt gatt, final int state, final int newState) -> {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    // Detect the reader.
                    if (mBluetoothReaderManager != null) {
                        mBluetoothReaderManager.detectReader(gatt, mGattCallback);
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    mBluetoothReader = null;

                    // Release resources occupied by Bluetooth GATT client.
                    if (mBluetoothGatt != null) {
                        mBluetoothGatt.close();
                        mBluetoothGatt = null;
                    }
                }
                this.connectionState = newState;
            });


            final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(myDeviceAddress);
            this.mBluetoothGatt = device.connectGatt(this.cordova.getContext(), false, mGattCallback);


            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, this.mBluetoothGatt.GATT_SUCCESS));
        } catch (Exception e) {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "Something went wrong with the connection"));
        }


    }


    private void getConnectionStatus(final CallbackContext callbackContext) {
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, this.connectionState));
    }


    private void getCardStatus(final CallbackContext callbackContext) {
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, mBluetoothReader.getCardStatus()));
    }

    private void listenForADPU(final CallbackContext callbackContext) {
        this.adpuContext = callbackContext;
        mBluetoothReader.setOnResponseApduAvailableListener((BluetoothReader bluetoothReader, byte[] apdu, int errorCode) -> {
            Gson gson = new Gson();
            String resultStr = gson.toJson(apdu);
            PluginResult pluginRes = new PluginResult(PluginResult.Status.OK, resultStr);
            pluginRes.setKeepCallback(true);
            startScanCallbackContext.sendPluginResult(pluginRes);
        });
    }

    private void stopListeningForADPU(){
        this.adpuContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "finished"));
        this.mBluetoothReader.setOnResponseApduAvailableListener(null);
    }

}
