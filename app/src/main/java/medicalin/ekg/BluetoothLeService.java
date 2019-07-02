package medicalin.ekg;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.List;

public class BluetoothLeService extends Service {

    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private String bluetoothDeviceAddress;
    private BluetoothGatt bluetoothGatt;
    private int connectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED = "medicalin.ekg.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "medicalin.ekg.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "medicalin.ekg.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "medicalin.ekg.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "medicalin.ekg.bluetooth.le.EXTRA_DATA";
    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.

    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback(){
        
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState){
            String intentAction;
            if(newState == BluetoothProfile.STATE_CONNECTED){
                intentAction = ACTION_GATT_CONNECTED;
                connectionState = STATE_CONNECTED;
                broadCastUpdate(intentAction);

                Log.i(TAG,"Connected to GATT server.");
                Log.i(TAG, "Attemping to start service discovery:" + bluetoothGatt.discoverServices());
            }

            else if(newState == BluetoothProfile.STATE_DISCONNECTED){
                intentAction = ACTION_GATT_DISCONNECTED;
                connectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server");
                broadCastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status){
            if(status == BluetoothGatt.GATT_SUCCESS){
                broadCastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            }else{
                Log.w(TAG, "onServicesDiscovered received: "+ status);
            }
        }

        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
            if(status == BluetoothGatt.GATT_SUCCESS){
                broadCastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){
            broadCastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

    };

    private void broadCastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadCastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        final byte[] data = characteristic.getValue();

        if(data != null && data.length> 0){
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for(byte byteChar : data)
                stringBuilder.append(String.format("%02X",byteChar));
            intent.putExtra(EXTRA_DATA, new String(data));
        }
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService(){
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public boolean onUnbind(Intent intent){
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder binder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize(){
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if(bluetoothManager == null){
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if(bluetoothManager == null){
                Log.e(TAG, "Unable to initialize BluetoothManager");
                return false;
            }
        }

        bluetoothAdapter = bluetoothManager.getAdapter();
        if(bluetoothAdapter == null){
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */

    public boolean connect(final String address){
        if(bluetoothAdapter == null || address == null){
            Log.w(TAG,"BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if(bluetoothDeviceAddress != null && address.equals(bluetoothDeviceAddress) && bluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing bluetoothGatt for connection");
            if (bluetoothGatt.connect()) {
                Log.d("Bluetooth GATT is ","CONNECTED");
                connectionState = STATE_CONNECTING;
                return true;
            } else {
                Log.d("Bluetooth GATT is ","DISCONNECTED");
                return false;
            }
        }
        final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);

        if(device == null){
            Log.w(TAG, "Device not found. Unable to connect");
            return false;
        }

        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.

        bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        bluetoothDeviceAddress = address;
        connectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */

    public void disconnect(){
        if(bluetoothAdapter == null || bluetoothGatt == null){
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        bluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close(){
        if(bluetoothGatt == null){
            return;
        }
        bluetoothGatt.close();
        bluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic){
        if(bluetoothAdapter == null || bluetoothGatt == null){
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        bluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled){
        if(bluetoothAdapter == null || bluetoothGatt == null){
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        bluetoothGatt.setCharacteristicNotification(characteristic,enabled);
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedServices(){
        if(bluetoothGatt == null) return null;
        return bluetoothGatt.getServices();
    }

}
