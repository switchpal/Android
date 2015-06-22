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
 * When a requested Bluetooth operation, such as set SwitchState on, fails, show the dialog
 */
public class OperationFailedDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setMessage("The requested operation fails, please check the device and try again")
                .setPositiveButton("Try Again", new DialogInterface.OnClickListener() {
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
