package android;

import android.bluetooth.*;
import android.content.Context;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;

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
        super.initialize(cordova, webView);

        Runnable runnable = new Runnable() {
            public void run() {
                SumUpState.init(cordova.getActivity());
            }
        };

        cordova.getActivity().runOnUiThread(runnable);
    }


    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
        if(action.equalsIgnoreCase(CONNECT_READER)){
            connectReader(callbackContext, data);
        } else {
            return false;
        }

        return true;
    }


    private String connectReader(final CallbackContext callbackContext, String[] data) {
        String myDeviceAddress = data.getString(0);
        callbackContext.success(myDeviceAddress);


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
