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
    private static final String DISCONNECT_READER = "disconnectReader";
    private static final String ENABLE_NOTIFICATIONS = "enableNotifications";
    private static final String AUTHENTICATE = "authenticate";
    private static final String LISTEN_FOR_ADPU_RESPONSE = "listenForAdpuResponse";
    private static final String LISTEN_FOR_CARD_STATUS = "listenForCardStatus";
    private static final String LISTEN_FOR_CONNECTION_STATE = "listenForConnectionState";
    private static final String START_POLLING = "startPolling";
    private static final String STOP_POLLING = "stopPolling";
    private static final String START_SCAN = "startScan";
    private static final String STOP_SCAN = "stopScan";
    private static final String REQUEST_CARD_ID = "requestCardId";
    private static final String REQUEST_TURN_OFF_SLEEP_MODE = "turnOffSleepMode";


    private static final int REQUEST_ENABLE_BT = 1;
    private final int REQUEST_PERMISSION_ACCESS_COARSE_LOCATION = 1;

    private static final byte[] DEFAULT_1255_MASTER_KEY = new byte[]{65, 67, 82, 49, 50, 53, 53, 85, 45, 74, 49, 32, 65, 117, 116, 104};
    private static final byte[] DEFAULT_REQUEST_CARD_ID = new byte[]{(byte) 0xFF, (byte) 0xCA, (byte) 0x0, (byte) 0x0, (byte) 0x0};
    private static final byte[] TURN_OFF_SLEEP_MODE =  new byte[]{(byte) 0xE0, (byte) 0x00, (byte) 0x00, (byte) 0x48, (byte) 0x04};
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
    private ArrayList<BTScanResult> foundDevices;

    // Callback contexts
    private CallbackContext startScanCallbackContext;
    private CallbackContext connectReaderCallbackContext;
    private CallbackContext disconnectReaderCallbackContext;
    private CallbackContext cardStatusCallbackContext;
    private CallbackContext adpuResponseCallbackContext;
    private CallbackContext connectionStateCallbackContext;

    private int currentState = BluetoothReader.STATE_DISCONNECTED;

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
        } else if (action.equalsIgnoreCase(DISCONNECT_READER)) {
            cordova.getThreadPool().execute(() -> disconnectReader(callbackContext));
        } else if (action.equalsIgnoreCase(START_SCAN)) {
            cordova.getThreadPool().execute(() -> startScan(callbackContext));
        } else if (action.equalsIgnoreCase(STOP_SCAN)) {
            cordova.getThreadPool().execute(() -> stopScan());
        } else if (action.equalsIgnoreCase(LISTEN_FOR_CONNECTION_STATE)) {
            connectionStateCallbackContext = callbackContext;
        } else if (action.equalsIgnoreCase(LISTEN_FOR_ADPU_RESPONSE)) {
            adpuResponseCallbackContext = callbackContext;
        } else if (action.equalsIgnoreCase(LISTEN_FOR_CARD_STATUS)) {
            cardStatusCallbackContext = callbackContext;
        } else if (action.equalsIgnoreCase(AUTHENTICATE)) {
            cordova.getThreadPool().execute(() -> authenticate(callbackContext));
        } else if (action.equalsIgnoreCase(ENABLE_NOTIFICATIONS)) {
            cordova.getThreadPool().execute(() -> enableNotifications(callbackContext));
        } else if (action.equalsIgnoreCase(START_POLLING)) {
            cordova.getThreadPool().execute(() -> startPolling(callbackContext));
        } else if (action.equalsIgnoreCase(STOP_POLLING)) {
            cordova.getThreadPool().execute(() -> stopPolling(callbackContext));
        } else if (action.equalsIgnoreCase(REQUEST_CARD_ID)) {
            cordova.getThreadPool().execute(() -> requestId(callbackContext));
        } else if (action.equalsIgnoreCase(REQUEST_TURN_OFF_SLEEP_MODE)) {
            cordova.getThreadPool().execute(() -> turnOffSleepMode());
        } else {
            return false;
        }

        return true;
    }

    private void authenticate(CallbackContext callbackContext) {
        if (mBluetoothReader == null) {
            callbackContext.error("Reader is not initialized");
            return;
        }

        mBluetoothReader.setOnAuthenticationCompleteListener((BluetoothReader bluetoothReader, int errorCode) -> {
            if (errorCode == BluetoothReader.ERROR_SUCCESS) {
                callbackContext.success();
            } else {
                callbackContext.error(errorCode);
            }
        });

        boolean result = mBluetoothReader.authenticate(DEFAULT_1255_MASTER_KEY);
        if(!result){
            callbackContext.error("retry");
        }
    }

    private void enableNotifications(CallbackContext callbackContext) {
        if (mBluetoothReader == null) {
            callbackContext.error("Reader is not initialized");
            return;
        }

        mBluetoothReader.setOnEnableNotificationCompleteListener((BluetoothReader bluetoothReader, int errorCode) -> {
            if (errorCode == BluetoothReader.ERROR_SUCCESS) {
                callbackContext.success();
            } else {
                callbackContext.error(errorCode);
            }
        });

        mBluetoothReader.enableNotification(true);
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

        foundDevices = new ArrayList<>();
        startScanCallbackContext = callbackContext;
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        pluginActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

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

        mHandler.removeCallbacksAndMessages(null);
        mScanning = false;
        mBluetoothLeScanner.stopScan(mScanCallback);
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
                    PluginResult pluginRes = new PluginResult(PluginResult.Status.ERROR, e.getMessage());
                    pluginRes.setKeepCallback(true);
                    startScanCallbackContext.sendPluginResult(pluginRes);
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Gson gson = new Gson();
            for (ScanResult sr : results) {
                String resultStr = gson.toJson(sr);
                BTScanResult resultBt = gson.fromJson(resultStr, BTScanResult.class);
                if (!checkIfAlreadyAdded(resultBt)) {
                    try {
                        foundDevices.add(resultBt);
                        PluginResult pluginRes = new PluginResult(PluginResult.Status.OK, resultStr);
                        pluginRes.setKeepCallback(true);
                        startScanCallbackContext.sendPluginResult(pluginRes);
                    } catch (Throwable e) {
                        PluginResult pluginRes = new PluginResult(PluginResult.Status.ERROR, e.getMessage());
                        pluginRes.setKeepCallback(true);
                        startScanCallbackContext.sendPluginResult(pluginRes);
                    }
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            PluginResult pluginRes = new PluginResult(PluginResult.Status.ERROR, errorCode);
            pluginRes.setKeepCallback(true);
            startScanCallbackContext.sendPluginResult(pluginRes);
        }
    };

    private boolean checkIfAlreadyAdded(BTScanResult toCheck) {
        boolean found = false;
        for (BTScanResult temp : foundDevices) {
            found = temp.mDevice.mAddress.equals(toCheck.mDevice.mAddress);
            if (found) {
                break;
            }
        }
        return found;
    }


    private void connectReader(final CallbackContext callbackContext, JSONArray data) {
        try {
            // Check for already existing connections
            if(currentState != BluetoothReader.STATE_DISCONNECTED){
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "Disconnect reader before connecting new one"));
                return;
            }

            // Get the device address
            String myDeviceAddress = data.getString(0);
            connectReaderCallbackContext = callbackContext;


            // Create a new GATT Callback and initialize listeners
            mGattCallback = new BluetoothReaderGattCallback();
            initializeGattCallbackListeners();

            // Reinitialize BluetoothReaderManager Listeners
            initializeBluetoothReaderManagerListeners();

            // Get the device by it's address and connect to it with the callBack
            final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(myDeviceAddress);
            mBluetoothGatt = device.connectGatt(cordova.getContext(), false, mGattCallback);
        } catch (Exception e) {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, e.getMessage()));
        }
    }

    private void disconnectReader(final CallbackContext callbackContext) {
        try {
            if (mBluetoothGatt != null) {
                disconnectReaderCallbackContext = callbackContext;
                mBluetoothGatt.disconnect();
            } else {
                callbackContext.success();
            }
        } catch (Exception e) {
            callbackContext.error(e.getMessage());
        }
    }


    private void initializeGattCallbackListeners() {
        mGattCallback.setOnConnectionStateChangeListener((final BluetoothGatt gatt, final int state, final int newState) -> {
            currentState = newState;
            if (connectionStateCallbackContext != null) {
                PluginResult pluginRes = new PluginResult(PluginResult.Status.OK, getConnectionStateJSON(newState));
                pluginRes.setKeepCallback(true);
                connectionStateCallbackContext.sendPluginResult(pluginRes);
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (mBluetoothReaderManager != null) {
                    mBluetoothReaderManager.detectReader(gatt, mGattCallback);
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mBluetoothReader = null;
                if (mBluetoothGatt != null) {
                    mBluetoothGatt.close();
                    mBluetoothGatt = null;
                }
                if (disconnectReaderCallbackContext != null){
                    disconnectReaderCallbackContext.success();
                    disconnectReaderCallbackContext = null;
                }
            }
        });
    }

    private void initializeBluetoothReaderManagerListeners() {
        mBluetoothReaderManager.setOnReaderDetectionListener((BluetoothReader bluetoothReader) -> {
            if (!(bluetoothReader instanceof Acr1255uj1Reader)) {
                connectReaderCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "Reader type not supported"));
                mBluetoothReader = null;
                mBluetoothGatt.disconnect();
                return;
            }

            mBluetoothReader = bluetoothReader;
            initializeBluetoothReaderListeners();
            connectReaderCallbackContext.success();
        });
    }

    private void initializeBluetoothReaderListeners() {
        mBluetoothReader.setOnCardStatusChangeListener((BluetoothReader bluetoothReader, int cardStatus) -> {
            if (cardStatusCallbackContext != null) {
                PluginResult pluginRes = new PluginResult(PluginResult.Status.OK, getCardStatusJSON(cardStatus));
                pluginRes.setKeepCallback(true);
                cardStatusCallbackContext.sendPluginResult(pluginRes);
            }
        });

        mBluetoothReader.setOnResponseApduAvailableListener((BluetoothReader bluetoothReader, byte[] apdu, int errorCode) -> {
            if (adpuResponseCallbackContext != null) {
                PluginResult pluginRes = new PluginResult(PluginResult.Status.OK, bytesToHex(apdu));
                pluginRes.setKeepCallback(true);
                adpuResponseCallbackContext.sendPluginResult(pluginRes);
            }
        });
    }

    private void requestId(CallbackContext callbackContext) {
        if (mBluetoothReader == null) {
            callbackContext.error("Reader is not initialized");
            return;
        }

        boolean result = mBluetoothReader.transmitApdu(DEFAULT_REQUEST_CARD_ID);
        callbackContext.success(Boolean.toString(result));
    }

    private void turnOffSleepMode(){
        mBluetoothReader.transmitApdu(TURN_OFF_SLEEP_MODE);

    }

    private String getCardStatusJSON(int cardStatus) {
        Gson gson = new Gson();
        if (cardStatus == BluetoothReader.CARD_STATUS_ABSENT) {
            return gson.toJson(new StatusMessage(1, "Card is absent."));
        } else if (cardStatus == BluetoothReader.CARD_STATUS_PRESENT) {
            return gson.toJson(new StatusMessage(2, "Card is present."));
        } else if (cardStatus == BluetoothReader.CARD_STATUS_POWER_SAVING_MODE) {
            return gson.toJson(new StatusMessage(3, "Reader is in power saving mode."));
        } else if (cardStatus == BluetoothReader.CARD_STATUS_POWERED) {
            return gson.toJson(new StatusMessage(4, "Card is powered."));
        }
        return gson.toJson(new StatusMessage(0, "Card status is unknown."));
    }

    private String getConnectionStateJSON(int status) {
        Gson gson = new Gson();
        if (status == BluetoothReader.STATE_DISCONNECTED) {
            return gson.toJson(new StatusMessage(1, "Disconnected"));
        } else if (status == BluetoothReader.STATE_CONNECTED) {
            return gson.toJson(new StatusMessage(2, "Connected."));
        } else if (status == BluetoothReader.STATE_CONNECTING) {
            return gson.toJson(new StatusMessage(3, "Connecting"));
        } else if (status == BluetoothReader.STATE_DISCONNECTING) {
            return gson.toJson(new StatusMessage(4, "Disconnecting"));
        }
        return gson.toJson(new StatusMessage(0, "Status unknown."));
    }


    private static String bytesToHex(byte[] bytes) {
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

class StatusMessage {
    private int code;
    private String message;

    StatusMessage(int code, String message) {
        this.code = code;
        this.message = message;
    }
}

