package com.getswitchpal.android.utils;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

import static com.getswitchpal.android.utils.DeviceOperation.*;

/**
 * Bluetooth operations are synchronous. We must confirm the previous operation is complete before sending another one.
 */
public class DeviceOperationQueue {

    private static final String TAG = DeviceOperationQueue.class.getSimpleName();

    private BluetoothGatt mBluetoothGatt;
    private Queue<DeviceOperation> queue = new LinkedList<>();
    private DeviceOperation current = null;

    // the constructor
    public DeviceOperationQueue(BluetoothGatt bluetoothGatt) {
        this.mBluetoothGatt = bluetoothGatt;
    }

    public boolean isEmpty() {
        return this.queue.isEmpty();
    }

    /**
     * Add a new operation to the queue
     */
    public void add(DeviceOperation operation) {
        operation.setQueue(this);
        this.queue.add(operation);
        if (current == null) {
            executeNextIfAny();
        }
    }

    /**
     * Check response from onCharacteristicRead, onCharacteristicWrite callback
     */
    public void checkCharacteristicResponse(BluetoothGattCharacteristic characteristic, Type type, int status) {
        if (type == Type.NOTIFY) {
            return;
        }

        if (current == null) {
            Log.e(TAG, "current is null");
        }

        switch (type) {
            case READ:
                if (current instanceof DeviceReadOperation &&
                        current.getUuid().toString().equals(characteristic.getUuid().toString())) {
                    markCurrentDone();
                    executeNextIfAny();
                }
                return;
            case WRITE:
                if (current instanceof DeviceWriteOperation &&
                        current.getUuid().toString().equals(characteristic.getUuid().toString())) {
                    markCurrentDone();
                    executeNextIfAny();
                }
                return;
        }

        Log.e(TAG, "Current CharacteristicResponse is not what we expected");
    }

    /**
     * We would receive onDescriptorWrite callback if we issue a DeviceEnableNotificationOperation
     */
    public void checkDescriptorResponse(BluetoothGattDescriptor descriptor, int status) {
        if (current == null) {
            Log.d(TAG, "current is null");
        }

        if (current instanceof DeviceEnableNotificationOperation) {
            if (current.getUuid().toString().equals(descriptor.getCharacteristic().getUuid().toString())) {
                // the current operation is successfully completed
                markCurrentDone();
                executeNextIfAny();
            } else {
                Log.e(TAG, "the current operation is not the one returns");
            }
        } else {
            Log.e(TAG, "the current operation is not DeviceEnableNotificationOperation");
        }

    }

    /**
     *
     */
    public void markCurrentDone() {
        current = null;
        queue.poll();
    }

    /**
     * execute the next one if there is any, and the current operation is null
     */
    public void executeNextIfAny() {
        if (current != null) {
            Log.e(TAG, "current operation is in progress");
            return;
        }
        current = this.queue.peek();
        if (current != null) {
            current.perform(mBluetoothGatt);
        }
    }

    public BluetoothGattService getService() {
        return mBluetoothGatt.getService(UUID.fromString(Device.UUID_SERVICE));
    }

    public abstract class QueueCallback {
        public abstract void onOperationInProgress();
        public abstract void onQueueCleared();
    }
}

