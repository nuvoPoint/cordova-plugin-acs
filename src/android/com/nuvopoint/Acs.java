package android;

import android.bluetooth.*;
import android.content.Context;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.util.Log;

import com.acs.smartcard.Reader;
import com.acs.bluetooth.BluetoothReaderGattCallback;
import com.acs.bluetooth.BluetoothReaderGattCallback.OnConnectionStateChangeListener;

import org.apache.cordova.CallbackContext;

public class Acs extends CordovaPlugin {
    /* Bluetooth GATT client. */
    private BluetoothGatt mBluetoothGatt;
    private BluetoothReaderGattCallback mGattCallback;

    private boolean connectReader(final CallbackContext callbackContext, JSONArray data) {
        String mDeviceAddress = data.getString(0);
        Log.w(mDeviceAddress);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            Log.w("Unable to initialize BluetoothManager.");
            return false;
        }

        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Log.w("Unable to obtain a BluetoothAdapter.");
            return false;
        }

        /*
         * Connect Device.
         */
        /* Clear old GATT connection. */
        if (mBluetoothGatt != null) {
            Log.i("Clear old GATT connection");
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }

        /* Create a new connection. */
        final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(mDeviceAddress);

        if (device == null) {
            Log.w("Device not found. Unable to connect.");
            return false;
        }

        /* Connect to GATT server. */
        mBluetoothGatt = device.connect(mDeviceAddress, callbackContext);
        return true;
    }

}
