package com.getswitchpal.android;

import com.getswitchpal.android.utils.Device;
import junit.framework.TestCase;

/**
 */
public class UtilsTest extends TestCase {

    public void testGetDeviceInfoFromUrl() {
        Device device = Device.getDeviceInfoFromQRCode("http://getswitchpal.com/app/?device=ZNJW59nKFChX");
        assertEquals(device.getAddress(), "64:D2:56:E7:D9:CA");
        assertEquals(device.getPasskey(), "142857");
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
        Device device = Device.decodeDeviceInfo("ZNJW59nKFChX");
        assertEquals(device.getAddress(), "64:D2:56:E7:D9:CA");
        assertEquals(device.getPasskey(), "142857");
    }
}