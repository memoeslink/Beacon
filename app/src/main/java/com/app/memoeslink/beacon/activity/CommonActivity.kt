package com.app.memoeslink.beacon.activity

import android.content.Context
import android.content.ContextWrapper
import android.os.LocaleList
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

open class CommonActivity : AppCompatActivity() {

    override fun attachBaseContext(context: Context) {
        var currentContext = context
        currentContext = wrap(currentContext)
        super.attachBaseContext(currentContext)
    }

    private fun wrap(context: Context): ContextWrapper {
        var currentContext = context
        var language = Locale.getDefault().language

        if (Locale.getDefault().language != "en" && Locale.getDefault().language != "es") language =
            "en"
        val locale = Locale(language)
        Locale.setDefault(locale)
        val res = currentContext.resources
        val configuration = res.configuration
        configuration.setLocale(locale)
        val localeList = LocaleList(locale)
        LocaleList.setDefault(localeList)
        configuration.setLocales(localeList)
        currentContext = currentContext.createConfigurationContext(configuration)
        return ContextWrapper(currentContext)
    }
}