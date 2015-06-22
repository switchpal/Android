package com.getswitchpal.android.activities;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import com.getswitchpal.android.utils.Device;
import com.getswitchpal.android.R;
import com.parse.ParseAnalytics;

/**
 * The activity that is used when a user has not connect to a device.
 */
public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set the layout
        setContentView(R.layout.activity_main);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        Device device = Device.getDeviceFromSharedPreferences(sharedPref);
        if (device != null) {
            Intent intent = new Intent(MainActivity.this, DeviceActivity.class);
            intent.putExtra(DeviceActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
            intent.putExtra(DeviceActivity.EXTRAS_DEVICE_PASSKEY, device.getPasskey());

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            MainActivity.this.startActivity(intent);
            finish();
            return;
        }

        // hide the actionBar, later we can decide whether to disable the ActionBar globally
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        // get hold on the scan button
        final ImageView scanButton = (ImageView) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, QRScanActivity.class);
                intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
                MainActivity.this.startActivity(intent);
            }
        });

        // TODO: remove this after debugging
        final Button debugButton = (Button) findViewById(R.id.text_debug);
        debugButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DeviceActivity.class);
                intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
                MainActivity.this.startActivity(intent);
            }
        });

        // track app open
        ParseAnalytics.trackAppOpenedInBackground(getIntent());
    }
}