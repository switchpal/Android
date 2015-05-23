package com.getswitchpal.android;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.*;
import android.content.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The activity that is used when a user has not connect to a device.
 */
public class DeviceActivity extends Activity implements NumberPicker.OnValueChangeListener {

    private final static String TAG = DeviceActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_ADDRESS = "address";
    public static final String EXTRAS_DEVICE_PASSKEY = "passkey";

    private BluetoothAdapter mBluetoothAdapter;

    // device related
    private String mDeviceAddress;
    private String mDevicePasskey;
    // the control mode, true = auto, false = manual
    private boolean mDeviceMode;
    // the state of the switch, on/off
    private boolean mDeviceState;

    private BluetoothLeService mBluetoothLeService;
    private TextView mConnectionState;
    private boolean mConnected = false;

    // the device
    private BluetoothDevice mDevice;

    // double back to exit
    private boolean doubleBackToExitPressedOnce = false;


    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            mBluetoothLeService.initialize();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.i("ON GATT CONNECTED 1", "OK");
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
                Log.i("ON GATT CONNECTED 2", "OK");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                //clearUI();
                Log.i("ON GATT DISCONNECTED", "OK");
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                Log.i("ON GATT SERVICE DISCOVERED", "OK");
                //displayGattServices(mBluetoothLeService.getSupportedGattServices());
                Log.i("ON DISPLAYED SERVICES", "OK");
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.i("ON GATT DATA AVAILABLE", "OK");
                Log.i(TAG, intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //mConnectionState.setText(resourceId);
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set the layout
        setContentView(R.layout.activity_device);

        // hide the actionBar, later we can decide whether to disable the ActionBar globally
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        // get device info from the intent
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mDeviceAddress = extras.getString(EXTRAS_DEVICE_ADDRESS);
            mDevicePasskey = extras.getString(EXTRAS_DEVICE_PASSKEY);
        }

        if (mDeviceAddress == null) {
            //mDeviceAddress = "64:D2:56:E7:D9:CA";
            //mDeviceAddress = "73:4F:6B:15:8A:A4";
            mDeviceAddress = "00:18:31:F1:68:C0";
        }

        final TextView addressText = (TextView) findViewById(R.id.text_device_address);
        addressText.setText(mDeviceAddress);

        // get hold on the scan button
        final Button connectButton = (Button) findViewById(R.id.button_connect);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectDevice(mDeviceAddress);
            }
        });

        // Initializes Bluetooth adapter.
        //final BluetoothManager bluetoothManager =
        //        (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        //mBluetoothAdapter = bluetoothManager.getAdapter();

        // Bind temperature settings
        final LinearLayout rangeGroup = (LinearLayout) findViewById(R.id.group_range);
        rangeGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRangeConfigDialog();
            }
        });

        mConnectionState = (TextView) findViewById(R.id.connection_state);

        // Bind our Bluetooth service
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    /**
     * Connect to the device with a given MAC address
     */
    private void connectDevice(String address) {


        //List<BluetoothGattService> services = mBluetoothLeService.getSupportedGattServices();
        //Log.i(TAG, "number of supported services: " + services.size());
        //for (BluetoothGattService service: services) {
        //    Log.i(TAG, "service: " + service.toString());
        //};


        // temperature characteristic
        BluetoothGattCharacteristic characteristic = mBluetoothLeService.getCharacteristic(SwitchPal.UUID_CHARACTERISTIC_TEMPERATURE_CURRENT);
        //mBluetoothLeService.readCharacteristic(characteristic);
        characteristic.setValue("1");
        boolean status = mBluetoothLeService.getmBluetoothGatt().writeCharacteristic(characteristic);
        Log.d(TAG, "write :" + status);


        //mDevice = mBluetoothAdapter.getRemoteDevice(address);
        //if (mDevice.getName().equals("SwitchPal")) {
        //    Toast.makeText(DeviceActivity.this, "Connected!", Toast.LENGTH_LONG).show();
        //} else {
        //    Toast.makeText(DeviceActivity.this, "Error happens!", Toast.LENGTH_LONG).show();
        //}
    }

    private void showRangeConfigDialog() {
        final Dialog dialog = new Dialog(DeviceActivity.this);
        dialog.setTitle("Temperature Range:");
        dialog.setContentView(R.layout.dialog_temperature);

        // the values that can be selected by user, between 24 to 30
        ArrayList<String> values = new ArrayList<>();
        for (float temp = 24; temp < 30.1; temp += 0.1) {
            values.add(String.format("%2.1f", temp));
        }
        String[] displayedValues = values.toArray(new String[values.size()]);

        // setup min temperature
        final NumberPicker minTempPicker = (NumberPicker) dialog.findViewById(R.id.picker_temp_min);
        minTempPicker.setDisplayedValues(displayedValues);
        minTempPicker.setMaxValue(displayedValues.length - 1);
        minTempPicker.setMinValue(0);
        minTempPicker.setValue(40);
        minTempPicker.setWrapSelectorWheel(false);
        minTempPicker.setOnValueChangedListener(DeviceActivity.this);

        final NumberPicker maxTempPicker = (NumberPicker) dialog.findViewById(R.id.picker_temp_max);
        maxTempPicker.setDisplayedValues(displayedValues);
        maxTempPicker.setMaxValue(displayedValues.length - 1);
        maxTempPicker.setMinValue(0);
        maxTempPicker.setValue(40);
        maxTempPicker.setWrapSelectorWheel(false);
        maxTempPicker.setOnValueChangedListener(DeviceActivity.this);

        // setup button
        Button confirmButton = (Button) dialog.findViewById(R.id.button_confirm);
        Button cancelButton = (Button) dialog.findViewById(R.id.button_cancel);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //tv.setText(String.valueOf(np.getValue())); //set the value to textview
                dialog.dismiss();
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss(); // dismiss the dialog
            }
        });
        dialog.show();
    }

    /**
     * Read current temperature from the device
     */
    private float getTemperature() {
        return 0;
    }

    /**
     * When to turn on/off the switch
     */
    private void setTemperatureRange() {
    }

    /**
     * Get current control mode from the device
     */
    private void getControlMode() {

    }

    private void setControlMode() {
    }

    /**
     * Get current switch state
     */
    private boolean getSwitchState() {
        return false;
    }

    private void setSwitchState(boolean state) {
    }

    /**
     * Double back to exit the app
     */
    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);
    }

    /**
     * Temperature settings
     *
     * @param picker
     * @param oldVal
     * @param newVal
     */
    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}