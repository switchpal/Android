package com.getswitchpal.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import android.view.View;
import android.widget.Toast;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

import java.util.Collections;

/**
 * The activity that is used when a user has not connect to a device.
 */
public class QRScanActivity extends Activity implements ZXingScannerView.ResultHandler {
    private ZXingScannerView mScannerView;

    private final static String TAG = QRScanActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        // to ensure we got fullscreen preview
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        mScannerView = new ZXingScannerView(this);   // Programmatically initialize the scanner view
        setContentView(mScannerView);                // Set the scanner view as the content view
    }

    @Override
    public void onResume() {
        super.onResume();
        // we are only interested in QR Code
        mScannerView.setFormats(Collections.singletonList(BarcodeFormat.QR_CODE));
        mScannerView.setResultHandler(this); // Register ourselves as a handler for scan results.
        mScannerView.startCamera();          // Start camera on resume
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();           // Stop camera on pause
    }

    /**
     * Handle the QR code content
     * @param rawResult
     */
    @Override
    public void handleResult(Result rawResult) {

        String url = rawResult.getText();
        Utils.DeviceInfo deviceInfo = Utils.getDeviceInfoFromUrl(url);
        if (deviceInfo != null) {

            Toast.makeText(this,
                    "address = " + deviceInfo.getAddress() + ", passkey = " + deviceInfo.getPasskey(),
                    Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(QRScanActivity.this, DeviceActivity.class);
            intent.putExtra(DeviceActivity.EXTRAS_DEVICE_ADDRESS, deviceInfo.getAddress());
            intent.putExtra(DeviceActivity.EXTRAS_DEVICE_PASSKEY, deviceInfo.getPasskey());
            QRScanActivity.this.startActivity(intent);
        } else {
            Toast.makeText(this, "Cannot parse device info from the QR code: " + rawResult.getText(), Toast.LENGTH_LONG)
                    .show();
            mScannerView.startCamera();
        }
    }
}
