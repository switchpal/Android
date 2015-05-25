package com.getswitchpal.android;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.*;
import android.content.*;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.*;

import java.util.ArrayList;
import java.util.UUID;

/**
 * The activity that is used when a user has not connect to a device.
 */
public class DeviceActivity extends Activity implements NumberPicker.OnValueChangeListener {

    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private final static String TAG = DeviceActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_ADDRESS = "address";
    public static final String EXTRAS_DEVICE_PASSKEY = "passkey";


    private TextView mConnectionState;
    private boolean mConnected = false;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;

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

        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager == null) {
            Log.e(TAG, "Unable to initialize BluetoothManager.");
            // TODO
            finish();
            return;
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            // TODO
            finish();
        }

        // check if Bluetooth is enabled
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
        }
    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("STATE CONNECTED", "OK");
                // Attempts to discover services after successful connection.
                mBluetoothGatt.discoverServices();
                Log.i("AFTER DISCOVER SERVICES", "OK");

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i("STATE DISCONNECTED", "OK");
                Log.i(TAG, "Disconnected from GATT server.");
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                byte[] data = characteristic.getValue();
                String uuid = characteristic.getUuid().toString();
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
                        Log.i(TAG, "Temperature Range: min=" + mDevice.getTemperatureRangeMin() + ", max=" + mDevice.getTemperatureRangeMax());
                        break;
                    default:
                        Log.i(TAG, "unknown UUID: " + uuid);
                }
                updateView();
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                byte[] data = characteristic.getValue();
                String uuid = characteristic.getUuid().toString();

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
                        Log.i(TAG, "Temperature Range: min=" + mDevice.getTemperatureRangeMin() + ", max=" + mDevice.getTemperatureRangeMax());
                        break;
                    default:
                        Log.i(TAG, "unknown UUID: " + uuid);
                }
                updateView();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            Log.i(TAG, "bluetooth request to enable, resultCode=" +resultCode);
            //bindBluetoothService();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //requestAllDeviceInfo();

        BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager == null) {
            Log.e(TAG, "Unable to initialize BluetoothManager.");
            // TODO
            finish();
            return;
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            // TODO
            finish();
        }

        if (mBluetoothGatt == null) {
            final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mDevice.getAddress());
            if (device == null) {
                Log.w(TAG, "Device not found. Unable to connect.");
                return;
            }
            // We want to directly connect to the device, so we are setting the autoConnect
            // parameter to false.
            mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
            Log.d(TAG, "connect to device");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }

        if (mBluetoothAdapter != null) {
            mBluetoothAdapter = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
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
        });
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

    private BluetoothGattCharacteristic getCharacteristic(String uuid) {
        return mBluetoothGatt.getService(UUID.fromString(SwitchPal.UUID_SERVICE)).getCharacteristic(UUID.fromString(uuid));
    }

    private void requestReadCharacteristic(String uuid) {

        if (mBluetoothGatt == null) {
            Log.e(TAG, "mBluetoothGatt is nul");
            return;
        }

        mBluetoothGatt.readCharacteristic(getCharacteristic(uuid));
    }

    private void requestWriteCharacteristic(String uuid, byte[] data) {

        if (mBluetoothGatt == null) {
            Log.e(TAG, "mBluetoothGatt is nul");
            return;
        }

        BluetoothGattCharacteristic characteristic = getCharacteristic(uuid);
        characteristic.setValue(data);
        mBluetoothGatt.writeCharacteristic(characteristic);
    }

    /**
     * Read current temperature from the device
     */
    private void requestTemperature() {
        // temperature characteristic
        requestReadCharacteristic(SwitchPal.UUID_CHARACTERISTIC_TEMPERATURE);
    }

    /**
     * Read current temperature from the device
     */
    private void requestTemperatureRange() {
        requestReadCharacteristic(SwitchPal.UUID_CHARACTERISTIC_TEMPERATURE_RANGE);
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
        requestReadCharacteristic(SwitchPal.UUID_CHARACTERISTIC_CONTROL_MODE);
    }

    private void setControlMode(Device.ControlMode mode) {
        byte[] data = new byte[1];
        data[0] = mode.toByte();
        requestWriteCharacteristic(SwitchPal.UUID_CHARACTERISTIC_CONTROL_MODE, data);
    }

    private void toggleControlMode() {
        setControlMode(mDevice.getControlMode().getToggle());
    }

    /**
     * Get current switch state from the device
     */
    private void requestSwitchState() {
        requestReadCharacteristic(SwitchPal.UUID_CHARACTERISTIC_SWITCH_STATE);
    }

    private void setSwitchState(Device.SwitchState state) {
        byte[] data = new byte[1];
        data[0] = state.toByte();
        requestWriteCharacteristic(SwitchPal.UUID_CHARACTERISTIC_SWITCH_STATE, data);
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
}