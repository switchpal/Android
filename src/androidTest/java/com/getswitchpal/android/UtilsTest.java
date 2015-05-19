package com.getswitchpal.android;

import android.util.Pair;
import junit.framework.TestCase;

/**
 */
public class UtilsTest extends TestCase {

    public void testGetDeviceInfoFromUrl() {
        Utils.DeviceInfo deviceInfo = Utils.getDeviceInfoFromUrl("http://getswitchpal.com/app/?device=ZNJW59nKFChX");
        assertEquals(deviceInfo.getAddress(), "64:D2:56:E7:D9:CA");
        assertEquals(deviceInfo.getPasskey(), "142857");
    }

    /**
     *
     * MAC address: 64:D2:56:E7:D9:CA, [0x64, 0xD2, 0x56, 0xE7, 0xD9, 0xCA]
     * Passkey: 142857, [0x14, 0x28, 0x57]
     * the concatenated 9-byte array converted to base64(using python):
     *   base64.b64encode(bytearray([0x64, 0xD2, 0x56, 0xE7, 0xD9, 0xCA, 0x14, 0x28, 0x57]))
     *   is `ZNJW59nKFChX`
     */
    public void testDecodeDeviceInfo() {
        Utils.DeviceInfo deviceInfo = Utils.decodeDeviceInfo("ZNJW59nKFChX");
        assertEquals(deviceInfo.getAddress(), "64:D2:56:E7:D9:CA");
        assertEquals(deviceInfo.getPasskey(), "142857");
    }
}