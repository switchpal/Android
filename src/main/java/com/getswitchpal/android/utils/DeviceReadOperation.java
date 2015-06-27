package com.getswitchpal.android.utils;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import java.util.UUID;

/**
 */
public class DeviceReadOperation extends DeviceOperation {

    public DeviceReadOperation(String uuidString) {
        this.uuid = UUID.fromString(uuidString);
    }

    @Override
    public void perform(BluetoothGatt bluetoothGatt) {
        BluetoothGattCharacteristic characteristic = getCharacteristic();
        bluetoothGatt.readCharacteristic(characteristic);
    }
}
