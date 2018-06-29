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
import android.nfc.NfcAdapter;
import android.os.Handler;
import android.os.Looper;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;

import com.acs.bluetooth.*;
import com.google.gson.Gson;

public class Acs extends CordovaPlugin {
  private static final String CONNECT_GATT = "connectGatt";
  private static final String DISCONNECT_GATT = "disconnectGatt";
  private static final String DETECT_READER = "detectReader";
  private static final String ENABLE_NOTIFICATIONS = "enableNotifications";
  private static final String AUTHENTICATE = "authenticate";
  private static final String LISTEN_FOR_ADPU_RESPONSE = "listenForAdpuResponse";
  private static final String LISTEN_FOR_ESCAPE_RESPONSE = "listenForEscapeResponse";
  private static final String LISTEN_FOR_CARD_STATUS = "listenForCardStatus";
  private static final String LISTEN_FOR_GATT_CONNECTION_STATE = "listenForGattConnectionState";
  private static final String LISTEN_FOR_NFC_CONNECTION_STATE = "listenForNfcConnectionState";
  private static final String LISTEN_FOR_BT_CONNECTION_STATE = "listenForBtConnectionState";
  private static final String START_SCAN = "startScan";
  private static final String STOP_SCAN = "stopScan";
  private static final String TRANSMIT_ADPU_COMMAND = "transmitAdpuCommand";
  private static final String TRANSMIT_ESCAPE_COMMAND = "transmitEscapeCommand";
  private static final String REQUEST_BT = "requestBt";
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
  private static final int ERR_OPERATION_ALREADY_IN_PROGRESS = 6;
  private static final int ERR_SCAN_FAILED = 7;
  private static final int ERR_GATT_ALREADY_CONNECTED = 8;
  private static final int ERR_GATT_CONNECTION_IN_PROGRESS = 9;
  private static final int ERR_GATT_CONNECTION_CANCELLED = 10;
  private static final int ERR_READER_TYPE_NOT_SUPPORTED = 11;

  /* Card status codes */
  private static final int CARD_ABSENT = 1;
  private static final int CARD_PRESENT = 2;
  private static final int CARD_POWER_SAVING_MODE = 3;
  private static final int CARD_POWERED = 4;

  /* Connection state codes */
  private static final int CON_DISCONNECTED = 1;
  private static final int CON_CONNECTED = 2;
  private static final int CON_DISCONNECTING = 3;
  private static final int CON_CONNECTING = 4;


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
  private Handler mScanHandler = new Handler();
  private ArrayList<BTScanResult> foundDevices;
  private static final long SCAN_PERIOD = 20000;

  // Callback contexts
  private CallbackContext ccAdpuResponse;
  private CallbackContext ccBtConnectionState;
  private CallbackContext ccCardStatus;
  private CallbackContext ccConnectGatt;
  private CallbackContext ccGattConnectionState;
  private CallbackContext ccDetectReader;
  private CallbackContext ccEscapeResponse;
  private CallbackContext ccNfcConnectionState;
  private CallbackContext ccRequestBt;
  private CallbackContext ccRequestBtPermissions;
  private CallbackContext ccStartScan;

  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    // Initialize bluetooth manager and adapter and enable bluetooth if it's disabled.
    pluginActivity = cordova.getActivity();
    mBluetoothManager = (BluetoothManager) pluginActivity.getSystemService(Context.BLUETOOTH_SERVICE);
    mBluetoothAdapter = mBluetoothManager.getAdapter();
    mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

    initializeBluetoothReaderManagerListeners();
    pluginActivity.registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    pluginActivity.registerReceiver(mReceiver, new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED));
  }


  @Override
  public void onDestroy() {
    pluginActivity.unregisterReceiver(mReceiver);
    releaseResources();
    super.onDestroy();
  }


  private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();

      if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
        final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
        switch (state) {
          case BluetoothAdapter.STATE_OFF:
            PluginResult off = new PluginResult(PluginResult.Status.OK, CON_DISCONNECTED);
            off.setKeepCallback(true);
            ccBtConnectionState.sendPluginResult(off);
            break;
          case BluetoothAdapter.STATE_ON:
            PluginResult on = new PluginResult(PluginResult.Status.OK, CON_CONNECTED);
            on.setKeepCallback(true);
            ccBtConnectionState.sendPluginResult(on);

            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            break;
          case BluetoothAdapter.STATE_TURNING_OFF:
            PluginResult turningOff = new PluginResult(PluginResult.Status.OK, CON_DISCONNECTING);
            turningOff.setKeepCallback(true);
            ccBtConnectionState.sendPluginResult(turningOff);

            releaseResources();
            break;
          case BluetoothAdapter.STATE_TURNING_ON:
            PluginResult turningOn = new PluginResult(PluginResult.Status.OK, CON_CONNECTING);
            turningOn.setKeepCallback(true);
            ccBtConnectionState.sendPluginResult(turningOn);
            break;
        }
      } else if (NfcAdapter.ACTION_ADAPTER_STATE_CHANGED.equals(action)) {
        final int state = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_OFF);
        switch (state) {
          case NfcAdapter.STATE_OFF:
            PluginResult off = new PluginResult(PluginResult.Status.OK, CON_DISCONNECTED);
            off.setKeepCallback(true);
            ccNfcConnectionState.sendPluginResult(off);
            break;
          case NfcAdapter.STATE_ON:
            PluginResult on = new PluginResult(PluginResult.Status.OK, CON_CONNECTED);
            on.setKeepCallback(true);
            ccNfcConnectionState.sendPluginResult(on);
            break;
          case NfcAdapter.STATE_TURNING_OFF:
            PluginResult turningOff = new PluginResult(PluginResult.Status.OK, CON_DISCONNECTING);
            turningOff.setKeepCallback(true);
            ccNfcConnectionState.sendPluginResult(turningOff);
            break;
          case NfcAdapter.STATE_TURNING_ON:
            PluginResult turningOn = new PluginResult(PluginResult.Status.OK, CON_CONNECTING);
            turningOn.setKeepCallback(true);
            ccNfcConnectionState.sendPluginResult(turningOn);
            break;
        }
      }
    }
  };


  @Override
  public boolean execute(String action, JSONArray data, CallbackContext callbackContext) {
    if (action.equalsIgnoreCase(CONNECT_GATT)) {
      cordova.getThreadPool().execute(() -> connectGatt(callbackContext, data));
    } else if (action.equalsIgnoreCase(DISCONNECT_GATT)) {
      cordova.getThreadPool().execute(() -> disconnectGatt(callbackContext));
    } else if (action.equalsIgnoreCase(DETECT_READER)) {
      cordova.getThreadPool().execute(() -> detectReader(callbackContext));
    } else if (action.equalsIgnoreCase(START_SCAN)) {
      cordova.getThreadPool().execute(() -> startScan(callbackContext));
    } else if (action.equalsIgnoreCase(STOP_SCAN)) {
      cordova.getThreadPool().execute(() -> stopScan(callbackContext));
    } else if (action.equalsIgnoreCase(LISTEN_FOR_GATT_CONNECTION_STATE)) {
      ccGattConnectionState = callbackContext;
    } else if (action.equalsIgnoreCase(LISTEN_FOR_ADPU_RESPONSE)) {
      ccAdpuResponse = callbackContext;
    } else if (action.equalsIgnoreCase(LISTEN_FOR_ESCAPE_RESPONSE)) {
      ccEscapeResponse = callbackContext;
    } else if (action.equalsIgnoreCase(LISTEN_FOR_CARD_STATUS)) {
      ccCardStatus = callbackContext;
    } else if (action.equalsIgnoreCase(LISTEN_FOR_NFC_CONNECTION_STATE)) {
      ccNfcConnectionState = callbackContext;
    } else if (action.equalsIgnoreCase(LISTEN_FOR_BT_CONNECTION_STATE)) {
      ccBtConnectionState = callbackContext;
    } else if (action.equalsIgnoreCase(AUTHENTICATE)) {
      cordova.getThreadPool().execute(() -> authenticate(callbackContext));
    } else if (action.equalsIgnoreCase(ENABLE_NOTIFICATIONS)) {
      cordova.getThreadPool().execute(() -> enableNotifications(callbackContext));
    } else if (action.equalsIgnoreCase(TRANSMIT_ADPU_COMMAND)) {
      cordova.getThreadPool().execute(() -> transmitAdpuCommand(callbackContext, data));
    } else if (action.equalsIgnoreCase(TRANSMIT_ESCAPE_COMMAND)) {
      cordova.getThreadPool().execute(() -> transmitEscapeCommand(callbackContext, data));
    } else if (action.equalsIgnoreCase(REQUEST_BT)) {
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
    if (callbackContext == null) {
      return;
    }

    if (mBluetoothAdapter == null) {
      callbackContext.error(getAcsErrorCodeJSON(ERR_BT_ERROR, "Critical error. Bluetooth adapter is not initialized"));
      return;
    }

    if (ccRequestBt != null) {
      callbackContext.error(ERR_OPERATION_ALREADY_IN_PROGRESS);
      return;
    }

    if (mBluetoothAdapter.isEnabled()) {
      callbackContext.success();
      return;
    }

    ccRequestBt = callbackContext;
    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    pluginActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
  }


  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == REQUEST_ENABLE_BT && ccRequestBt != null) {
      if (resultCode == pluginActivity.RESULT_OK) {
        ccRequestBt.success();
      } else {
        ccRequestBt.error(getAcsErrorCodeJSON(ERR_UNKNOWN, "Failed to turn on BT"));
      }
      ccRequestBt = null;
    }
  }


  private void requestBtPermissions(CallbackContext callbackContext) {
    if (callbackContext == null) {
      return;
    }

    if (cordova.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
      callbackContext.success();
      return;
    }

    if (ccRequestBtPermissions != null) {
      callbackContext.error(ERR_OPERATION_ALREADY_IN_PROGRESS);
      return;
    }

    ccRequestBtPermissions = callbackContext;
    String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION};
    cordova.requestPermissions(this, REQUEST_PERMISSION_ACCESS_COARSE_LOCATION, permissions);
  }


  @Override
  public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
    switch (requestCode) {
      case REQUEST_PERMISSION_ACCESS_COARSE_LOCATION: {
        // If request is cancelled, the result arrays are empty.
        if (ccRequestBtPermissions != null) {
          if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            ccRequestBtPermissions.success();
          } else {
            ccRequestBtPermissions.error(getAcsErrorCodeJSON(ERR_UNKNOWN, "Bluetooth permissions not granted"));
          }
          ccRequestBtPermissions = null;
        }
        break;
      }
    }
  }

  private void authenticate(CallbackContext callbackContext) {
    if (callbackContext == null) {
      return;
    }

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
    if (!result) {
      callbackContext.error(getAcsErrorCodeJSON(ERR_OPERATION_FAILED, "Authentication operation was unsuccessful"));
      return;
    }
    checkIfTimedOut(callbackContext, "Authentication operation timed out", 2000);
  }


  private void enableNotifications(CallbackContext callbackContext) {
    if (callbackContext == null) {
      return;
    }

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
    if (!result) {
      callbackContext.error(getAcsErrorCodeJSON(ERR_OPERATION_FAILED, "Enable Notifications operation was unsuccessful"));
      return;
    }
    checkIfTimedOut(callbackContext, "Enable notifications operation timed out", 2000);
  }


  private void transmitAdpuCommand(CallbackContext callbackContext, JSONArray data) {
    if (callbackContext == null) {
      return;
    }

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
    if (callbackContext == null) {
      return;
    }

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
    if (callbackContext == null) {
      return;
    }

    if (mBluetoothManager == null || mBluetoothAdapter == null || mBluetoothAdapter == null || mBluetoothLeScanner == null) {
      callbackContext.error(getAcsErrorCodeJSON(ERR_BT_ERROR, null));
      return;
    }

    if (!mBluetoothAdapter.isEnabled()) {
      callbackContext.error(getAcsErrorCodeJSON(ERR_BT_IS_OFF, null));
      return;
    }

    if (ccStartScan != null) {
      callbackContext.error(getAcsErrorCodeJSON(ERR_OPERATION_ALREADY_IN_PROGRESS, null));
      return;
    }

    foundDevices = new ArrayList<>();
    ccStartScan = callbackContext;
    pluginActivity.startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);

    mScanHandler.postDelayed(() -> {
      if (mScanning) {
        mScanning = false;
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() && mBluetoothLeScanner != null) {
          mBluetoothLeScanner.stopScan(mScanCallback);
        }
      }
      if (ccStartScan != null) {
        ccStartScan.success();
        ccStartScan = null;
      }
    }, SCAN_PERIOD);

    mScanning = true;
    mBluetoothLeScanner.startScan(mScanCallback);
  }


  public void stopScan(CallbackContext callbackContext) {
    if (callbackContext == null) {
      return;
    }

    if (ccStartScan == null || !mScanning) {
      callbackContext.success();
      return;
    }

    if (mBluetoothManager == null || mBluetoothAdapter == null) {
      callbackContext.error(getAcsErrorCodeJSON(ERR_BT_ERROR, null));
      return;
    }

    if (!mBluetoothAdapter.isEnabled()) {
      callbackContext.error(getAcsErrorCodeJSON(ERR_BT_IS_OFF, null));
      return;
    }

    mScanHandler.removeCallbacksAndMessages(null);
    mScanning = false;
    mBluetoothLeScanner.stopScan(mScanCallback);
    ccStartScan.success();
    ccStartScan = null;
    callbackContext.success();
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
          ccStartScan.sendPluginResult(pluginRes);
        } catch (Throwable e) {
          PluginResult pluginRes = new PluginResult(PluginResult.Status.ERROR, getAcsErrorCodeJSON(ERR_UNKNOWN, e.getMessage()));
          pluginRes.setKeepCallback(true);
          ccStartScan.sendPluginResult(pluginRes);
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
            ccStartScan.sendPluginResult(pluginRes);
          } catch (Throwable e) {
            PluginResult pluginRes = new PluginResult(PluginResult.Status.ERROR, getAcsErrorCodeJSON(ERR_UNKNOWN, e.getMessage()));
            pluginRes.setKeepCallback(true);
            ccStartScan.sendPluginResult(pluginRes);
          }
        }
      }
    }

    @Override
    public void onScanFailed(int errorCode) {
      ccStartScan.error(getAcsErrorCodeJSON(ERR_UNKNOWN, "Scan failed - " + errorCode));
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


  private void connectGatt(final CallbackContext callbackContext, JSONArray data) {
    if (callbackContext == null) {
      return;
    }

    try {
      // Check if BT is enabled
      if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
        callbackContext.error(getAcsErrorCodeJSON(ERR_BT_IS_OFF, null));
        return;
      }

      // Check for already existing connections
      if (!mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT).isEmpty()) {
        callbackContext.error(getAcsErrorCodeJSON(ERR_GATT_ALREADY_CONNECTED, null));
        return;
      }

      // Check for connections in progress
      if (ccConnectGatt != null) {
        callbackContext.error(getAcsErrorCodeJSON(ERR_GATT_CONNECTION_IN_PROGRESS, null));
        return;
      }

      // Get the device address
      String myDeviceAddress = data.getString(0);
      ccConnectGatt = callbackContext;


      // Create a new GATT Callback and initialize listeners
      mGattCallback = new BluetoothReaderGattCallback();
      initializeGattCallbackListeners();


      // Get the device by it's address and connect to it with the callBack
      final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(myDeviceAddress);
      checkIfTimedOut(callbackContext, "Connecting GATT timed out", 10000);
      mBluetoothGatt = device.connectGatt(cordova.getContext(), true, mGattCallback);
    } catch (Exception e) {
      ccConnectGatt.error(getAcsErrorCodeJSON(ERR_UNKNOWN, "Connect GATT - " + e.getMessage()));
      ccConnectGatt = null;
      releaseResources();
    }
  }

  private void disconnectGatt(final CallbackContext callbackContext) {
    if (callbackContext == null) {
      return;
    }

    releaseResources();
    if (ccConnectGatt != null) {
      ccConnectGatt.error(getAcsErrorCodeJSON(ERR_GATT_CONNECTION_CANCELLED, null));
      ccConnectGatt = null;
    }
    callbackContext.success();
  }


  private void initializeGattCallbackListeners() {
    mGattCallback.setOnConnectionStateChangeListener((final BluetoothGatt gatt, final int state, final int newState) -> {
      if (ccGattConnectionState != null) {
        switch (newState) {
          case BluetoothReader.STATE_DISCONNECTED:
            PluginResult off = new PluginResult(PluginResult.Status.OK, CON_DISCONNECTED);
            off.setKeepCallback(true);
            ccGattConnectionState.sendPluginResult(off);

            if (ccConnectGatt != null) {
              ccConnectGatt.error(getAcsErrorCodeJSON(ERR_OPERATION_FAILED, "Failed to connect to GATT"));
              ccConnectGatt = null;
            }
            break;
          case BluetoothReader.STATE_CONNECTED:
            PluginResult on = new PluginResult(PluginResult.Status.OK, CON_CONNECTED);
            on.setKeepCallback(true);
            ccGattConnectionState.sendPluginResult(on);

            if (ccConnectGatt != null) {
              ccConnectGatt.success();
              ccConnectGatt = null;
            }
            break;
          case BluetoothReader.STATE_DISCONNECTING:
            PluginResult turningOff = new PluginResult(PluginResult.Status.OK, CON_DISCONNECTING);
            turningOff.setKeepCallback(true);
            ccGattConnectionState.sendPluginResult(turningOff);
            break;
          case BluetoothReader.STATE_CONNECTING:
            PluginResult turningOn = new PluginResult(PluginResult.Status.OK, CON_CONNECTING);
            turningOn.setKeepCallback(true);
            ccGattConnectionState.sendPluginResult(turningOn);
            break;
        }
      }
    });
  }

  private void detectReader(CallbackContext callbackContext) {
    if (callbackContext == null) {
      return;
    }

    if (ccDetectReader != null) {
      callbackContext.error(getAcsErrorCodeJSON(ERR_OPERATION_ALREADY_IN_PROGRESS, null));
      return;
    }

    if (mBluetoothReaderManager != null) {
      ccDetectReader = callbackContext;
      boolean result = mBluetoothReaderManager.detectReader(mBluetoothGatt, mGattCallback);
      if (!result) {
        ccDetectReader.error(getAcsErrorCodeJSON(ERR_OPERATION_FAILED, "Detect reader operation was unsuccessful"));
        ccDetectReader = null;
        return;
      }
      checkIfTimedOut(ccDetectReader, "Detecting the reader timed out", 5000);
    } else {
      callbackContext.error(getAcsErrorCodeJSON(ERR_OPERATION_FAILED, "Bluetooth reader manager is not initialized"));
    }
  }

  private void initializeBluetoothReaderManagerListeners() {
    mBluetoothReaderManager.setOnReaderDetectionListener((BluetoothReader bluetoothReader) -> {
      if (!(bluetoothReader instanceof Acr1255uj1Reader)) {
        if (ccDetectReader != null) {
          ccDetectReader.error(getAcsErrorCodeJSON(ERR_READER_TYPE_NOT_SUPPORTED, null));
          ccDetectReader = null;
        }
        releaseResources();
        return;
      }

      mBluetoothReader = bluetoothReader;
      initializeBluetoothReaderListeners();
      if (ccDetectReader != null) {
        ccDetectReader.success();
        ccDetectReader = null;
      }
    });
  }

  private void initializeBluetoothReaderListeners() {
    mBluetoothReader.setOnCardStatusChangeListener((BluetoothReader bluetoothReader, int cardStatus) -> {
      if (ccCardStatus != null) {
        switch (cardStatus) {
          case BluetoothReader.CARD_STATUS_ABSENT:
            PluginResult absent = new PluginResult(PluginResult.Status.OK, CARD_ABSENT);
            absent.setKeepCallback(true);
            ccCardStatus.sendPluginResult(absent);
            break;
          case BluetoothReader.CARD_STATUS_PRESENT:
            PluginResult present = new PluginResult(PluginResult.Status.OK, CARD_PRESENT);
            present.setKeepCallback(true);
            ccCardStatus.sendPluginResult(present);
            break;
          case BluetoothReader.CARD_STATUS_POWER_SAVING_MODE:
            PluginResult powerSaving = new PluginResult(PluginResult.Status.OK, CARD_POWER_SAVING_MODE);
            powerSaving.setKeepCallback(true);
            ccCardStatus.sendPluginResult(powerSaving);
            break;
          case BluetoothReader.CARD_STATUS_POWERED:
            PluginResult powered = new PluginResult(PluginResult.Status.OK, CARD_POWERED);
            powered.setKeepCallback(true);
            ccCardStatus.sendPluginResult(powered);
            break;
        }
      }
    });

    mBluetoothReader.setOnResponseApduAvailableListener((BluetoothReader bluetoothReader, byte[] response, int errorCode) -> {
      if (ccAdpuResponse != null) {
        if (bytesToHex(response) == "6300") {
          PluginResult pluginRes = new PluginResult(PluginResult.Status.ERROR, getAcsErrorCodeJSON(ERR_UNKNOWN, "ADPU response - the operation failed"));
          pluginRes.setKeepCallback(true);
          ccAdpuResponse.sendPluginResult(pluginRes);
        } else if (bytesToHex(response) == "6A81") {
          PluginResult pluginRes = new PluginResult(PluginResult.Status.ERROR, getAcsErrorCodeJSON(ERR_UNKNOWN, "ADPU response - the operation is not supported"));
          pluginRes.setKeepCallback(true);
          ccAdpuResponse.sendPluginResult(pluginRes);
        } else if (response != null) {
          // Remove last 2 entries of the array as they are not part of the ID
          response = Arrays.copyOf(response, response.length - 2);
          PluginResult pluginRes = new PluginResult(PluginResult.Status.OK, byteArrayToJSON(response));
          pluginRes.setKeepCallback(true);
          ccAdpuResponse.sendPluginResult(pluginRes);
        } else {
          PluginResult pluginRes = new PluginResult(PluginResult.Status.ERROR, getAcsErrorCodeJSON(ERR_UNKNOWN, "ADPU response error - " + errorCode));
          pluginRes.setKeepCallback(true);
          ccAdpuResponse.sendPluginResult(pluginRes);
        }
      }
    });

    mBluetoothReader.setOnEscapeResponseAvailableListener((BluetoothReader bluetoothReader, byte[] response, int errorCode) -> {
      if (ccEscapeResponse != null) {
        if (response != null) {
          PluginResult pluginRes = new PluginResult(PluginResult.Status.OK, byteArrayToJSON(response));
          pluginRes.setKeepCallback(true);
          ccEscapeResponse.sendPluginResult(pluginRes);
        } else {
          PluginResult pluginRes = new PluginResult(PluginResult.Status.ERROR, getAcsErrorCodeJSON(ERR_UNKNOWN, "Escape response error - " + errorCode));
          pluginRes.setKeepCallback(true);
          ccEscapeResponse.sendPluginResult(pluginRes);
        }
      }
    });
  }

  private void releaseResources() {
    if (mScanning) {
      mScanning = false;
      ccStartScan = null;
      if (mScanHandler != null) {
        mScanHandler.removeCallbacksAndMessages(null);
      }
      if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() && mBluetoothLeScanner != null) {
        mBluetoothLeScanner.stopScan(mScanCallback);
      }
    }


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
      if(callbackContext == null){
        handler.removeCallbacksAndMessages(null);
        return;
      }

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
    } else if (errorCode == ERR_OPERATION_ALREADY_IN_PROGRESS && customMessage == null) {
      customMessage = "Operation already in progress";
    } else if (errorCode == ERR_SCAN_FAILED && customMessage == null) {
      customMessage = "Scan failed";
    } else if (errorCode == ERR_GATT_ALREADY_CONNECTED && customMessage == null) {
      customMessage = "GATT already connected. Disconnect before connecting a new reader.";
    } else if (errorCode == ERR_GATT_CONNECTION_IN_PROGRESS && customMessage == null) {
      customMessage = "GATT connection already in progress";
    } else if (errorCode == ERR_GATT_CONNECTION_CANCELLED && customMessage == null) {
      customMessage = "GATT connection cancelled";
    } else if (errorCode == ERR_READER_TYPE_NOT_SUPPORTED && customMessage == null) {
      customMessage = "Reader type is not supported. Currently only Acr1255uj1Reader is supported.";
    } else if (customMessage == null) {
      customMessage = "No error message specified. You're in trouble now boy.";
    }

    return gson.toJson(new StatusMessage(errorCode, customMessage));
  }

  private static JSONArray byteArrayToJSON(byte[] bytes) {
    JSONArray json = new JSONArray();
    for (byte aByte : bytes) {
      json.put(aByte);
    }
    return json;
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

