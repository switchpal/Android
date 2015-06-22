package com.getswitchpal.android.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.*;
import android.content.*;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.getswitchpal.android.*;
import com.getswitchpal.android.dialogs.BluetoothScanFailedDialog;
import com.getswitchpal.android.utils.Device;
import com.getswitchpal.android.widgets.ToggleButton;
import com.parse.ParseAnalytics;

import java.util.*;

/**
 * The activity that is used when a user has not connect to a device.
 *
 *
 */
public class DeviceActivity extends Activity implements PopupMenu.OnMenuItemClickListener {

    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private final static String TAG = DeviceActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_ADDRESS = "address";
    public static final String EXTRAS_DEVICE_PASSKEY = "passkey";


    private TextView mConnectionState;
    private boolean mConnected = false;

    // whether we have got a complete Device information like control mode and switch state,
    // The temperature will be updated periodically, but control mode and switch state will be notified by the device
    private boolean mHasInitialized = false;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;

    // the device
    private Device mDevice;
    private BluetoothDevice mBluetoothDevice;

    // views
    private TextView mTemperatureIntegerView;
    private TextView mTemperatureFractionalView;
    private TextView mTemperatureRangeMinView;
    private TextView mTemperatureRangeMaxView;
    private ToggleButton mSwitchStateToggle;
    private ToggleButton mControlModeToggle;
    private TextView mSwitchStateView;
    private TextView mControlModeView;

    private ImageView moreView;

    private RelativeLayout mProgressOverlay;
    private ProgressBar mProgressBar;
    private TextView mProgressTextView;

    // double back to exit
    private boolean doubleBackToExitPressedOnce = false;

    // we need to update Device info periodically, every 15 seconds
    private int mInterval = 15000;
    private Handler mHandler;

    // when requesting Device info periodically, we need to request characteristics one by one
    private Stack<String> queuedReadCharacteristicUuids = new Stack<>();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set the layout
        setContentView(R.layout.activity_device);

        // get device info from the intent
        Bundle extras = getIntent().getExtras();

        // device related
        String mDeviceAddress = null;
        String mDevicePasskey = null;
        if (extras != null) {
            mDeviceAddress = extras.getString(EXTRAS_DEVICE_ADDRESS);
            mDevicePasskey = extras.getString(EXTRAS_DEVICE_PASSKEY);
        }

        if (mDeviceAddress == null) {
            Toast.makeText(DeviceActivity.this, "No device address is given, this is wrong!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mDevice = new Device(mDeviceAddress, mDevicePasskey);

        mTemperatureIntegerView = (TextView) findViewById(R.id.text_temperature_integer);
        mTemperatureFractionalView = (TextView) findViewById(R.id.text_temperature_fractional);
        mTemperatureRangeMinView = (TextView) findViewById(R.id.text_temperature_min);
        mTemperatureRangeMaxView = (TextView) findViewById(R.id.text_temperature_max);
        mSwitchStateToggle = (com.getswitchpal.android.widgets.ToggleButton) findViewById(R.id.button_switch);
        mControlModeToggle = (ToggleButton) findViewById(R.id.button_mode);
        mSwitchStateView = (TextView) findViewById(R.id.text_state);
        mControlModeView = (TextView) findViewById(R.id.text_mode);

        mProgressOverlay = (RelativeLayout) findViewById(R.id.overlay_progress);
        mProgressTextView = (TextView) findViewById(R.id.text_progress);

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

        mHandler = new Handler();

        moreView = (ImageView) findViewById(R.id.image_more);
        moreView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popup = new PopupMenu(DeviceActivity.this, view);
                popup.setOnMenuItemClickListener(DeviceActivity.this);
                popup.inflate(R.menu.more);
                popup.show();
            }
        });

        // Bind temperature settings
        final ImageView configButton = (ImageView) findViewById(R.id.button_config);
        configButton.setOnClickListener(new View.OnClickListener() {
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

    Runnable mDeviceInfoUpdater = new Runnable() {
        @Override
        public void run() {

            // we force a full device info read at resume
            if (!mHasInitialized) {
                mHasInitialized = true;
                // queue all the readable info
                queuedReadCharacteristicUuids.push(Device.UUID_CHARACTERISTIC_CONTROL_MODE);
                queuedReadCharacteristicUuids.push(Device.UUID_CHARACTERISTIC_SWITCH_STATE);
                queuedReadCharacteristicUuids.push(Device.UUID_CHARACTERISTIC_TEMPERATURE_RANGE);

                //queuedReadCharacteristicUuids.push(SwitchPal.UUID_CHARACTERISTIC_TEMPERATURE);
                requestTemperature();

                mHandler.postDelayed(mDeviceInfoUpdater, mInterval);
            } else if (isManualOperationInProgress()) {
                // if user operation is in progress, back off and try again later.
                mHandler.postDelayed(mDeviceInfoUpdater, 1000);
            } else {
                requestTemperature();
                mHandler.postDelayed(mDeviceInfoUpdater, mInterval);
            }
        }
    };

    void startRepeatingTask() {
        // we force a full device info read at resume
        mHasInitialized = false;
        mDeviceInfoUpdater.run();
    }

    void stopRepeatingTask() {
        mHandler.removeCallbacks(mDeviceInfoUpdater);
    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, final int newState) {
            String intentAction;

            // For debugging purpose
            //runOnUiThread(new Runnable() {
            //    @Override
            //    public void run() {
            //        Toast.makeText(DeviceActivity.this, "BluetoothGattCallback state change to:" + newState, Toast.LENGTH_LONG).show();
            //    }
            //});

            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    hideProgress();
                    Log.i(TAG, "STATE CONNECTED");
                    // Attempts to discover services after successful connection.
                    mBluetoothGatt.discoverServices();
                    // before the services is discovered, we should not allow user to interact with the device
                    showProgress("Checking remote devices");
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.i(TAG, "Disconnected from GATT server.");
                    mDevice.setIsConnected(false);

                    showProgress("Reconnecting to device");
                    connectDevice(mBluetoothDevice);
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    hideProgress();
                    mDevice.setIsConnected(true);
                    startRepeatingTask();
                }
            });
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                byte[] data = characteristic.getValue();
                String uuid = characteristic.getUuid().toString();
                switch (uuid) {
                    case Device.UUID_CHARACTERISTIC_SWITCH_STATE:
                        mDevice.setSwitchState(data);
                        Log.i(TAG, "Switch State is:" + mDevice.getSwitchState());
                        break;
                    case Device.UUID_CHARACTERISTIC_CONTROL_MODE:
                        mDevice.setControlMode(data);
                        Log.i(TAG, "Control Mode is:" + mDevice.getControlMode());
                        break;
                    case Device.UUID_CHARACTERISTIC_TEMPERATURE:
                        mDevice.setTemperature(data);
                        Log.i(TAG, "Temperature is: " + mDevice.getTemperature());
                        break;
                    case Device.UUID_CHARACTERISTIC_TEMPERATURE_RANGE:
                        mDevice.setTemperatureRange(data);
                        Log.i(TAG, "Temperature Range: min=" + mDevice.getTemperatureRangeMin() + ", max=" + mDevice.getTemperatureRangeMax());
                        break;
                    default:
                        Log.i(TAG, "unknown UUID: " + uuid);
                }
                updateView();
                if (queuedReadCharacteristicUuids.size() != 0) {
                    requestReadCharacteristic(queuedReadCharacteristicUuids.pop());
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                byte[] data = characteristic.getValue();
                String uuid = characteristic.getUuid().toString();

                Log.i(TAG, "data written intent:" + uuid);
                switch (uuid) {
                    case Device.UUID_CHARACTERISTIC_SWITCH_STATE:
                        mDevice.setSwitchState(data);
                        Log.i(TAG, "Switch State is:" + mDevice.getSwitchState());
                        break;
                    case Device.UUID_CHARACTERISTIC_CONTROL_MODE:
                        mDevice.setControlMode(data);
                        Log.i(TAG, "Control Mode is:" + mDevice.getControlMode());
                        break;
                    case Device.UUID_CHARACTERISTIC_TEMPERATURE_RANGE:
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

            boolean foundInExistingPairedDevices = false;
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice d: pairedDevices) {
                    if (d.getAddress().equals(mDevice.getAddress())) {
                        foundInExistingPairedDevices = true;
                        connectDevice(d);
                        break;
                    }
                }
            }
            // not found, lets start a scanning
            // Register the BroadcastReceiver
            if (!foundInExistingPairedDevices) {

                // Stops scanning after a pre-defined scan period.
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mScanning) {
                            mScanning = false;
                            if (mBluetoothAdapter != null) {
                                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                            }
                            DialogFragment dialog = new BluetoothScanFailedDialog();
                            dialog.show(getFragmentManager(), TAG);
                        }
                    }
                }, SCAN_PERIOD);

                mScanning = true;
                mBluetoothAdapter.startLeScan(mLeScanCallback);
                showProgress("Scanning devices");
            }
        }
    }

    private static boolean mScanning = false;
    private static final long SCAN_PERIOD = 10000;

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (device.getAddress().equals(mDevice.getAddress())) {
                                mScanning = false;
                                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                                hideProgress();

                                connectDevice(device);
                            }
                        }
                    });
                }


            };

    /**
     * We may comes here by two ways:
     * - we found the device has been paired before, so no scanning is involved
     * - we found the device by scanning
     */
    private void connectDevice(BluetoothDevice device) {
        Log.d(TAG, "Connecting to the device");
        if (device == null) {
            Log.w(TAG, "Device not found. Unable to connect.");
            return;
        }

        mBluetoothDevice = device;
        // We want to directly connect to the device, so we are setting the autoConnect parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        if (mBluetoothGatt == null) {
            Toast.makeText(this, "device.connectGatt returns null", Toast.LENGTH_LONG).show();
            return;
        }
        showProgress("Connecting to your SwitchPal device");
    }


    @Override
    protected void onPause() {
        super.onPause();

        stopRepeatingTask();

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
                mTemperatureIntegerView.setText(String.format("%d", (int) mDevice.getTemperature()));
                mTemperatureFractionalView.setText(String.format(".%d", (int) ((mDevice.getTemperature() - (int) mDevice.getTemperature()) * 10)));
                mTemperatureRangeMinView.setText(String.format("MIN: %.1f", mDevice.getTemperatureRangeMin()));
                mTemperatureRangeMaxView.setText(String.format("MAX: %.1f", mDevice.getTemperatureRangeMax()));

                Device.SwitchState state = mDevice.getSwitchState();
                if (state.isValid()) {
                    mSwitchStateToggle.setChecked(state.toBoolean());
                    if (state.toBoolean()) {
                        mSwitchStateView.setText("On");
                    } else {
                        mSwitchStateView.setText("Off");
                    }
                }

                Device.ControlMode mode = mDevice.getControlMode();
                if (mode.isValid()) {
                    mControlModeToggle.setChecked(mode.toBoolean());
                    if (mode.toBoolean()) {
                        mControlModeView.setText("Auto");
                    } else {
                        mControlModeView.setText("Manual");
                    }
                }

                // since the view is updated, remove ProgressBar
                hideProgress();
            }
        });
    }

    private void showRangeConfigDialog() {
        final Dialog dialog = new Dialog(DeviceActivity.this);
        dialog.setTitle("Temperature Range:");
        dialog.setContentView(R.layout.dialog_temperature);

        // the values that can be selected by user, between 24 to 30
        final ArrayList<String> values = new ArrayList<>();
        for (float temp = 24; temp < 30.1; temp += 0.1) {
            values.add(String.format("%2.1f", temp));
        }

        final SeekBar minTemperatureBar = (SeekBar) dialog.findViewById(R.id.slider_temperature_range_min);
        final TextView minTemperatureView = (TextView) dialog.findViewById(R.id.text_temperature_range_min);
        final SeekBar maxTemperatureBar = (SeekBar) dialog.findViewById(R.id.slider_temperature_range_max);
        final TextView maxTemperatureView = (TextView) dialog.findViewById(R.id.text_temperature_range_max);
        minTemperatureBar.setMax(values.size() - 1);
        maxTemperatureBar.setMax(values.size() - 1);

        int minProgress = values.indexOf(String.format("%2.1f", mDevice.getTemperatureRangeMin()));
        if (minProgress == -1) {
            minProgress = 0;
        }
        int maxProgress = values.indexOf(String.format("%2.1f", mDevice.getTemperatureRangeMax()));
        if (maxProgress == -1) {
            maxProgress = 10;
        }

        minTemperatureBar.setProgress(minProgress);
        maxTemperatureBar.setProgress(maxProgress);
        minTemperatureView.setText(values.get(minTemperatureBar.getProgress()));
        maxTemperatureView.setText(values.get(maxTemperatureBar.getProgress()));

        final SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                String text = values.get(seekBar.getProgress());
                float min = Float.parseFloat(values.get(minTemperatureBar.getProgress()));
                float max = Float.parseFloat(values.get(maxTemperatureBar.getProgress()));
                float diff = max - min;

                if (seekBar == minTemperatureBar) {
                    minTemperatureView.setText(text);
                    // if min is greater than max, adjust max
                    if (diff < 1) {
                        if (maxTemperatureBar.getProgress() == maxTemperatureBar.getMax()) {
                            minTemperatureBar.setProgress(maxTemperatureBar.getMax() - 10);
                        } else {
                            maxTemperatureBar.setProgress(minTemperatureBar.getProgress() + 10);
                        }
                    }
                } else if (seekBar == maxTemperatureBar) {
                    maxTemperatureView.setText(text);
                    // if max is lower than min, adjust min
                    if (diff < 1) {
                        if (minTemperatureBar.getProgress() == 0) {
                            maxTemperatureBar.setProgress(10);
                        } else {
                            minTemperatureBar.setProgress(maxTemperatureBar.getProgress() - 10);
                        }
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };
        minTemperatureBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
        maxTemperatureBar.setOnSeekBarChangeListener(onSeekBarChangeListener);

        // setup button
        Button confirmButton = (Button) dialog.findViewById(R.id.button_confirm);
        Button cancelButton = (Button) dialog.findViewById(R.id.button_cancel);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //tv.setText(String.valueOf(np.getValue())); //set the value to textview
                float min = Float.parseFloat(values.get(minTemperatureBar.getProgress()));
                float max = Float.parseFloat(values.get(maxTemperatureBar.getProgress()));
                if ((Math.abs(min - mDevice.getTemperatureRangeMin()) > 0.1) || (Math.abs(max - mDevice.getTemperatureRangeMax()) > 0.1)) {
                    setTemperatureRange(min, max);
                }
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

    private boolean isManualOperationInProgress() {
        return mProgressOverlay.getVisibility() == View.VISIBLE;
    }

    ShowProgressTaskForLongOperations showProgressTaskForLongOperationsTask = null;

    /**
     * Add a overlay showing an operation is in progress
     *
     * The progress overlay is only displayed if the operation did not finish in 1 seconds
     */
    private void showProgress(String text) {
        String displayText;
        if (text == null) {
            displayText = "";
        } else {
            displayText = text;
        }
        showProgressTaskForLongOperationsTask = new ShowProgressTaskForLongOperations();
        showProgressTaskForLongOperationsTask.doInBackground(displayText);
    }

    /**
     * Background task to show progress overlay for long-running operations
     */
    private class ShowProgressTaskForLongOperations extends AsyncTask<String, Void, Void> {
        @Override
        protected void onPreExecute() {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected Void doInBackground(String... texts) {
            Log.d(TAG, "showing progress dialog in background thread");
            final String displayText = texts[0];
            // check if the task has been cancelled
            if (!isCancelled()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mProgressTextView.setText(displayText);
                        mProgressOverlay.setVisibility(View.VISIBLE);
                    }
                });
            }

            // lets double check
            if (isCancelled()) {
                hideProgress();
            }
            return null;
        }
    }

    /**
     * Remove the overlay
     */
    private void hideProgress() {
        if (showProgressTaskForLongOperationsTask != null) {
            showProgressTaskForLongOperationsTask.cancel(true);
            showProgressTaskForLongOperationsTask = null;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressOverlay.setVisibility(View.INVISIBLE);
            }
        });
    }

    private BluetoothGattCharacteristic getCharacteristic(String uuid) {
        BluetoothGattService service = mBluetoothGatt.getService(UUID.fromString(Device.UUID_SERVICE));
        return service.getCharacteristic(UUID.fromString(uuid));
    }

    private void requestReadCharacteristic(String uuid) {
        //showProgress("Communicating with your SwitchPal Device");
        if (!mDevice.isConnected()) {
            Toast.makeText(this, "Device is disconnected, unable to command", Toast.LENGTH_LONG).show();
            return;
        }

        if (mBluetoothGatt == null) {
            Log.e(TAG, "mBluetoothGatt is nul");
            return;
        }

        mBluetoothGatt.readCharacteristic(getCharacteristic(uuid));
    }

    private void requestWriteCharacteristic(String uuid, byte[] data) {
        if (!mDevice.isConnected()) {
            Toast.makeText(this, "Device is disconnected, unable to command", Toast.LENGTH_LONG).show();
            return;
        }

        showProgress("Commanding your SwitchPal Device");

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
        requestReadCharacteristic(Device.UUID_CHARACTERISTIC_TEMPERATURE);
    }

    /**
     * Read current temperature from the device
     */
    private void requestTemperatureRange() {
        requestReadCharacteristic(Device.UUID_CHARACTERISTIC_TEMPERATURE_RANGE);
    }

    /**
     * When to turn on/off the switch
     */
    private void setTemperatureRange(float min, float max) {

        if (min < 20 || min > 32 || max < 20 || max > 32) {
            new AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage("Temperature should between 20 to 32.")
                    .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //
                        }
                    })
                    .show();
            return;
        }

        mDevice.setTemperatureRange(min, max);

        byte[] data = new byte[4];
        data[0] = (byte) ((int) min);
        data[1] = (byte) ((min - (int) min) * 100);
        data[2] = (byte) ((int) max);
        data[3] = (byte) ((max - (int) max) * 100);

        requestWriteCharacteristic(Device.UUID_CHARACTERISTIC_TEMPERATURE_RANGE, data);
    }

    /**
     * Get current control mode from the device
     */
    private void requestControlMode() {
        requestReadCharacteristic(Device.UUID_CHARACTERISTIC_CONTROL_MODE);
    }

    private void setControlMode(Device.ControlMode mode) {
        Map<String, String> dimensions = new HashMap<>();
        String modeString;
        if (mode == Device.ControlMode.AUTO) {
            modeString = "auto";
        } else if (mode == Device.ControlMode.MANUAL) {
            modeString = "manual";
        } else {
            modeString = "unknown";
        }
        dimensions.put("mode", modeString);
        ParseAnalytics.trackEventInBackground("setControlMode", dimensions);

        byte[] data = new byte[1];
        data[0] = mode.toByte();
        requestWriteCharacteristic(Device.UUID_CHARACTERISTIC_CONTROL_MODE, data);
    }

    private void toggleControlMode() {
        setControlMode(mDevice.getControlMode().getToggle());
    }

    /**
     * Get current switch state from the device
     */
    private void requestSwitchState() {
        requestReadCharacteristic(Device.UUID_CHARACTERISTIC_SWITCH_STATE);
    }

    private void setSwitchState(Device.SwitchState state) {
        Map<String, String> dimensions = new HashMap<>();
        String stateString;
        if (state == Device.SwitchState.ON) {
            stateString = "on";
        } else if (state == Device.SwitchState.OFF) {
            stateString = "off";
        } else {
            stateString = "unknown";
        }
        dimensions.put("state", stateString);
        ParseAnalytics.trackEventInBackground("setSwitchState", dimensions);

        byte[] data = new byte[1];
        data[0] = state.toByte();
        requestWriteCharacteristic(Device.UUID_CHARACTERISTIC_SWITCH_STATE, data);
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
     * We have a more icon, that contains two menu items:
     * - menu_feedback: Go to the website, and allows user to leave comments
     * - menu_new_device: Scan a new device
     */
    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_feedback:
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("http://getswitchpal.com/"));
                startActivity(i);
                return true;

            case R.id.menu_new_device:
                Intent intent = new Intent(this, QRScanActivity.class);
                startActivity(intent);
                return true;

            default:
                return false;
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            moreView.performClick();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }
}
