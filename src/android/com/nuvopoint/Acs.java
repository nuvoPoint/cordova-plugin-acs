package android;

import android.bluetooth.*;
import android.content.Context;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;

import com.acs.bluetooth.BluetoothReaderGattCallback;
import com.acs.bluetooth.BluetoothReaderGattCallback.OnConnectionStateChangeListener;

import org.apache.cordova.CallbackContext;

public class CardReader {
    /* Bluetooth GATT client. */
    private BluetoothGatt mBluetoothGatt;
    private BluetoothReaderGattCallback mGattCallback;

    private boolean connectReader(final CallbackContext callbackContext, JSONArray data) {
        String mDeviceAddress = data.getString(0);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            return false;
        }

        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            return false;
        }

        /*
         * Connect Device.
         */
        /* Clear old GATT connection. */
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = nulll

        /* Create a new connection. */
        final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(mDeviceAddress);

        if (device == null) {
            return false;
        }

        /* Connect to GATT server. */
        mBluetoothGatt = device.connect(mDeviceAddress, callbackContext);
        return true;
    }

}
