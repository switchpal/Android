package com.getswitchpal.android.utils;

import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import java.util.Locale;

/**
 * Wrap information about a SwitchPal device
 */
public class Device {

    // service UUID
    public static final String UUID_SERVICE = "0000fff0-0000-1000-8000-00805f9b34fb";
    // the switch state, read and write
    public static final String UUID_CHARACTERISTIC_SWITCH_STATE = "0000fff1-0000-1000-8000-00805f9b34fb";
    // the control mode, read and write
    public static final String UUID_CHARACTERISTIC_CONTROL_MODE = "0000fff2-0000-1000-8000-00805f9b34fb";
    // the current temperature, read only
    public static final String UUID_CHARACTERISTIC_TEMPERATURE = "0000fff3-0000-1000-8000-00805f9b34fb";
    // temperature range, read and write
    public static final String UUID_CHARACTERISTIC_TEMPERATURE_RANGE = "0000fff4-0000-1000-8000-00805f9b34fb";

    public enum ControlMode {
        MANUAL, AUTO, UNKNOWN;

        private ControlMode toggle;
        private byte data;

        static {
            MANUAL.toggle = AUTO;
            AUTO.toggle = MANUAL;
            UNKNOWN.toggle = AUTO;

            MANUAL.data = 0x0;
            AUTO.data = 0x1;
        }

        public boolean isValid() {
            return this != UNKNOWN;
        }

        public byte toByte() {
            if (this == UNKNOWN) {
                throw new RuntimeException("Unknown should never call toByte");
            }
            return data;
        }

        public boolean toBoolean() {
            if (this == UNKNOWN) {
                throw new RuntimeException("Unknown should never call toBoolean");
            }
            return data == 0x1;
        }

        public ControlMode getToggle() {
            return toggle;
        }
    }

    public enum SwitchState {
        OFF, ON, UNKNOWN;

        private SwitchState toggle;
        private byte data;

        static {
            OFF.toggle = ON;
            ON.toggle = OFF;
            UNKNOWN.toggle = ON;

            OFF.data = 0x0;
            ON.data = 0x1;
        }

        public boolean isValid() {
            return this != UNKNOWN;
        }

        public byte toByte() {
            if (this == UNKNOWN) {
                throw new RuntimeException("Unknown should never call toByte");
            }
            return data;
        }

        public boolean toBoolean() {
            if (this == UNKNOWN) {
                throw new RuntimeException("Unknown should never call toBoolean");
            }
            return data == 0x1;
        }

        public SwitchState getToggle() {
            return toggle;
        }
    }

    private static final String TAG = Device.class.getSimpleName();

    private String address;
    private String passkey;

    private ControlMode controlMode;
    private SwitchState switchState;

    private float temperature;
    private float temperatureRangeMin;
    private float temperatureRangeMax;

    public boolean isConnected() {
        return isConnected;
    }

    public void setIsConnected(boolean isConnected) {
        this.isConnected = isConnected;
    }

    private boolean isConnected = false;

    public Device(String address, String passkey) {
        this.address = address;
        this.passkey = passkey;

        this.controlMode = ControlMode.UNKNOWN;
        this.switchState = SwitchState.UNKNOWN;
    }

    public String getAddress() {
        return address;
    }

    public String getPasskey() {
        return passkey;
    }

    public ControlMode getControlMode() {
        return controlMode;
    }

    public void setControlMode(ControlMode controlMode) {
        this.controlMode = controlMode;
    }

    public void setControlMode(byte[] data) {
        if (data.length != 1) {
            Log.e(TAG, "control mode data is invalid");
            return;
        }

        if (data[0] == (byte) 0x00) {
            this.controlMode = ControlMode.MANUAL;
        } else if (data[0] == (byte) 0x01) {
            this.controlMode = ControlMode.AUTO;
        } else {
            Log.e(TAG, "unknown control mode:" + String.format("%02X", data[0]));
        }
    }

    public SwitchState getSwitchState() {
        return switchState;
    }

    public void setSwitchState(SwitchState switchState) {
        this.switchState = switchState;
    }

    public void setSwitchState(byte[] data) {
        if (data.length != 1) {
            Log.e(TAG, "switch state data is invalid");
            return;
        }

        if (data[0] == (byte) 0x00) {
            this.switchState = SwitchState.OFF;
        } else if (data[0] == (byte) 0x01) {
            this.switchState = SwitchState.ON;
        } else {
            Log.e(TAG, "unknown switch state byte: " + String.format("%02X", data[0]));
        }
    }

    /**
     * Decode http://getswitchpal.com/app/?device=0123456789AB,
     *
     * @return Device
     */
    public static Device getDeviceInfoFromQRCode(String url) {

        if (url == null || !url.contains("getswitchpal.com")) {
            return null;
        }

        Uri uri = Uri.parse(url);
        String deviceInfoString = uri.getQueryParameter("device");
        return decodeDeviceInfo(deviceInfoString);
    }

    private final static String PREF_ADDRESS = "deviceAddress";
    private final static String PREF_PASSKEY = "devicePasskey";
    public static Device getDeviceFromSharedPreferences(SharedPreferences sharedPref) {
        String address = sharedPref.getString(PREF_ADDRESS, null);
        String passkey = sharedPref.getString(PREF_PASSKEY, null);
        if (address == null || passkey == null) {
            return null;
        }
        return new Device(address, passkey);
    }

    public void writeSharedPreferences(SharedPreferences sharedPref) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(PREF_ADDRESS, getAddress());
        editor.putString(PREF_PASSKEY, getPasskey());
        if (!editor.commit()) {
            Log.e(TAG, "Error writing SharedPreferences file");
        } else {
            Log.d(TAG, "Device information saved");
        }
    }

    /**
     * Decode the 12-bytes Base64 encoded string into standard device MAC address and 6-digit passkey
     *
     * @param deviceInfoString a 12-bytes long base64 encoded deviceInfo string
     */
    public static Device decodeDeviceInfo(String deviceInfoString) {

        if (deviceInfoString.length() != 12) {
            return null;
        }

        byte[] data = Base64.decode(deviceInfoString, Base64.DEFAULT);

        // data should be 9-bytes long
        //assert data.length == 9;

        // first
        String deviceAddress = String.format(Locale.US, "%02X:%02X:%02X:%02X:%02X:%02X",
                data[0], data[1], data[2], data[3], data[4], data[5]);
        String passKey = String.format(Locale.US, "%02X%02X%02X", data[6], data[7], data[8]);

        return new Device(deviceAddress, passKey);
    }

    private static float decodeTemperatureFromBytes(byte byte1, byte byte2) {
        return (float) (byte1 + 0.01 * byte2);
    }

    /**
     * Decode temperature from byte array
     */
    public void setTemperature(byte[] data) {
        if (data.length != 2) {
            Log.d(TAG, "data is invalid");
        } else {
            temperature = decodeTemperatureFromBytes(data[0], data[1]);
        }
    }

    public float getTemperature() {
        return temperature;
    }

    /**
     *
     */
    public void setTemperatureRange(byte[] data) {
        if (data.length != 4) {
            Log.d(TAG, "data is invalid");
        } else {
            temperatureRangeMin = decodeTemperatureFromBytes(data[0], data[1]);
            temperatureRangeMax = decodeTemperatureFromBytes(data[2], data[3]);
        }
    }

    public void setTemperatureRange(float min, float max) {
        temperatureRangeMin = min;
        temperatureRangeMax = max;
    }

    public float getTemperatureRangeMin() {
        return temperatureRangeMin;
    }

    public float getTemperatureRangeMax() {
        return temperatureRangeMax;
    }
}
