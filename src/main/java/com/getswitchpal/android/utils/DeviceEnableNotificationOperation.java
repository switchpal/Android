package com.getswitchpal.android.utils;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

import java.util.UUID;

/**
 */
public class DeviceEnableNotificationOperation extends DeviceOperation {
    private static final UUID UUID_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public DeviceEnableNotificationOperation(String uuidString) {
        this.uuid = UUID.fromString(uuidString);
    }

    @Override
    public void perform(BluetoothGatt bluetoothGatt) {
        BluetoothGattCharacteristic characteristic = getCharacteristic();
        bluetoothGatt.setCharacteristicNotification(characteristic, true);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID_CONFIG);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        bluetoothGatt.writeDescriptor(descriptor);
    }
}
