package com.getswitchpal.android;

import android.net.Uri;
import android.util.Base64;
import android.util.Pair;

import java.util.Locale;

/**
 *
 */
public class Utils {

    public static class DeviceInfo {
        private String address;
        private String passkey;

        public DeviceInfo(String address, String passkey) {
            this.address = address;
            this.passkey = passkey;
        }

        public String getAddress() {
            return address;
        }

        public String getPasskey() {
            return passkey;
        }
    }

    /**
     * Decode http://getswitchpal.com/app/?device=0123456789AB,
     *
     * @return DeviceInfo
     */
    public static DeviceInfo getDeviceInfoFromUrl(String url) {

        if (url == null || !url.contains("getswitchpal.com")) {
            return null;
        }

        Uri uri = Uri.parse(url);
        String deviceInfoString = uri.getQueryParameter("device");
        return decodeDeviceInfo(deviceInfoString);
    }

    /**
     * Decode the 12-bytes Base64 encoded string into standard device MAC address and 6-digit passkey
     * @param deviceInfoString a 12-bytes long base64 encoded deviceInfo string
     */
    public static DeviceInfo decodeDeviceInfo(String deviceInfoString) {

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

        return new DeviceInfo(deviceAddress, passKey);
    }
}
