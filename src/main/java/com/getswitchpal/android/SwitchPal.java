package com.getswitchpal.android;

/**
 * UUIDs defined by SwitchPal
 */
public class SwitchPal {
    public static final String UUID_SERVICE = "0000fff0-0000-1000-8000-00805f9b34fb";

    // the current temperature, read only
    public static final String UUID_CHARACTERISTIC_TEMPERATURE_CURRENT = "0000fff1-0000-1000-8000-00805f9b34fb";
    // minimum temperature, read and write
    public static final String UUID_CHARACTERISTIC_TEMPERATURE_MIN = "00000000-0000-0000-0011-000000000000";
    // maximum temperature, read and write
    public static final String UUID_CHARACTERISTIC_TEMPERATURE_MAX = "00000000-0000-0000-0012-000000000000";

    // the switch state, read and write
    public static final String UUID_CHARACTERISTIC_STATE = "00000000-0000-0000-0020-000000000000";
    // the control mode, read and write
    public static final String UUID_CHARACTERISTIC_MODE = "00000000-0000-0000-0030-000000000000";

}
