package com.app.memoeslink.beacon;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Created by Memoeslink on 17/10/2017.
 */

public class BaseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }
}
