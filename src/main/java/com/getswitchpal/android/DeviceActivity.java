package com.getswitchpal.android;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.*;

import java.util.ArrayList;
import java.util.Locale;

/**
 * The activity that is used when a user has not connect to a device.
 */
public class DeviceActivity extends Activity implements NumberPicker.OnValueChangeListener {

    public static final String EXTRAS_DEVICE_ADDRESS = "address";
    public static final String EXTRAS_DEVICE_PASSKEY = "passkey";

    private BluetoothAdapter mBluetoothAdapter;

    // device related
    private String mDeviceAddress;
    private String mDevicePasskey;
    private BluetoothDevice mDevice;

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

        // get device info from the intent
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mDeviceAddress = extras.getString(EXTRAS_DEVICE_ADDRESS);
            mDevicePasskey = extras.getString(EXTRAS_DEVICE_PASSKEY);
        }

        if (mDeviceAddress == null) {
            mDeviceAddress = "64:D2:56:E7:D9:CA";
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
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        final LinearLayout rangeGroup = (LinearLayout) findViewById(R.id.group_range);
        rangeGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRangeConfigDialog();
            }
        });
    }

    /**
     * Connect to the device with a given MAC address
     */
    private void connectDevice(String address) {
        mDevice = mBluetoothAdapter.getRemoteDevice(address);
        if (mDevice.getName().equals("SwitchPal")) {
            Toast.makeText(DeviceActivity.this, "Connected!", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(DeviceActivity.this, "Error happens!", Toast.LENGTH_LONG).show();
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

    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {

    }
}