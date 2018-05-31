package android;

import android.bluetooth.*;
import android.content.Context;
import android.content.Intent;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;

import java.math.BigDecimal;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.acs.bluetooth.BluetoothReaderGattCallback;
import com.acs.bluetooth.BluetoothReaderGattCallback.OnConnectionStateChangeListener;

import org.apache.cordova.CallbackContext;

public class CardReader extends CordovaPlugin {
    private static final String CONNECT_READER = "connectReader";

    /* Bluetooth GATT client. */
    private BluetoothGatt mBluetoothGatt;
    private BluetoothReaderGattCallback mGattCallback;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    }


    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
        if (action.equalsIgnoreCase(CONNECT_READER)) {
            connectReader(callbackContext, data);
        } else {
            return false;
        }

        return true;
    }


    private void connectReader(final CallbackContext callbackContext, JSONArray data) {
        try {
            String myDeviceAddress = data.getString(0);
            callbackContext.success(myDeviceAddress);
        } catch (Exception e){
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
