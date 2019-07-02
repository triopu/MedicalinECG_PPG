package medicalin.ekg;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;


public class DeviceScanActivity extends ListActivity {
    private LeDeviceListAdapter leDeviceListAdapter;
    private BluetoothAdapter bluetoothAdapter;
    private boolean scanning;
    private Handler handler;

    private static final int REQUEST_ENABLE_BT = 1;

    //Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        getActionBar().setTitle(R.string.title_devices);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        handler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
            Toast.makeText(this, R.string.ble_not_supported,Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if(bluetoothAdapter == null){
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.main, menu);
        if(!scanning){
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_download).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        }else{
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_download).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.menu_scan:
                leDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;

            case R.id.menu_download:
                final Intent intent = new Intent(this, Main.class);

                if(scanning){
                    bluetoothAdapter.stopLeScan(leScanCallback);
                    scanning = false;
                }
                startActivity(intent);
                break;
        }
        return true;
    }

    @Override
    protected void onResume(){
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if(!bluetoothAdapter.isEnabled()){
            if(!bluetoothAdapter.isEnabled()){
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        //Initializes list view adapter
        leDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(leDeviceListAdapter);
        scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requetsCode, int resultCode, Intent data){
        //User chose not to enable Bluetooth
        if(requetsCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED){
            finish();
            return;
        }
        super.onActivityResult(requetsCode,resultCode,data);
    }

    @Override
    protected void onPause(){
        super.onPause();
        scanLeDevice(false);
        leDeviceListAdapter.clear();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id){
        final BluetoothDevice device = leDeviceListAdapter.getDevice(position);
        if(device == null) return;
        final Intent intent = new Intent(this, Main.class);
        intent.putExtra(Main.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(Main.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if(scanning){
            bluetoothAdapter.stopLeScan(leScanCallback);
            scanning = false;
        }
        startActivity(intent);
    }

    private void scanLeDevice(final boolean enable) {
        if(enable){
            //Stops scanning after a pre-defined scan period
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanning = false;
                    bluetoothAdapter.stopLeScan(leScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            scanning = true;
            bluetoothAdapter.startLeScan(leScanCallback);
        }else{
            scanning = false;
            bluetoothAdapter.stopLeScan(leScanCallback);
        }
        invalidateOptionsMenu();
    }

    private class LeDeviceListAdapter extends BaseAdapter{
        private ArrayList<BluetoothDevice> leDevices;
        private LayoutInflater inflator;

        public LeDeviceListAdapter(){
            super();
            leDevices = new ArrayList<BluetoothDevice>();
            inflator = DeviceScanActivity.this.getLayoutInflater();
        }


        public void addDevice(BluetoothDevice device){
            if(!leDevices.contains(device)){
                leDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position){
            return leDevices.get(position);
        }

        public void clear(){
            leDevices.clear();
        }

        @Override
        public int getCount() {
            return leDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return leDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            //General ListView optimization code.
            if (view == null){
                view = inflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = view.findViewById(R.id.device_address);
                viewHolder.deviceName = view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            }else {
                viewHolder = (ViewHolder)view.getTag();
            }
            BluetoothDevice device = leDevices.get(i);
            final String deviceName = device.getName();
            if(deviceName != null && deviceName.length()> 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());
            return view;
        }
    }

    //Device Scan Callback
    private BluetoothAdapter.LeScanCallback leScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            leDeviceListAdapter.addDevice(device);
                            leDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };

    private class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}
