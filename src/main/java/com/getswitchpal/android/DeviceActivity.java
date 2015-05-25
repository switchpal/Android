package com.getswitchpal.android;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.*;
import android.content.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.*;

import java.util.ArrayList;

/**
 * The activity that is used when a user has not connect to a device.
 */
public class DeviceActivity extends Activity implements NumberPicker.OnValueChangeListener {

    private final static String TAG = DeviceActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_ADDRESS = "address";
    public static final String EXTRAS_DEVICE_PASSKEY = "passkey";


    private BluetoothLeService mBluetoothLeService;
    private TextView mConnectionState;
    private boolean mConnected = false;

    // the device
    private Device mDevice;

    // views
    private TextView mTemperatureView;
    private TextView mTemperatureRangeMinView;
    private TextView mTemperatureRangeMaxView;
    private ToggleButton mSwitchStateToggle;
    private ToggleButton mControlModeToggle;

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
            mBluetoothLeService.connect(mDevice.getAddress());
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
                //Log.i(TAG, intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA));
                byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                String uuid = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
                switch (uuid) {
                    case SwitchPal.UUID_CHARACTERISTIC_SWITCH_STATE:
                        mDevice.setSwitchState(data);
                        Log.i(TAG, "Switch State is:" + mDevice.getSwitchState());
                        break;
                    case SwitchPal.UUID_CHARACTERISTIC_CONTROL_MODE:
                        mDevice.setControlMode(data);
                        Log.i(TAG, "Control Mode is:" + mDevice.getControlMode());
                        break;
                    case SwitchPal.UUID_CHARACTERISTIC_TEMPERATURE:
                        mDevice.setTemperature(data);
                        Log.i(TAG, "Temperature is: " + mDevice.getTemperature());
                        break;
                    case SwitchPal.UUID_CHARACTERISTIC_TEMPERATURE_RANGE:
                        mDevice.setTemperatureRange(data);
                        Log.i(TAG, "Temperature Range: min=" + mDevice.getTemperatureRangeMin() + ", max=" +mDevice.getTemperatureRangeMax());
                        break;
                    default:
                        Log.i(TAG, "unknown UUID: " + uuid);
                }
               updateView();
            } else if (BluetoothLeService.ACTION_DATA_WRITTEN.equals(action)) {
                byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                String uuid = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);

                Log.i(TAG, "data written intent:" + uuid);
                switch (uuid) {
                    case SwitchPal.UUID_CHARACTERISTIC_SWITCH_STATE:
                        mDevice.setSwitchState(data);
                        Log.i(TAG, "Switch State is:" + mDevice.getSwitchState());
                        break;
                    case SwitchPal.UUID_CHARACTERISTIC_CONTROL_MODE:
                        mDevice.setControlMode(data);
                        Log.i(TAG, "Control Mode is:" + mDevice.getControlMode());
                        break;
                    case SwitchPal.UUID_CHARACTERISTIC_TEMPERATURE_RANGE:
                        mDevice.setTemperatureRange(data);
                        Log.i(TAG, "Temperature Range: min=" + mDevice.getTemperatureRangeMin() + ", max=" +mDevice.getTemperatureRangeMax());
                        break;
                    default:
                        Log.i(TAG, "unknown UUID: " + uuid);
                }
                updateView();
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

        // device related
        String mDeviceAddress = null;
        String mDevicePasskey = null;

        // get device info from the intent
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mDeviceAddress = extras.getString(EXTRAS_DEVICE_ADDRESS);
            mDevicePasskey = extras.getString(EXTRAS_DEVICE_PASSKEY);
        }

        if (mDeviceAddress == null) {
            mDeviceAddress = "00:18:31:F1:68:C0";
        }

        mDevice = new Device(mDeviceAddress, mDevicePasskey);

        final TextView addressText = (TextView) findViewById(R.id.text_device_address);
        addressText.setText(mDevice.getAddress());

        mTemperatureView = (TextView) findViewById(R.id.text_temperature);
        mTemperatureRangeMinView = (TextView) findViewById(R.id.text_temperature_min);
        mTemperatureRangeMaxView = (TextView) findViewById(R.id.text_temperature_max);
        mSwitchStateToggle = (ToggleButton) findViewById(R.id.button_switch);
        mControlModeToggle = (ToggleButton) findViewById(R.id.button_mode);

        mSwitchStateToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSwitchState();
            }
        });

        mControlModeToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleControlMode();
            }
        });


        // get hold on the scan button
        final Button connectButton = (Button) findViewById(R.id.button_connect);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestAllDeviceInfo();
            }
        });

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
            final boolean result = mBluetoothLeService.connect(mDevice.getAddress());
            Log.d(TAG, "Connect request result=" + result);

            requestAllDeviceInfo();
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
    private void requestAllDeviceInfo() {
        //requestSwitchState();
        //requestControlMode();
        requestTemperature();
        //requestTemperatureRange();
    }

    /**
     * Update Views:
     * - temperature
     * - temperature range
     * - switch state
     * - control mode
     */
    private void updateView() {
        mTemperatureView.setText(String.format("%.1f", mDevice.getTemperature()));
        mTemperatureRangeMinView.setText(String.format("%.1f", mDevice.getTemperatureRangeMin()));
        mTemperatureRangeMaxView.setText(String.format("%.1f", mDevice.getTemperatureRangeMax()));

        if (mDevice.getSwitchState().isValid()) {
            mSwitchStateToggle.setChecked(mDevice.getSwitchState().toBoolean());
        }
        if (mDevice.getControlMode().isValid()) {
            mControlModeToggle.setChecked(mDevice.getControlMode().toBoolean());
        }
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
    private void requestTemperature() {
        // temperature characteristic
        BluetoothGattCharacteristic characteristic = mBluetoothLeService.getCharacteristic(SwitchPal.UUID_CHARACTERISTIC_TEMPERATURE);
        mBluetoothLeService.readCharacteristic(characteristic);
    }

    /**
     * Read current temperature from the device
     */
    private void requestTemperatureRange() {
        // temperature characteristic
        BluetoothGattCharacteristic characteristic = mBluetoothLeService.getCharacteristic(SwitchPal.UUID_CHARACTERISTIC_TEMPERATURE_RANGE);
        mBluetoothLeService.readCharacteristic(characteristic);
    }

    /**
     * When to turn on/off the switch
     */
    private void setTemperatureRange() {
    }

    /**
     * Get current control mode from the device
     */
    private void requestControlMode() {
        // temperature characteristic
        BluetoothGattCharacteristic characteristic = mBluetoothLeService.getCharacteristic(SwitchPal.UUID_CHARACTERISTIC_CONTROL_MODE);
        mBluetoothLeService.readCharacteristic(characteristic);
    }

    private void setControlMode(Device.ControlMode mode) {
        byte[] data = new byte[1];
        data[0] = mode.toByte();
        BluetoothGattCharacteristic characteristic = mBluetoothLeService.getCharacteristic(SwitchPal.UUID_CHARACTERISTIC_CONTROL_MODE);
        characteristic.setValue(data);
        boolean status = mBluetoothLeService.getmBluetoothGatt().writeCharacteristic(characteristic);
        Log.d(TAG, "write :" + status);
    }

    private void toggleControlMode() {
        setControlMode(mDevice.getControlMode().getToggle());
    }

    /**
     * Get current switch state from the device
     */
    private void requestSwitchState() {
        // temperature characteristic
        BluetoothGattCharacteristic characteristic = mBluetoothLeService.getCharacteristic(SwitchPal.UUID_CHARACTERISTIC_SWITCH_STATE);
        mBluetoothLeService.readCharacteristic(characteristic);
    }

    private void setSwitchState(Device.SwitchState state) {
        byte[] data = new byte[1];
        data[0] = state.toByte();
        BluetoothGattCharacteristic characteristic = mBluetoothLeService.getCharacteristic(SwitchPal.UUID_CHARACTERISTIC_SWITCH_STATE);
        characteristic.setValue(data);
        boolean status = mBluetoothLeService.getmBluetoothGatt().writeCharacteristic(characteristic);
        Log.d(TAG, "write :" + status);
    }

    private void toggleSwitchState() {
        setSwitchState(mDevice.getSwitchState().getToggle());
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
                doubleBackToExitPressedOnce = false;
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
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_WRITTEN);
        return intentFilter;
    }
}