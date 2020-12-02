package com.app.memoeslink.beacon;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.LocaleList;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class CommonActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void attachBaseContext(Context context) {
        context = wrap(context);
        super.attachBaseContext(context);
    }

    public ContextWrapper wrap(Context context) {
        String language = Locale.getDefault().getLanguage();

        if (!Locale.getDefault().getLanguage().equals("en") && !Locale.getDefault().getLanguage().equals("es"))
            language = "en";
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Resources res = context.getResources();
        Configuration configuration = res.getConfiguration();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale);
            LocaleList localeList = new LocaleList(locale);
            LocaleList.setDefault(localeList);
            configuration.setLocales(localeList);
            context = context.createConfigurationContext(configuration);
        } else {
            configuration.setLocale(locale);
            context = context.createConfigurationContext(configuration);
        }
        return new ContextWrapper(context);
    }
}
