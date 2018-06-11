package com.nuvopoint.cordova;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;

import com.acs.bluetooth.*;
import com.google.gson.Gson;

public class Acs extends CordovaPlugin {
    private static final String CONNECT_READER = "connectReader";
    private static final String AUTHENTICATE = "authenticate";
    private static final String LISTEN_FOR_ADPU_RESPONSE = "listenForAdpuResponse";
    private static final String LISTEN_FOR_CARD_STATUS_AVAILABLE = "listenForCardStatusAvailable";

    private static final String START_POLLING = "startPolling";
    private static final String STOP_POLLING = "stopPolling";
    private static final String START_SCAN = "startScan";
    private static final String STOP_SCAN = "stopScan";

    private static final String REQUEST_CARD_ID = "requestCardId";
    private static final String GET_CARD_STATUS = "getCardStatus";
    private static final String GET_CONNECTION_STATE = "getConnectionState";


    private static final int REQUEST_ENABLE_BT = 1;
    private final int REQUEST_PERMISSION_ACCESS_COARSE_LOCATION = 1;

    private static final byte[] DEFAULT_1255_MASTER_KEY = new byte[]{65, 67, 82, 49, 50, 53, 53, 85, 45, 74, 49, 32, 65, 117, 116, 104};
    private static final byte[] DEFAULT_REQUEST_CARD_ID = new byte[]{(byte) 0xFF, (byte) 0xCA, (byte) 0x0, (byte) 0x0, (byte) 0x0};
    private static final byte[] AUTO_POLLING_START = {(byte) 0xE0, 0x00, 0x00, 0x40, 0x01};
    private static final byte[] AUTO_POLLING_STOP = {(byte) 0xE0, 0x00, 0x00, 0x40, 0x00};


    private Activity pluginActivity;
    /* Bluetooth GATT client. */
    private BluetoothGatt mBluetoothGatt;
    private BluetoothReaderGattCallback mGattCallback;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothReaderManager mBluetoothReaderManager = new BluetoothReaderManager();
    private BluetoothReader mBluetoothReader;
    private BluetoothLeScanner mBluetoothLeScanner;

    //For Scanning
    private boolean mScanning;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;
    private int connectionState;
    private ArrayList<BTScanResult> foundDevices;

    // Idk
    private CallbackContext startScanCallbackContext;
    private CallbackContext connectReaderCallbackContext;
    private CallbackContext authenticationCallbackContext;
    private CallbackContext cardAvailableCallbackContext;
    private CallbackContext adpuResponseCallbackContext;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        mHandler = new Handler();

        // Initialize bluetooth manager and adapter and enable bluetooth if it's disabled.
        pluginActivity = cordova.getActivity();
        mBluetoothManager = (BluetoothManager) pluginActivity.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            pluginActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // Get the permissions for scanning BT devices
        if (ContextCompat.checkSelfPermission(pluginActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION};
            ActivityCompat.requestPermissions(pluginActivity, permissions, REQUEST_PERMISSION_ACCESS_COARSE_LOCATION);
        }
    }

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) {
        if (action.equalsIgnoreCase(CONNECT_READER)) {
            cordova.getThreadPool().execute(() -> connectReader(callbackContext, data));
        } else if (action.equalsIgnoreCase(START_SCAN)) {
            cordova.getThreadPool().execute(() -> startScan(callbackContext));
        } else if (action.equalsIgnoreCase(STOP_SCAN)) {
            cordova.getThreadPool().execute(() -> stopScan());
        } else if (action.equalsIgnoreCase(GET_CONNECTION_STATE)) {
            cordova.getThreadPool().execute(() -> getConnectionState(callbackContext));
        } else if (action.equalsIgnoreCase(LISTEN_FOR_ADPU_RESPONSE)) {
            adpuResponseCallbackContext = callbackContext;
        } else if (action.equalsIgnoreCase(AUTHENTICATE)) {
            cordova.getThreadPool().execute(() -> authenticate(callbackContext));
        } else if (action.equalsIgnoreCase(START_POLLING)) {
            cordova.getThreadPool().execute(() -> startPolling(callbackContext));
        } else if (action.equalsIgnoreCase(STOP_POLLING)) {
            cordova.getThreadPool().execute(() -> stopPolling(callbackContext));
        } else if (action.equalsIgnoreCase(REQUEST_CARD_ID)) {
            cordova.getThreadPool().execute(() -> requestId());
        } else if (action.equalsIgnoreCase(LISTEN_FOR_CARD_STATUS_AVAILABLE)) {
            cardAvailableCallbackContext = callbackContext;
        } else if (action.equalsIgnoreCase(GET_CARD_STATUS)) {
            cordova.getThreadPool().execute(() -> getCardStatus(callbackContext));
        } else {
            return false;
        }

        return true;
    }

    private void authenticate(CallbackContext callbackContext) {
        authenticationCallbackContext = callbackContext;
        mBluetoothReader.authenticate(DEFAULT_1255_MASTER_KEY);
    }

    private void startPolling(CallbackContext callbackContext) {
        boolean result = mBluetoothReader.transmitEscapeCommand(AUTO_POLLING_START);
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));
    }

    private void stopPolling(CallbackContext callbackContext) {
        boolean result = mBluetoothReader.transmitEscapeCommand(AUTO_POLLING_STOP);
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));
    }


    public void startScan(CallbackContext callbackContext) {
        if (mBluetoothManager == null || mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "Bluetooth error, please check your bluetooth setting!"));
            return;
        }

        if (startScanCallbackContext != null) {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "Scan already in progress"));
            return;
        }

        foundDevices = new ArrayList<BTScanResult>();

        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        pluginActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

        // Stops scanning after a predefined scan period.
        startScanCallbackContext = callbackContext;
        mHandler.postDelayed(() -> {
            if (mScanning) {
                mScanning = false;
                mBluetoothLeScanner.stopScan(mScanCallback);
            }
            startScanCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "Scan complete"));
            startScanCallbackContext = null;
        }, SCAN_PERIOD);

        mScanning = true;
        mBluetoothLeScanner.startScan(mScanCallback);
    }

    public void stopScan() {
        if (mBluetoothManager == null || mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            return;
        }

        mScanning = false;
        mBluetoothLeScanner.stopScan(mScanCallback);
        mHandler.removeCallbacksAndMessages(null);
        startScanCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "Scan complete"));
        startScanCallbackContext = null;
    }


    ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Gson gson = new Gson();
            String resultStr = gson.toJson(result);
            BTScanResult resultBt = gson.fromJson(resultStr, BTScanResult.class);
            if (!checkIfAlreadyAdded(resultBt)) {
                try {
                    foundDevices.add(resultBt);
                    PluginResult pluginRes = new PluginResult(PluginResult.Status.OK, resultStr);
                    pluginRes.setKeepCallback(true);
                    startScanCallbackContext.sendPluginResult(pluginRes);
                } catch (Throwable e) {
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Gson gson = new Gson();
            for (ScanResult sr : results) {
                String resultStr = gson.toJson(sr);
                BTScanResult resultBt = gson.fromJson(resultStr, BTScanResult.class);
                if(!checkIfAlreadyAdded(resultBt)){
                    try {
                        foundDevices.add(resultBt);
                        PluginResult pluginRes = new PluginResult(PluginResult.Status.OK, resultStr);
                        pluginRes.setKeepCallback(true);
                        startScanCallbackContext.sendPluginResult(pluginRes);
                    } catch (Throwable e) {
                    }
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            startScanCallbackContext.error(errorCode);
        }
    };

    private boolean checkIfAlreadyAdded(BTScanResult toCheck) {
        boolean found = false;
        for (BTScanResult temp : foundDevices) {
            found = temp.mDevice.mAddress.equals(toCheck.mDevice.mAddress);
            if (found){
                break;
            }
        }
        return found;
    }


    private void connectReader(final CallbackContext callbackContext, JSONArray data) {
        try {
            // Get the device address
            String myDeviceAddress = data.getString(0);
            connectReaderCallbackContext = callbackContext;

            // Remove any existing connections
            if (mBluetoothGatt != null) {
                mBluetoothGatt.disconnect();
                mBluetoothGatt.close();
                mBluetoothGatt = null;
            }

            // Create a new GATT Callback and initialize listeners
            mGattCallback = new BluetoothReaderGattCallback();
            initializeGattCallbackListeners();

            // Reinitialize BluetoothReaderManager Listeners
            initializeBluetoothReaderManagerListeners();

// Get the device by it's address and connect to it with the callBack
            final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(myDeviceAddress);
            mBluetoothGatt = device.connectGatt(cordova.getContext(), false, mGattCallback);
        } catch (Exception e) {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "Something went wrong with the connection"));
        }
    }


    private void initializeGattCallbackListeners() {
        mGattCallback.setOnConnectionStateChangeListener((final BluetoothGatt gatt, final int state, final int newState) -> {
            connectionState = newState;
            if (connectionState == BluetoothProfile.STATE_CONNECTED) {
                if (mBluetoothReaderManager != null) {
                    mBluetoothReaderManager.detectReader(gatt, mGattCallback);
                }
            } else if (connectionState == BluetoothProfile.STATE_DISCONNECTED) {
                mBluetoothReader = null;
                // Release resources occupied by Bluetooth GATT client.
                if (mBluetoothGatt != null) {
                    mBluetoothGatt.close();
                    mBluetoothGatt = null;
                }
            }
        });
    }

    private void initializeBluetoothReaderManagerListeners() {
        mBluetoothReaderManager.setOnReaderDetectionListener((BluetoothReader bluetoothReader) -> {
            mBluetoothReader = bluetoothReader;
            mBluetoothReader.enableNotification(true);
            initializeBluetoothReaderListeners();
            this.connectReaderCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, "Connected"));
        });
    }

    private void initializeBluetoothReaderListeners() {
        mBluetoothReader.setOnAuthenticationCompleteListener((BluetoothReader bluetoothReader, int errorCode) -> {
            if (errorCode == BluetoothReader.ERROR_SUCCESS) {
                authenticationCallbackContext.success();
            } else {
                authenticationCallbackContext.error(errorCode);
            }
        });


        mBluetoothReader.setOnCardStatusAvailableListener((BluetoothReader bluetoothReader, int cardStatus, int errorCode) -> {
            if (cardAvailableCallbackContext != null) {
                PluginResult pluginRes = new PluginResult(PluginResult.Status.OK, cardStatus);
                pluginRes.setKeepCallback(true);
                startScanCallbackContext.sendPluginResult(pluginRes);
            }
        });

        mBluetoothReader.setOnResponseApduAvailableListener((BluetoothReader bluetoothReader, byte[] apdu, int errorCode) -> {
            if (adpuResponseCallbackContext != null) {
                PluginResult pluginRes = new PluginResult(PluginResult.Status.OK, bytesToHex(apdu));
                pluginRes.setKeepCallback(true);
                adpuResponseCallbackContext.sendPluginResult(pluginRes);
            }
        });


        mBluetoothReader.setOnCardPowerOffCompleteListener((BluetoothReader bluetoothReader, int result) -> {
            // TODO: Show the power off card response.
        });

        mBluetoothReader.setOnCardStatusChangeListener((BluetoothReader bluetoothReader, int cardStatus) -> {
            // TODO: Show the card status.
        });

    }


    private void getConnectionState(final CallbackContext callbackContext) {
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, connectionState));
    }

    private void getCardStatus(final CallbackContext callbackContext) {
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, mBluetoothReader.getCardStatus()));
    }

    private void requestId() {
        mBluetoothReader.transmitApdu(DEFAULT_REQUEST_CARD_ID);
    }


    public static String bytesToHex(byte[] bytes) {
        final char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}

class BTScanResult {
    public BTScanResultDevice mDevice;
}

class BTScanResultDevice {
    public String mAddress;
}

