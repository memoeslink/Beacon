package com.app.memoeslink.beacon.activity

import android.os.Bundle
import android.text.Html
import android.view.View
import com.app.memoeslink.beacon.BuildConfig
import com.app.memoeslink.beacon.R
import mehdi.sakout.aboutpage.AboutPage
import mehdi.sakout.aboutpage.Element
import java.time.Year

class AboutActivity : CommonActivity() {
    private var vAbout: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.DefaultTheme)
        super.onCreate(savedInstanceState)

        // Define 'About' page
        val backElement = Element().setTitle("⟵")
            .setOnClickListener { finish() }
        val description = Html.fromHtml(
            getString(
                R.string.about,
                getString(R.string.app_name),
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
                Year.now().value
            ),
            Html.FROM_HTML_MODE_LEGACY
        )
        vAbout = AboutPage(this@AboutActivity)
            .isRTL(false)
            .addPlayStore("com.app.memoeslink.beacon")
            .addWebsite("https://www.linkedin.com/in/guillermo-almaguer/")
            .addGitHub("memoeslink")
            .addEmail("memocad@gmail.com")
            .addItem(backElement)
            .setImage(R.drawable.ic_cube)
            .setDescription(description)
            .create()
        setContentView(vAbout)
    }
}