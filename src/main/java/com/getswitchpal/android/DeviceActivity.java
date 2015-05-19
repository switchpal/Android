package com.getswitchpal.android;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The activity that is used when a user has not connect to a device.
 */
public class DeviceActivity extends Activity {

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice;

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

        final String deviceAddress = "64:D2:56:E7:D9:CA";

        final TextView addressText = (TextView) findViewById(R.id.text_device_address);
        addressText.setText(deviceAddress);

        // get hold on the scan button
        final Button connectButton = (Button) findViewById(R.id.button_connect);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectDevice(deviceAddress);
            }
        });

        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
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
}