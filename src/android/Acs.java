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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
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

public class Acs extends CordovaPlugin implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String CONNECT_READER = "connectReader";
    private static final String DISCONNECT_READER = "disconnectReader";
    private static final String ENABLE_NOTIFICATIONS = "enableNotifications";
    private static final String AUTHENTICATE = "authenticate";
    private static final String LISTEN_FOR_ADPU_RESPONSE = "listenForAdpuResponse";
    private static final String LISTEN_FOR_ESCAPE_RESPONSE = "listenForEscapeResponse";
    private static final String LISTEN_FOR_CARD_STATUS = "listenForCardStatus";
    private static final String LISTEN_FOR_CONNECTION_STATE = "listenForConnectionState";
    private static final String START_SCAN = "startScan";
    private static final String STOP_SCAN = "stopScan";
    private static final String TRANSMIT_ADPU_COMMAND = "transmitAdpuCommand";
    private static final String TRANSMIT_ESCAPE_COMMAND = "transmitEscapeCommand";
    private static final String REQUEST_TURN_ON_BT = "requestTurnOnBt";
    private static final String REQUEST_BT_PERMISSIONS = "requestBtPermissions";


    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSION_ACCESS_COARSE_LOCATION = 2;


    /* Error codes */
    private static final int ERR_UNKNOWN = 0;
    private static final int ERR_READER_NOT_INITIALIZED = 1;
    private static final int ERR_OPERATION_FAILED = 2;
    private static final int ERR_OPERATION_TIMED_OUT = 3;
    private static final int ERR_BT_IS_OFF = 4;
    private static final int ERR_BT_ERROR = 5;
    private static final int ERR_SCAN_IN_PROGRESS = 6;
    private static final int ERR_SCAN_FAILED = 7;
    private static final int ERR_READER_ALREADY_CONNECTED = 8;
    private static final int ERR_READER_CONNECTION_IN_PROGRESS = 9;
    private static final int ERR_READER_CONNECTION_CANCELLED = 10;
    private static final int ERR_READER_TYPE_NOT_SUPPORTED = 11;


    /* Card status codes */
    private static final int CARD_UNKNOWN = 0;
    private static final int CARD_ABSENT = 1;
    private static final int CARD_PRESENT = 2;
    private static final int CARD_POWER_SAVING_MODE = 3;
    private static final int CARD_POWERED = 4;

    /* Connection state codes */
    private static final int CON_UNKNOWN = 0;
    private static final int CON_DISCONNECTED = 1;
    private static final int CON_CONNECTED = 2;
    private static final int CON_CONNECTING = 3;
    private static final int CON_DISCONNECTING = 4;


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
    private Handler mHandler = new Handler();
    private static final long SCAN_PERIOD = 20000;
    private ArrayList<BTScanResult> foundDevices;

    // Callback contexts
    private CallbackContext startScanCallbackContext;
    private CallbackContext connectReaderCallbackContext;
    private CallbackContext cardStatusCallbackContext;
    private CallbackContext adpuResponseCallbackContext;
    private CallbackContext escapeResponseCallbackContext;
    private CallbackContext connectionStateCallbackContext;
    private CallbackContext requestTurnOnBtCallbackContext;
    private CallbackContext requestBtPermissionsCallbackContext;

    private int currentState = BluetoothReader.STATE_DISCONNECTED;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        // Initialize bluetooth manager and adapter and enable bluetooth if it's disabled.
        pluginActivity = cordova.getActivity();
        mBluetoothManager = (BluetoothManager) pluginActivity.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        // Get the permissions for scanning BT devices
        if (ContextCompat.checkSelfPermission(pluginActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION};
            ActivityCompat.requestPermissions(pluginActivity, permissions, REQUEST_PERMISSION_ACCESS_COARSE_LOCATION);
        }

        initializeBluetoothReaderManagerListeners();
        pluginActivity.registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        pluginActivity.unregisterReceiver(mReceiver);
    }


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        updateConnectionState(BluetoothReader.STATE_DISCONNECTED);
                        break;

                    case BluetoothAdapter.STATE_TURNING_ON:
                        break;

                    case BluetoothAdapter.STATE_ON:
                        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
                        break;

                    case BluetoothAdapter.STATE_TURNING_OFF:
                        releaseResources();
                        break;
                }
            }
        }
    };


    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) {
        if (action.equalsIgnoreCase(CONNECT_READER)) {
            cordova.getThreadPool().execute(() -> connectReader(callbackContext, data));
        } else if (action.equalsIgnoreCase(DISCONNECT_READER)) {
            cordova.getThreadPool().execute(() -> disconnectReader(callbackContext));
        } else if (action.equalsIgnoreCase(START_SCAN)) {
            cordova.getThreadPool().execute(() -> startScan(callbackContext));
        } else if (action.equalsIgnoreCase(STOP_SCAN)) {
            cordova.getThreadPool().execute(() -> stopScan(callbackContext));
        } else if (action.equalsIgnoreCase(LISTEN_FOR_CONNECTION_STATE)) {
            connectionStateCallbackContext = callbackContext;
        } else if (action.equalsIgnoreCase(LISTEN_FOR_ADPU_RESPONSE)) {
            adpuResponseCallbackContext = callbackContext;
        } else if (action.equalsIgnoreCase(LISTEN_FOR_ESCAPE_RESPONSE)) {
            escapeResponseCallbackContext = callbackContext;
        } else if (action.equalsIgnoreCase(LISTEN_FOR_CARD_STATUS)) {
            cardStatusCallbackContext = callbackContext;
        } else if (action.equalsIgnoreCase(AUTHENTICATE)) {
            cordova.getThreadPool().execute(() -> authenticate(callbackContext));
        } else if (action.equalsIgnoreCase(ENABLE_NOTIFICATIONS)) {
            cordova.getThreadPool().execute(() -> enableNotifications(callbackContext));
        } else if (action.equalsIgnoreCase(TRANSMIT_ADPU_COMMAND)) {
            cordova.getThreadPool().execute(() -> transmitAdpuCommand(callbackContext, data));
        } else if (action.equalsIgnoreCase(TRANSMIT_ESCAPE_COMMAND)) {
            cordova.getThreadPool().execute(() -> transmitEscapeCommand(callbackContext, data));
        } else if (action.equalsIgnoreCase(REQUEST_TURN_ON_BT)) {
            cordova.setActivityResultCallback(this);
            cordova.getThreadPool().execute(() -> requestTurnOnBt(callbackContext));
        } else if (action.equalsIgnoreCase(REQUEST_BT_PERMISSIONS)) {
            cordova.getThreadPool().execute(() -> requestBtPermissions(callbackContext));
        } else {
            return false;
        }
        return true;
    }

    private void requestTurnOnBt(CallbackContext callbackContext) {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            requestTurnOnBtCallbackContext = callbackContext;
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            pluginActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else if (callbackContext != null && !callbackContext.isFinished()) {
            callbackContext.success();
        }
    }


    private void requestBtPermissions(CallbackContext callbackContext){
        if (!cordova.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            requestBtPermissionsCallbackContext = callbackContext;
            String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION};
            cordova.requestPermissions(this, REQUEST_PERMISSION_ACCESS_COARSE_LOCATION, permissions);
        } else if (callbackContext != null && !callbackContext.isFinished()) {
            callbackContext.success();
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT && requestTurnOnBtCallbackContext != null && !requestTurnOnBtCallbackContext.isFinished()) {
            if (resultCode == pluginActivity.RESULT_OK) {
                requestTurnOnBtCallbackContext.success();
            } else {
                requestTurnOnBtCallbackContext.error(getAcsErrorCodeJSON(ERR_UNKNOWN, "Failed to turn on BT"));
            }
        }
    }


    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_ACCESS_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (requestBtPermissionsCallbackContext != null && !requestBtPermissionsCallbackContext.isFinished()) {
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        requestBtPermissionsCallbackContext.success();
                    } else {
                        requestBtPermissionsCallbackContext.error("Bluetooth permissions not granted");
                    }
                }
                return;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_ACCESS_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (requestBtPermissionsCallbackContext != null && !requestBtPermissionsCallbackContext.isFinished()) {
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        requestBtPermissionsCallbackContext.success();
                    } else {
                        requestBtPermissionsCallbackContext.error("Bluetooth permissions not granted");
                    }
                }
                return;
            }
        }
    }


    private void authenticate(CallbackContext callbackContext) {
        if (mBluetoothReader == null) {
            callbackContext.error(getAcsErrorCodeJSON(ERR_READER_NOT_INITIALIZED, null));
            return;
        }

        mBluetoothReader.setOnAuthenticationCompleteListener((BluetoothReader bluetoothReader, int errorCode) -> {
            if (errorCode == BluetoothReader.ERROR_SUCCESS) {
                callbackContext.success();
            } else {
                callbackContext.error(getAcsErrorCodeJSON(ERR_UNKNOWN, "Bluetooth reader error - " + Integer.toString(errorCode)));
            }
        });

        final byte[] DEFAULT_1255_MASTER_KEY = new byte[]{65, 67, 82, 49, 50, 53, 53, 85, 45, 74, 49, 32, 65, 117, 116, 104};
        boolean result = mBluetoothReader.authenticate(DEFAULT_1255_MASTER_KEY);
        checkIfTimedOut(callbackContext, "Authentication operation timed out", 2000);
        if (!result) {
            callbackContext.error(getAcsErrorCodeJSON(ERR_OPERATION_FAILED, "Authentication operation was unsuccessful"));
        }
    }


    private void enableNotifications(CallbackContext callbackContext) {
        if (mBluetoothReader == null) {
            callbackContext.error(getAcsErrorCodeJSON(ERR_READER_NOT_INITIALIZED, null));
            return;
        }

        mBluetoothReader.setOnEnableNotificationCompleteListener((BluetoothReader bluetoothReader, int errorCode) -> {
            if (errorCode == BluetoothReader.ERROR_SUCCESS) {
                callbackContext.success();
            } else {
                callbackContext.error(getAcsErrorCodeJSON(ERR_UNKNOWN, "Bluetooth reader error - " + Integer.toString(errorCode)));
            }
        });

        boolean result = mBluetoothReader.enableNotification(true);
        checkIfTimedOut(callbackContext, "Enable notifications operation timed out", 2000);
        if (!result) {
            callbackContext.error(getAcsErrorCodeJSON(ERR_OPERATION_FAILED, "Enable Notifications operation was unsuccessful"));
        }
    }


    private void transmitAdpuCommand(CallbackContext callbackContext, JSONArray data) {
        if (mBluetoothReader == null) {
            callbackContext.error(getAcsErrorCodeJSON(ERR_READER_NOT_INITIALIZED, null));
            return;
        }

        try {
            boolean result = mBluetoothReader.transmitApdu(toByteArray(data.getString(0)));
            if (!result) {
                callbackContext.error(getAcsErrorCodeJSON(ERR_OPERATION_FAILED, "Transmit ADPU command was unsuccessful"));
            } else {
                callbackContext.success();
            }
        } catch (Exception e) {
            callbackContext.error(getAcsErrorCodeJSON(ERR_UNKNOWN, "Transmit ADPU Command - " + e.getMessage()));
        }
    }

    private void transmitEscapeCommand(CallbackContext callbackContext, JSONArray data) {
        if (mBluetoothReader == null) {
            callbackContext.error(getAcsErrorCodeJSON(ERR_READER_NOT_INITIALIZED, null));
            return;
        }

        try {
            boolean result = mBluetoothReader.transmitEscapeCommand(toByteArray(data.getString(0)));
            if (!result) {
                callbackContext.error(getAcsErrorCodeJSON(ERR_OPERATION_FAILED, "Transmit Escape command was unsuccessful"));
            } else {
                callbackContext.success();
            }
        } catch (Exception e) {
            callbackContext.error(getAcsErrorCodeJSON(ERR_UNKNOWN, "Transmit escape command - " + e.getMessage()));
        }
    }


    public void startScan(CallbackContext callbackContext) {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            callbackContext.error(getAcsErrorCodeJSON(ERR_BT_IS_OFF, null));
            return;
        }

        if (mBluetoothManager == null || mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            callbackContext.error(getAcsErrorCodeJSON(ERR_BT_ERROR, null));
            return;
        }

        if (startScanCallbackContext != null) {
            callbackContext.error(getAcsErrorCodeJSON(ERR_SCAN_IN_PROGRESS, null));
            return;
        }

        foundDevices = new ArrayList<>();
        startScanCallbackContext = callbackContext;
        pluginActivity.startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);

        mHandler.postDelayed(() -> {
            if (mScanning) {
                mScanning = false;
                if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
                    mBluetoothLeScanner.stopScan(mScanCallback);
                }
            }
            startScanCallbackContext.success();
            startScanCallbackContext = null;
        }, SCAN_PERIOD);

        mScanning = true;
        mBluetoothLeScanner.startScan(mScanCallback);
    }


    public void stopScan(CallbackContext callbackContext) {
        if (startScanCallbackContext == null || !mScanning) {
            if (callbackContext != null) {
                callbackContext.success();
            }
            return;
        }

        if (mBluetoothManager == null || mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            if (callbackContext != null) {
                callbackContext.error(getAcsErrorCodeJSON(ERR_UNKNOWN, null));
            }
            return;
        }

        mHandler.removeCallbacksAndMessages(null);
        mScanning = false;
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            mBluetoothLeScanner.stopScan(mScanCallback);
        }
        startScanCallbackContext.success();
        startScanCallbackContext = null;
        if (callbackContext != null) {
            callbackContext.success();
        }
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
                    PluginResult pluginRes = new PluginResult(PluginResult.Status.ERROR, getAcsErrorCodeJSON(ERR_UNKNOWN, e.getMessage()));
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
                        PluginResult pluginRes = new PluginResult(PluginResult.Status.ERROR, getAcsErrorCodeJSON(ERR_UNKNOWN, e.getMessage()));
                        pluginRes.setKeepCallback(true);
                        startScanCallbackContext.sendPluginResult(pluginRes);
                    }
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            startScanCallbackContext.error(getAcsErrorCodeJSON(ERR_UNKNOWN, "Scan failed - " + errorCode));
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
            if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                callbackContext.error(getAcsErrorCodeJSON(ERR_BT_IS_OFF, null));
                return;
            }


            // Check for already existing connections
            if (currentState != BluetoothReader.STATE_DISCONNECTED) {
                callbackContext.error(getAcsErrorCodeJSON(ERR_READER_ALREADY_CONNECTED, null));
                return;
            }

            if (connectReaderCallbackContext != null && !connectReaderCallbackContext.isFinished()) {
                callbackContext.error(getAcsErrorCodeJSON(ERR_READER_CONNECTION_IN_PROGRESS, null));
                return;
            }

            // Get the device address
            String myDeviceAddress = data.getString(0);
            connectReaderCallbackContext = callbackContext;


            // Create a new GATT Callback and initialize listeners
            mGattCallback = new BluetoothReaderGattCallback();
            initializeGattCallbackListeners();


            // Get the device by it's address and connect to it with the callBack
            final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(myDeviceAddress);
            checkIfTimedOut(callbackContext, "Connecting the reader timed out", 15000);
            mBluetoothGatt = device.connectGatt(cordova.getContext(), false, mGattCallback);
        } catch (Exception e) {
            connectReaderCallbackContext.error(getAcsErrorCodeJSON(ERR_UNKNOWN, "Connect reader - " + e.getMessage()));
            releaseResources();
            updateConnectionState(BluetoothReader.STATE_DISCONNECTED);
        }
    }

    private void disconnectReader(final CallbackContext callbackContext) {
        try {
            releaseResources();
            this.updateConnectionState(BluetoothReader.STATE_DISCONNECTED);
            if (connectReaderCallbackContext != null && !connectReaderCallbackContext.isFinished()) {
                callbackContext.error(getAcsErrorCodeJSON(ERR_READER_CONNECTION_CANCELLED, null));
                return;
            }
            callbackContext.success();
        } catch (Exception e) {
            callbackContext.error(getAcsErrorCodeJSON(ERR_UNKNOWN, "Disconnect reader - " + e.getMessage()));
        }
    }


    private void initializeGattCallbackListeners() {
        mGattCallback.setOnConnectionStateChangeListener((final BluetoothGatt gatt, final int state, final int newState) -> {
            this.updateConnectionState(state);

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (mBluetoothReaderManager != null) {
                    mBluetoothReaderManager.detectReader(gatt, mGattCallback);
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                releaseResources();
            }
        });
    }

    private void initializeBluetoothReaderManagerListeners() {
        mBluetoothReaderManager.setOnReaderDetectionListener((BluetoothReader bluetoothReader) -> {
            if (!(bluetoothReader instanceof Acr1255uj1Reader)) {
                connectReaderCallbackContext.error(getAcsErrorCodeJSON(ERR_READER_TYPE_NOT_SUPPORTED, null));
                this.updateConnectionState(BluetoothReader.STATE_DISCONNECTED);
                releaseResources();
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

        mBluetoothReader.setOnResponseApduAvailableListener((BluetoothReader bluetoothReader, byte[] response, int errorCode) -> {
            if (adpuResponseCallbackContext != null) {
                if (bytesToHex(response) == "6300") {
                    PluginResult pluginRes = new PluginResult(PluginResult.Status.ERROR, getAcsErrorCodeJSON(ERR_UNKNOWN, "ADPU response - the operation failed"));
                    pluginRes.setKeepCallback(true);
                    adpuResponseCallbackContext.sendPluginResult(pluginRes);
                } else if (bytesToHex(response) == "6A81") {
                    PluginResult pluginRes = new PluginResult(PluginResult.Status.ERROR, getAcsErrorCodeJSON(ERR_UNKNOWN, "ADPU response - the operation is not supported"));
                    pluginRes.setKeepCallback(true);
                    adpuResponseCallbackContext.sendPluginResult(pluginRes);
                } else if (response != null) {
                    PluginResult pluginRes = new PluginResult(PluginResult.Status.OK, bytesToHex(response));
                    pluginRes.setKeepCallback(true);
                    adpuResponseCallbackContext.sendPluginResult(pluginRes);
                } else {
                    PluginResult pluginRes = new PluginResult(PluginResult.Status.ERROR, getAcsErrorCodeJSON(ERR_UNKNOWN, "ADPU response error - " + errorCode));
                    pluginRes.setKeepCallback(true);
                    adpuResponseCallbackContext.sendPluginResult(pluginRes);
                }
            }
        });

        mBluetoothReader.setOnEscapeResponseAvailableListener((BluetoothReader bluetoothReader, byte[] response, int errorCode) -> {
            if (escapeResponseCallbackContext != null) {
                if (response != null) {
                    PluginResult pluginRes = new PluginResult(PluginResult.Status.OK, bytesToHex(response));
                    pluginRes.setKeepCallback(true);
                    escapeResponseCallbackContext.sendPluginResult(pluginRes);
                } else {
                    PluginResult pluginRes = new PluginResult(PluginResult.Status.ERROR, getAcsErrorCodeJSON(ERR_UNKNOWN, "Escape response error - " + errorCode));
                    pluginRes.setKeepCallback(true);
                    escapeResponseCallbackContext.sendPluginResult(pluginRes);
                }
            }
        });
    }

    private void updateConnectionState(int newState) {
        if (connectionStateCallbackContext != null && currentState != newState) {
            currentState = newState;
            PluginResult pluginRes = new PluginResult(PluginResult.Status.OK, getConnectionStateJSON(newState));
            pluginRes.setKeepCallback(true);
            connectionStateCallbackContext.sendPluginResult(pluginRes);
        }
    }

    private void releaseResources() {
        stopScan(null);
        if (mBluetoothReader != null) {
            mBluetoothReader.setOnDeviceInfoAvailableListener(null);
            mBluetoothReader.setOnCardStatusChangeListener(null);
            mBluetoothReader.setOnEnableNotificationCompleteListener(null);
            mBluetoothReader.setOnCardStatusAvailableListener(null);
            mBluetoothReader.setOnEscapeResponseAvailableListener(null);
            mBluetoothReader.setOnAuthenticationCompleteListener(null);
            mBluetoothReader.setOnResponseApduAvailableListener(null);
            mBluetoothReader.setOnAtrAvailableListener(null);
            mBluetoothReader.setOnCardPowerOffCompleteListener(null);
            mBluetoothReader = null;
        }
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
            mGattCallback.setOnConnectionStateChangeListener(null);
            mGattCallback = null;
        }
    }


    private void checkIfTimedOut(CallbackContext callbackContext, String msg, int delay) {
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            if (!callbackContext.isFinished()) {
                callbackContext.error(getAcsErrorCodeJSON(ERR_OPERATION_TIMED_OUT, msg));
            }
            handler.removeCallbacksAndMessages(null);
        }, delay);
    }


    private String getAcsErrorCodeJSON(int errorCode, String customMessage) {
        Gson gson = new Gson();

        if (errorCode == ERR_UNKNOWN && customMessage == null) {
            customMessage = "Unknown error";
        } else if (errorCode == ERR_READER_NOT_INITIALIZED && customMessage == null) {
            customMessage = "Reader not initialized";
        } else if (errorCode == ERR_OPERATION_FAILED && customMessage == null) {
            customMessage = "Operation failed";
        } else if (errorCode == ERR_OPERATION_TIMED_OUT && customMessage == null) {
            customMessage = "Operation timed out";
        } else if (errorCode == ERR_BT_IS_OFF && customMessage == null) {
            customMessage = "Bluetooth is off";
        } else if (errorCode == ERR_BT_ERROR && customMessage == null) {
            customMessage = "Unknown bluetooth error";
        } else if (errorCode == ERR_SCAN_IN_PROGRESS && customMessage == null) {
            customMessage = "Scan is already in progress";
        } else if (errorCode == ERR_SCAN_FAILED && customMessage == null) {
            customMessage = "Scan failed";
        } else if (errorCode == ERR_READER_ALREADY_CONNECTED && customMessage == null) {
            customMessage = "Reader already connected. Disconnect before connecting a new reader.";
        } else if (errorCode == ERR_READER_CONNECTION_IN_PROGRESS && customMessage == null) {
            customMessage = "Reader connection already in progress";
        } else if (errorCode == ERR_READER_CONNECTION_CANCELLED && customMessage == null) {
            customMessage = "Reader connection cancelled";
        } else if (errorCode == ERR_READER_TYPE_NOT_SUPPORTED && customMessage == null) {
            customMessage = "Reader type is not supported. Currently only Acr1255uj1Reader is supported.";
        } else if (customMessage == null) {
            customMessage = "No error message specified. You're in trouble now boy.";
        }

        return gson.toJson(new StatusMessage(errorCode, customMessage));
    }

    private String getCardStatusJSON(int cardStatus) {
        Gson gson = new Gson();
        if (cardStatus == BluetoothReader.CARD_STATUS_ABSENT) {
            return gson.toJson(new StatusMessage(CARD_ABSENT, "Card is absent"));
        } else if (cardStatus == BluetoothReader.CARD_STATUS_PRESENT) {
            return gson.toJson(new StatusMessage(CARD_PRESENT, "Card is present."));
        } else if (cardStatus == BluetoothReader.CARD_STATUS_POWER_SAVING_MODE) {
            return gson.toJson(new StatusMessage(CARD_POWER_SAVING_MODE, "Reader is in power saving mode."));
        } else if (cardStatus == BluetoothReader.CARD_STATUS_POWERED) {
            return gson.toJson(new StatusMessage(CARD_POWERED, "Card is powered."));
        }
        return gson.toJson(new StatusMessage(CARD_UNKNOWN, "Status unknown."));
    }

    private String getConnectionStateJSON(int status) {
        Gson gson = new Gson();
        if (status == BluetoothReader.STATE_DISCONNECTED) {
            return gson.toJson(new StatusMessage(CON_DISCONNECTED, "Disconnected"));
        } else if (status == BluetoothReader.STATE_CONNECTED) {
            return gson.toJson(new StatusMessage(CON_CONNECTED, "Connected."));
        } else if (status == BluetoothReader.STATE_CONNECTING) {
            return gson.toJson(new StatusMessage(CON_CONNECTING, "Connecting"));
        } else if (status == BluetoothReader.STATE_DISCONNECTING) {
            return gson.toJson(new StatusMessage(CON_DISCONNECTING, "Disconnecting"));
        }
        return gson.toJson(new StatusMessage(CON_UNKNOWN, "Status unknown."));
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

    private static byte[] toByteArray(String hexString) {
        int hexStringLength = hexString.length();
        byte[] byteArray = null;
        int count = 0;
        char c;
        int i;

        // Count number of hex characters
        for (i = 0; i < hexStringLength; i++) {

            c = hexString.charAt(i);
            if (c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a'
                    && c <= 'f') {
                count++;
            }
        }

        byteArray = new byte[(count + 1) / 2];
        boolean first = true;
        int len = 0;
        int value;
        for (i = 0; i < hexStringLength; i++) {

            c = hexString.charAt(i);
            if (c >= '0' && c <= '9') {
                value = c - '0';
            } else if (c >= 'A' && c <= 'F') {
                value = c - 'A' + 10;
            } else if (c >= 'a' && c <= 'f') {
                value = c - 'a' + 10;
            } else {
                value = -1;
            }

            if (value >= 0) {
                if (first) {
                    byteArray[len] = (byte) (value << 4);
                } else {
                    byteArray[len] |= value;
                    len++;
                }
                first = !first;
            }
        }

        return byteArray;
    }
}

class BTScanResult {
    public BTScanResultDevice mDevice;
}

class BTScanResultDevice {
    public String mAddress;
}

class StatusMessage {
    int code;
    String message;

    StatusMessage(int code, String message) {
        this.code = code;
        this.message = message;
    }
}

