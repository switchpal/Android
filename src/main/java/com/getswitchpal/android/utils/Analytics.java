package com.getswitchpal.android.utils;

import com.parse.ParseAnalytics;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Currently two kinds of events are defined:
 *
 * - User's interaction in the app
 * - Device's status
 */
public class Analytics {

    public static final String EVENT_APP = "App";
    public static final String EVENT_DEVICE = "Device";

    /**
     * Two kinds of dimensions:
     *
     * - {"setControlMode": "auto"}
     * - {"setControlMode": "manual"}
     */
    public static void trackSetControlMode(Device.ControlMode mode) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put("setControlMode", controlModeString(mode));
        ParseAnalytics.trackEventInBackground(EVENT_APP, dimensions);
    }

    /**
     * Two kinds of dimensions:
     *
     * - {"ControlMode": "auto"}
     * - {"ControlMode": "manual"}
     */
    public static void trackControlMode(Device.ControlMode mode) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put("ControlMode", controlModeString(mode));
        ParseAnalytics.trackEventInBackground(EVENT_DEVICE, dimensions);
    }

    private static String controlModeString(Device.ControlMode mode) {
        String modeString;
        if (mode == Device.ControlMode.AUTO) {
            modeString = "auto";
        } else if (mode == Device.ControlMode.MANUAL) {
            modeString = "manual";
        } else {
            modeString = "unknown";
        }
        return modeString;
    }

    /**
     *
     * Two kinds of dimensions:
     *
     * - {"setSwitchState": "on"}
     * - {"setSwitchState": "off"}
     */
    public static void trackSetSwitchState(Device.SwitchState state) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put("setSwitchState", switchStateString(state));
        ParseAnalytics.trackEventInBackground(EVENT_APP, dimensions);
    }

    /**
     * Two kinds of dimensions:
     *
     * - {"SwitchState": "on"}
     * - {"SwitchState": "off"}
     */
    public static void trackSwitchState(Device.SwitchState state) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put("SwitchState", switchStateString(state));
        ParseAnalytics.trackEventInBackground(EVENT_DEVICE, dimensions);
    }

    private static String switchStateString(Device.SwitchState state) {
        String stateString;
        if (state == Device.SwitchState.ON) {
            stateString = "on";
        } else if (state == Device.SwitchState.OFF) {
            stateString = "off";
        } else {
            stateString = "unknown";
        }
        return stateString;
    }

    /**
     * {"setTemperatureRangeMin": "26.00", "setTemperatureRangeMax": "28.00"}
     */
    public static void trackSetTemperatureRange(float min, float max) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put("setTemperatureRangeMin", String.format("%.2f", min));
        dimensions.put("setTemperatureRangeMax", String.format("%.2f", max));
        ParseAnalytics.trackEventInBackground(EVENT_APP, dimensions);
    }

    /**
     * {"TemperatureRangeMin": "26.00", "TemperatureRangeMax": "28.00"}
     */
    public static void trackTemperatureRange(float min, float max) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put("TemperatureRangeMin", String.format("%.2f", min));
        dimensions.put("TemperatureRangeMax", String.format("%.2f", max));
        ParseAnalytics.trackEventInBackground(EVENT_DEVICE, dimensions);
    }

    /**
     * {"Temperature": "27.12"}
     */
    public static void trackTemperature(float temperature) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put("Temperature", String.format("%.2f", temperature));
        ParseAnalytics.trackEventInBackground(EVENT_DEVICE, dimensions);
    }


    public static void trackClick(String element) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put("Click", element);
        ParseAnalytics.trackEventInBackground(EVENT_APP, dimensions);
    }

}

