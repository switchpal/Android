package com.getswitchpal.android.utils;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import java.util.UUID;

/**
 *
 */
public abstract class DeviceOperation {

    public enum Type {
        READ, WRITE, NOTIFY
    }

    protected UUID uuid;
    private DeviceOperationQueue queue;

    public UUID getUuid() {
        return uuid;
    }

    public void setQueue(DeviceOperationQueue queue) {
        this.queue = queue;
    }

    public BluetoothGattCharacteristic getCharacteristic() {
        if (this.queue == null) {
            return null;
        }

        return queue.getService().getCharacteristic(uuid);
    }


    public abstract void perform(BluetoothGatt bluetoothGatt);
}
