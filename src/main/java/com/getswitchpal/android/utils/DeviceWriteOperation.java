package com.getswitchpal.android.utils;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import java.util.UUID;

/**
 */
public class DeviceWriteOperation extends DeviceOperation {
    private byte[] data;

    public DeviceWriteOperation(String uuidString, byte[] data) {
        this.uuid = UUID.fromString(uuidString);
        this.data = data;
    }

    @Override
    public void perform(BluetoothGatt bluetoothGatt) {
        BluetoothGattCharacteristic characteristic = getCharacteristic();
        characteristic.setValue(data);
        bluetoothGatt.writeCharacteristic(characteristic);
    }
}
