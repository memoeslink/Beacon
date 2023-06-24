package com.app.memoeslink.beacon.activity

import android.Manifest
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.lifecycleScope
import com.app.memoeslink.beacon.AdUnitId
import com.app.memoeslink.beacon.BuildConfig
import com.app.memoeslink.beacon.Flashlight
import com.app.memoeslink.beacon.IlluminationType
import com.app.memoeslink.beacon.R
import com.app.memoeslink.beacon.Screen
import com.app.memoeslink.beacon.ScreenMode
import com.app.memoeslink.beacon.SharedPrefUtils
import com.github.evilbunny2008.androidmaterialcolorpickerdialog.ColorPicker
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

class MainActivity : CommonActivity() {
    private var rlMain: RelativeLayout? = null
    private var rlAdContainer: RelativeLayout? = null
    private var llAdContent: LinearLayout? = null
    private var llLeftSquare: LinearLayout? = null
    private var llMiddleSquare: LinearLayout? = null
    private var llRightSquare: LinearLayout? = null
    private var ivCube: ImageView? = null
    private var ivLight: ImageView? = null
    private var ivPattern: ImageView? = null
    private var ivCursor: ImageView? = null
    private var ivDismiss: ImageView? = null
    private var picker: ColorPicker? = null
    private var adView: AdView? = null
    private var adRequest: AdRequest? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var animation: ValueAnimator? = null

    private var job: Job? = null
    private var continuousJob: Job? = null
    private var adAdded: Boolean = false
    private var locked: Boolean = false
    private var milliseconds: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.DefaultTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        MobileAds.initialize(this@MainActivity)
        rlMain = findViewById(R.id.main)
        rlAdContainer = findViewById(R.id.ad_container)
        llAdContent = findViewById(R.id.ad_content)
        llLeftSquare = findViewById(R.id.left_square)
        llMiddleSquare = findViewById(R.id.middle_square)
        llRightSquare = findViewById(R.id.right_square)
        ivCube = findViewById(R.id.cube_icon)
        ivLight = findViewById(R.id.light_icon)
        ivPattern = findViewById(R.id.pattern_icon)
        ivCursor = findViewById(R.id.cursor)
        ivDismiss = findViewById(R.id.ad_dismiss)

        // Request ads
        val testDevices: MutableList<String> = ArrayList()
        testDevices.add(AdRequest.DEVICE_ID_EMULATOR)
        var requestConfiguration = RequestConfiguration.Builder().build()

        if (BuildConfig.DEBUG) requestConfiguration =
            RequestConfiguration.Builder().setTestDeviceIds(testDevices).build()
        MobileAds.setRequestConfiguration(requestConfiguration)
        adRequest = AdRequest.Builder().build()

        // Initialize components
        llLeftSquare?.setOnClickListener {
            showPanel()
            val illuminationType = IlluminationType.values()[SharedPrefUtils.getIntData(
                this@MainActivity, "pref_illuminationType"
            )].let { illuminationType ->
                if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) illuminationType.next()
                else illuminationType.next().next()
            }
            SharedPrefUtils.saveData(
                this@MainActivity, "pref_illuminationType", illuminationType.ordinal
            )
            changeIlluminationType(illuminationType)
        }

        llMiddleSquare?.setOnClickListener {
            showPanel()
            showColorPicker()
        }

        llRightSquare?.setOnClickListener {
            showPanel()
            val screenMode = ScreenMode.values()[SharedPrefUtils.getIntData(
                this@MainActivity, "pref_screenMode"
            )].next()
            SharedPrefUtils.saveData(
                this@MainActivity, "pref_screenMode", screenMode.ordinal
            )
            changeScreenMode(screenMode)
        }

        ivDismiss?.setOnClickListener {
            destroyAd()
        }
    }

    override fun onStart() {
        super.onStart()

        // Keep screen on
        layoutParams = window.attributes
        Screen.setContinuance(this@MainActivity, true)
        window.attributes = layoutParams

        if (ContextCompat.checkSelfPermission(
                this@MainActivity, Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.CAMERA
            ), 0
        )

        rlMain?.post {
            val screenColor = SharedPrefUtils.getIntData(this@MainActivity, "pref_screenColor")
            val illuminationType = IlluminationType.values()[SharedPrefUtils.getIntData(
                this@MainActivity, "pref_illuminationType"
            )]
            val screenMode = ScreenMode.values()[SharedPrefUtils.getIntData(
                this@MainActivity, "pref_screenMode"
            )]
            changeBackgroundColor(screenColor)
            changeIlluminationType(illuminationType)
            changeScreenMode(screenMode)
        }
    }

    override fun onResume() {
        super.onResume()
        showPanel()

        // Restart lights
        SharedPrefUtils.getIntData(this@MainActivity, "pref_illuminationType").let {
            IlluminationType.values()[SharedPrefUtils.getIntData(
                this@MainActivity, "pref_illuminationType"
            )]
        }.takeIf {
            it == IlluminationType.FLASH || it == IlluminationType.ALL
        }?.let {
            Flashlight.turnOn(this@MainActivity)
        }

        // Start main job
        if (continuousJob == null) {
            continuousJob = lifecycleScope.launch {
                while (true) {
                    delay(1)
                    val screenMode = ScreenMode.values()[SharedPrefUtils.getIntData(
                        this@MainActivity, "pref_screenMode"
                    )]

                    if (screenMode == ScreenMode.SOS) {
                        if (milliseconds < 6800) {
                            var screenColor: Int? = null

                            if (SOS_SEQUENCE.containsKey(milliseconds)) screenColor =
                                SOS_SEQUENCE[milliseconds]

                            if (screenColor != null) {
                                runOnUiThread {
                                    changeBackgroundColor(screenColor)
                                }
                            }
                            milliseconds++
                        } else milliseconds = 0
                    }
                }
            }
            continuousJob?.start()
        }

        // Show ads
        prepareAds(false)
    }

    override fun onPostResume() {
        super.onPostResume()

        job = lifecycleScope.launch(Dispatchers.IO) {
            delay(10000)
            runOnUiThread { hidePanel() }
            job = null
        }
        job?.start()
    }

    override fun onPause() {
        super.onPause()
        continuousJob?.cancel()
        continuousJob = null

        // Stop lights
        Flashlight.turnOff(this@MainActivity)
    }

    override fun onConfigurationChanged(configuration: Configuration) {
        super.onConfigurationChanged(configuration)
        prepareAds(true)

        picker?.takeIf { it.isShowing }?.let { showColorPicker() }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        showPanel()
        return super.onTouchEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        showPanel()

        when (keyCode) {
            KeyEvent.KEYCODE_MENU -> return super.onKeyDown(keyCode, event)
            KeyEvent.KEYCODE_BACK -> {
                finish()
                exitProcess(0)
            }
        }
        return false
    }

    override fun onUserInteraction() {
        super.onUserInteraction()

        if (job != null) {
            job?.cancel()
            job = null
        }

        job = lifecycleScope.launch(Dispatchers.IO) {
            delay(10000)
            runOnUiThread { hidePanel() }
            job = null
        }
        job?.start()
    }

    private fun changeBackgroundColor(color: Int) {
        rlMain?.setBackgroundColor(color)
        val clearColor = color or -0x1000000
        val a =
            1 - 0.299 * Color.red(clearColor) + 0.587 * Color.green(clearColor) + 0.114 * Color.blue(
                clearColor
            ) / 255
        if (a < 0.5) {
            (llLeftSquare?.background as GradientDrawable).setColor(
                ColorUtils.setAlphaComponent(
                    Color.BLACK, 26
                )
            )
            (llMiddleSquare?.background as GradientDrawable).setColor(
                ColorUtils.setAlphaComponent(
                    Color.BLACK, 26
                )
            )
            (llRightSquare?.background as GradientDrawable).setColor(
                ColorUtils.setAlphaComponent(
                    Color.BLACK, 26
                )
            )
        } else {
            (llLeftSquare?.background as GradientDrawable).setColor(
                ColorUtils.setAlphaComponent(
                    Color.WHITE, 26
                )
            )
            (llMiddleSquare?.background as GradientDrawable).setColor(
                ColorUtils.setAlphaComponent(
                    Color.WHITE, 26
                )
            )
            (llRightSquare?.background as GradientDrawable).setColor(
                ColorUtils.setAlphaComponent(
                    Color.WHITE, 26
                )
            )
        }
    }

    private fun changeIlluminationType(type: IlluminationType) {
        if (locked) return
        locked = true

        when (type) {
            IlluminationType.NONE -> {
                ivLight?.setImageResource(R.drawable.ic_turned_off)
                layoutParams?.screenBrightness = 0.0f
                window.attributes = layoutParams
                Flashlight.turnOff(this@MainActivity)
            }

            IlluminationType.SCREEN -> {
                ivLight?.setImageResource(R.drawable.ic_brightness)
                layoutParams?.screenBrightness = 1.0f
                window.attributes = layoutParams
                Flashlight.turnOff(this@MainActivity)
            }

            IlluminationType.FLASH -> {
                ivLight?.setImageResource(R.drawable.ic_mobile_phone)
                layoutParams?.screenBrightness = 0.0f
                window.attributes = layoutParams
                Flashlight.turnOn(this@MainActivity)
            }

            IlluminationType.ALL -> {
                ivLight?.setImageResource(R.drawable.ic_turned_on)
                layoutParams?.screenBrightness = 1.0f
                window.attributes = layoutParams
                Flashlight.turnOn(this@MainActivity)
            }
        }
        locked = false
    }

    private fun changeScreenMode(mode: ScreenMode) {
        when (mode) {
            ScreenMode.DEFAULT -> {
                animation?.cancel()
                animation = null
                val screenColor = SharedPrefUtils.getIntData(this@MainActivity, "pref_screenColor")
                changeBackgroundColor(screenColor)
                ivPattern?.setImageResource(R.drawable.ic_pantone)
                ivCube?.let { removeGrayFilter(it) }
                llMiddleSquare?.isClickable = true
                llMiddleSquare?.isEnabled = true
            }

            ScreenMode.SOS -> {
                ivPattern?.setImageResource(R.drawable.ic_help)
                ivCube?.let { applyGrayFilter(it) }
                llMiddleSquare?.isClickable = false
                llMiddleSquare?.isEnabled = false
            }

            ScreenMode.RAINBOW -> {
                animation = ValueAnimator.ofFloat(0.0F, 1.0F)
                animation?.duration = 2000
                animation?.addUpdateListener { animation ->
                    val hsv = floatArrayOf(360 * animation.animatedFraction, 1.0F, 1.0F)
                    val runColor = Color.HSVToColor(hsv)
                    rlMain?.setBackgroundColor(runColor)
                }
                animation?.repeatCount = Animation.INFINITE
                animation?.start()

                ivPattern?.setImageResource(R.drawable.ic_cube)
                ivCube?.let { applyGrayFilter(it) }
                llMiddleSquare?.isClickable = false
                llMiddleSquare?.isEnabled = false
            }
        }
    }

    private fun showColorPicker() {
        val color: Int = SharedPrefUtils.getIntData(this@MainActivity, "pref_screenColor")
        picker?.takeIf { it.isShowing }?.let { picker?.dismiss() }

        // Define default color for ColorPicker
        picker =
            ColorPicker(this@MainActivity, Color.red(color), Color.green(color), Color.blue(color))

        // Set listener
        picker?.setCallback { pickedColor ->
            SharedPrefUtils.saveData(this@MainActivity, "pref_screenColor", pickedColor)
            changeBackgroundColor(pickedColor)
            picker?.dismiss()
        }

        // Set listener
        picker?.show()
    }

    private fun showPanel() {
        Screen.unlockScreenOrientation(this@MainActivity)
        Screen.setContinuance(this@MainActivity, false)
        llLeftSquare?.visibility = View.VISIBLE
        llMiddleSquare?.visibility = View.VISIBLE
        llRightSquare?.visibility = View.VISIBLE
        ivCursor?.visibility = View.VISIBLE
    }

    private fun hidePanel() {
        Screen.lockScreenOrientation(this@MainActivity)
        Screen.setContinuance(this@MainActivity, true)
        llLeftSquare?.visibility = View.GONE
        llMiddleSquare?.visibility = View.GONE
        llRightSquare?.visibility = View.GONE
        ivCursor?.visibility = View.INVISIBLE
    }

    private fun applyGrayFilter(imageView: ImageView) {
        val matrix = ColorMatrix()
        matrix.setSaturation(0f)
        val filter = ColorMatrixColorFilter(matrix)
        imageView.colorFilter = filter
    }

    private fun removeGrayFilter(imageView: ImageView) {
        imageView.clearColorFilter()
        imageView.invalidate()
    }

    private fun prepareAds(restarted: Boolean) {
        if (restarted) destroyAd()

        if (!adAdded) {
            adView = AdView(this@MainActivity)
            adView?.setAdSize(AdSize.BANNER)
            adView?.adUnitId = AdUnitId.getBannerId()

            adView?.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    llAdContent?.addView(adView)
                    rlAdContainer?.visibility = View.VISIBLE
                    adAdded = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    destroyAd()
                }
            }

            try {
                adRequest?.let { adView?.loadAd(it) }
            } catch (e: Exception) {
                destroyAd()
            }
        }
    }

    private fun destroyAd() {
        if (adView != null) {
            adView?.destroy()
            rlAdContainer?.visibility = View.GONE
            llAdContent?.removeAllViews()
            adView = null
            adAdded = false
        }
    }

    companion object {
        private val SOS_SEQUENCE: Map<Int, Int> = mapOf(
            0 to Color.WHITE,
            200 to Color.BLACK,
            400 to Color.WHITE,
            600 to Color.BLACK,
            800 to Color.WHITE,
            1000 to Color.BLACK,
            1600 to Color.WHITE,
            2200 to Color.BLACK,
            2400 to Color.WHITE,
            3000 to Color.BLACK,
            3200 to Color.WHITE,
            3800 to Color.BLACK,
            4400 to Color.WHITE,
            4600 to Color.BLACK,
            4800 to Color.WHITE,
            5000 to Color.BLACK,
            5200 to Color.WHITE,
            5400 to Color.BLACK
        )
    }
}