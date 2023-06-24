package com.app.memoeslink.beacon;

import android.app.Application;
import android.graphics.Color;

import androidx.appcompat.app.AppCompatDelegate;

public class BaseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        // Initialize SharedPreferences
        SharedPrefUtils.saveData(getApplicationContext(), "pref_screenColor", Color.WHITE);
        SharedPrefUtils.saveData(getApplicationContext(), "pref_illuminationType", 0);
        SharedPrefUtils.saveData(getApplicationContext(), "pref_screenMode", 0);
    }
}
