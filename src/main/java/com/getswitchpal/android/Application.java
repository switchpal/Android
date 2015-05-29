package com.getswitchpal.android;

import com.parse.Parse;
import com.parse.ParseAnalytics;

import java.util.HashMap;
import java.util.Map;

/**
 */
public class Application extends android.app.Application {

    public Application() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Parse.initialize(this, "30HP5fkESZztpIcwLsWam8r4vldYvFhssYShoRKu", "C90NNvD1dSHFolPdxbFMWYuEtsDbWhvh9HauSOJv");
    }
}
