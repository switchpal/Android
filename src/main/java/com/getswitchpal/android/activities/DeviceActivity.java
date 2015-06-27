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
import com.getswitchpal.android.utils.*;
import com.getswitchpal.android.widgets.ToggleButton;
import com.parse.ParseAnalytics;

import java.util.*;

/**
 * The activity that is used when a user has not connect to a device.
 *
 */
public class DeviceActivity extends Activity implements PopupMenu.OnMenuItemClickListener {

    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private final static String TAG = DeviceActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_ADDRESS = "address";
    public static final String EXTRAS_DEVICE_PASSKEY = "passkey";

    // whether we have got a complete Device information like control mode and switch state,
    // The temperature will be updated periodically, but control mode and switch state will be notified by the device
    private boolean mHasInitialized = false;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private DeviceOperationQueue mDeviceOperationQueue;
    // we need to distinguish whether a temperature update request is sent automatically or required by the user
    private boolean isManuallyRequestingTemperature;

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
    private TextView mProgressTextView;

    // double back to exit
    private boolean doubleBackToExitPressedOnce = false;

    // we need to update Device info periodically, every 15 seconds
    private int mInterval = 15000;
    private Handler mHandler;


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

        // request to update temperature when clicking on the number
        mTemperatureIntegerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDeviceOperationQueue != null) {
                    isManuallyRequestingTemperature = true;
                    showProgress("Requesting Current Temperature");
                    mDeviceOperationQueue.add(new DeviceReadOperation(Device.UUID_CHARACTERISTIC_TEMPERATURE));
                }
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
                mDeviceOperationQueue.add(new DeviceReadOperation(Device.UUID_CHARACTERISTIC_TEMPERATURE));
                mDeviceOperationQueue.add(new DeviceReadOperation(Device.UUID_CHARACTERISTIC_CONTROL_MODE));
                mDeviceOperationQueue.add(new DeviceReadOperation(Device.UUID_CHARACTERISTIC_SWITCH_STATE));
                mDeviceOperationQueue.add(new DeviceReadOperation(Device.UUID_CHARACTERISTIC_TEMPERATURE_RANGE));
            } else {
                mDeviceOperationQueue.add(new DeviceReadOperation(Device.UUID_CHARACTERISTIC_TEMPERATURE));
            }
            mHandler.postDelayed(mDeviceInfoUpdater, mInterval);
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
                    mDeviceOperationQueue = new DeviceOperationQueue(mBluetoothGatt);
                    // enable notifications
                    mDeviceOperationQueue.add(new DeviceEnableNotificationOperation(Device.UUID_CHARACTERISTIC_SWITCH_STATE));
                    mDeviceOperationQueue.add(new DeviceEnableNotificationOperation(Device.UUID_CHARACTERISTIC_CONTROL_MODE));

                    startRepeatingTask();
                }
            });
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onGattCharacteristicCallback(characteristic, DeviceOperation.Type.READ, status);
                }
            });
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onGattCharacteristicCallback(characteristic, DeviceOperation.Type.WRITE, status);
                }
            });
        }

        /**
         * Respond to notifications from the device
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onGattCharacteristicCallback(characteristic, DeviceOperation.Type.NOTIFY, BluetoothGatt.GATT_SUCCESS);
                }
            });
        }

        /**
         * Respond to DeviceEnableNotificationOperation
         */
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status) {
            Log.d(TAG, "onDescriptorWrite: " + descriptor.getCharacteristic().getUuid().toString());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mDeviceOperationQueue.checkDescriptorResponse(descriptor, status);
                }
            });
        }
    };

    /**
     * Called when onCharacteristicRead, onCharacteristicWrite, onCharacteristicChange
     */
    private void onGattCharacteristicCallback(BluetoothGattCharacteristic characteristic,
                                              DeviceOperation.Type type,
                                              int status) {
        mDeviceOperationQueue.checkCharacteristicResponse(characteristic, type, status);

        if (status != BluetoothGatt.GATT_SUCCESS){
            Log.e(TAG, "onCharacteristicRead error status: " + status);
            return;
        }

        byte[] data = characteristic.getValue();
        String uuid = characteristic.getUuid().toString();
        switch (uuid) {
            case Device.UUID_CHARACTERISTIC_SWITCH_STATE:
                mDevice.setSwitchState(data);
                Log.i(TAG, "Switch State is:" + mDevice.getSwitchState());
                Analytics.trackSwitchState(mDevice.getSwitchState());
                break;
            case Device.UUID_CHARACTERISTIC_CONTROL_MODE:
                mDevice.setControlMode(data);
                Log.i(TAG, "Control Mode is:" + mDevice.getControlMode());
                Analytics.trackControlMode(mDevice.getControlMode());
                break;
            case Device.UUID_CHARACTERISTIC_TEMPERATURE:
                mDevice.setTemperature(data);
                Log.i(TAG, "Temperature is: " + mDevice.getTemperature());
                Analytics.trackTemperature(mDevice.getTemperature());
                break;
            case Device.UUID_CHARACTERISTIC_TEMPERATURE_RANGE:
                mDevice.setTemperatureRange(data);
                Log.i(TAG, "Temperature Range: min=" + mDevice.getTemperatureRangeMin() + ", max=" + mDevice.getTemperatureRangeMax());
                Analytics.trackTemperatureRange(mDevice.getTemperatureRangeMin(), mDevice.getTemperatureRangeMax());
                break;
            default:
                Log.i(TAG, "unknown UUID: " + uuid);
        }
        updateView();

        if (type == DeviceOperation.Type.WRITE) {
            hideProgress();
        }
        if (type == DeviceOperation.Type.READ && uuid.equals(Device.UUID_CHARACTERISTIC_TEMPERATURE) && isManuallyRequestingTemperature) {
            isManuallyRequestingTemperature = false;
            hideProgress();
        }
    }

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
                            Log.d(TAG, "got device" + device.getAddress());
                            Log.d(TAG, "target deivce:" + mDevice.getAddress());
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
     * - we lost the connection and want to reconnect
     */
    private void connectDevice(final BluetoothDevice device) {
        Log.d(TAG, "Connecting to the device");
        if (device == null) {
            Log.w(TAG, "Device not found. Unable to connect.");
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                mBluetoothDevice = device;
                // We want to directly connect to the device, so we are setting the autoConnect parameter to false.
                mBluetoothGatt = device.connectGatt(DeviceActivity.this, false, mGattCallback);
                if (mBluetoothGatt == null) {
                    Toast.makeText(DeviceActivity.this, "device.connectGatt returns null", Toast.LENGTH_LONG).show();
                    return;
                }

                showProgress("Connecting to your SwitchPal device");
            }
        });

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

    /**
     * When to turn on/off the switch
     */
    private void setTemperatureRange(float min, float max) {
        Analytics.trackSetTemperatureRange(min, max);

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

        showProgress("Setting Temperature Range");
        mDeviceOperationQueue.add(new DeviceWriteOperation(Device.UUID_CHARACTERISTIC_TEMPERATURE_RANGE, data));
    }

    private void setControlMode(Device.ControlMode mode) {
        Analytics.trackSetControlMode(mode);

        byte[] data = new byte[1];
        data[0] = mode.toByte();
        mDeviceOperationQueue.add(new DeviceWriteOperation(Device.UUID_CHARACTERISTIC_CONTROL_MODE, data));

        showProgress("Setting Control Mode");
    }

    private void toggleControlMode() {
        setControlMode(mDevice.getControlMode().getToggle());
    }

    private void setSwitchState(Device.SwitchState state) {
        Analytics.trackSetSwitchState(state);

        byte[] data = new byte[1];
        data[0] = state.toByte();
        mDeviceOperationQueue.add(new DeviceWriteOperation(Device.UUID_CHARACTERISTIC_SWITCH_STATE, data));

        showProgress("Setting Switch State");
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
                Analytics.trackClick("Feedback");
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("http://getswitchpal.com/"));
                startActivity(i);
                return true;

            case R.id.menu_new_device:
                Analytics.trackClick("NewDevice");
                Intent intent = new Intent(this, QRScanActivity.class);
                startActivity(intent);
                return true;

            default:
                return false;
        }
    }

    /**
     * Respond to physical menu button click
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            Analytics.trackClick("More");
            moreView.performClick();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }
}
