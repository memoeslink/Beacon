package com.app.memoeslink.beacon;

import android.app.Application;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import androidx.appcompat.app.AppCompatDelegate;

import java.util.Locale;

/**
 * Created by Memoeslink on 17/10/2017.
 */

public class ApplicationContext extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        setLanguage(); //Change app language according to device
    }

    @SuppressWarnings("deprecation")
    private void setLanguage() {
        String language = Locale.getDefault().getLanguage();

        if (!Locale.getDefault().getLanguage().equals("en") && !Locale.getDefault().getLanguage().equals("es"))
            language = "en";
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Resources res = this.getResources();
        Configuration config;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config = res.getConfiguration();
            config.setLocale(locale);
            LocaleList localeList = new LocaleList(locale);
            LocaleList.setDefault(localeList);
            config.setLocales(localeList);
        } else {
            config = res.getConfiguration();
            config.setLocale(locale);
            res.updateConfiguration(config, res.getDisplayMetrics());
        }
    }
}
