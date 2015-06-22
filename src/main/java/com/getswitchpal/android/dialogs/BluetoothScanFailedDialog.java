package com.getswitchpal.android.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import com.getswitchpal.android.activities.QRScanActivity;

/**
 * When the Bluetooth scanning finds no device in a specific time
 */
public class BluetoothScanFailedDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle("Device Not Found")
                .setMessage("We didn't found your device by Bluetooth. Please check if you have turned the device on.")
                .setPositiveButton("Scan Device Again", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                    }
                })
                .setNegativeButton("Scan the QR Code", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(context, QRScanActivity.class);
                        startActivity(intent);
                        dismiss();
                    }
                });

        return builder.create();
    }
}
