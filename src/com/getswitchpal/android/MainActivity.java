package com.getswitchpal.android;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;

/**
 * The activity that is used when a user has not connect to a device.
 */
public class MainActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set the layout
        setContentView(R.layout.activity_main);

        // hide the actionBar, later we can decide whether to disable the ActionBar globally
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

    }
}