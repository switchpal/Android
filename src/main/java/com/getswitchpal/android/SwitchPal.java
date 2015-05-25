package com.getswitchpal.android;

/**
 * UUIDs defined by SwitchPal
 */
public class SwitchPal {
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


}
