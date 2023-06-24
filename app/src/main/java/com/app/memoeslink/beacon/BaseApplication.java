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
        if (!SharedPrefUtils.has(getApplicationContext(), "pref_screenColor"))
            SharedPrefUtils.saveData(getApplicationContext(), "pref_screenColor", Color.WHITE);

        if (!SharedPrefUtils.has(getApplicationContext(), "pref_illuminationType"))
            SharedPrefUtils.saveData(getApplicationContext(), "pref_illuminationType", 0);

        if (!SharedPrefUtils.has(getApplicationContext(), "pref_screenMode"))
            SharedPrefUtils.saveData(getApplicationContext(), "pref_screenMode", 0);
    }
}
