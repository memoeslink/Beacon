package com.app.memoeslink.beacon;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.pes.androidmaterialcolorpickerdialog.ColorPicker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends CommonActivity {
    private static final HashMap<Integer, Integer> SOS_SEQUENCE = new HashMap<>() {{
        put(0, Color.WHITE);
        put(200, Color.BLACK);
        put(400, Color.WHITE);
        put(600, Color.BLACK);
        put(800, Color.WHITE);
        put(1000, Color.BLACK);
        put(1600, Color.WHITE);
        put(2200, Color.BLACK);
        put(2400, Color.WHITE);
        put(3000, Color.BLACK);
        put(3200, Color.WHITE);
        put(3800, Color.BLACK);
        put(4400, Color.WHITE);
        put(4600, Color.BLACK);
        put(4800, Color.WHITE);
        put(5000, Color.BLACK);
        put(5200, Color.WHITE);
        put(5400, Color.BLACK);
    }};
    private final int[] milliseconds = {0, 0};
    private RelativeLayout rlMain;
    private RelativeLayout rlAdContainer;
    private LinearLayout llAdContent;
    private LinearLayout llLeftSquare;
    private LinearLayout llMiddleSquare;
    private LinearLayout llRightSquare;
    private ImageView ivCube;
    private ImageView ivLight;
    private ImageView ivPattern;
    private ImageView ivCursor;
    private ImageView ivDismiss;
    private ColorPicker picker;
    private boolean running = true;
    private boolean adAdded = false;
    private boolean busy = false;
    private boolean defined = false;
    private boolean locked = false;
    private Illumination type = Illumination.NONE;
    private Mode mode = Mode.DEFAULT;
    private Integer colorInteger = null;
    private Thread thread = null;
    private AdView adView;
    private AdRequest adRequest;
    private AlertDialog dialog;
    private WindowManager.LayoutParams layoutParams;
    private SharedPreferences preferences;

    private static void onInitializationComplete(InitializationStatus initializationStatus) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.DefaultTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MobileAds.initialize(MainActivity.this, MainActivity::onInitializationComplete);
        preferences = getSharedPreferences("preferences", Context.MODE_PRIVATE);
        rlMain = findViewById(R.id.main);
        rlAdContainer = findViewById(R.id.ad_container);
        llAdContent = findViewById(R.id.ad_content);
        llLeftSquare = findViewById(R.id.left_square);
        llMiddleSquare = findViewById(R.id.middle_square);
        llRightSquare = findViewById(R.id.right_square);
        ivCube = findViewById(R.id.cube_icon);
        ivLight = findViewById(R.id.light_icon);
        ivPattern = findViewById(R.id.pattern_icon);
        ivCursor = findViewById(R.id.cursor);
        ivDismiss = findViewById(R.id.ad_dismiss);
        setShapeColor(preferences.getInt("color", Color.WHITE)); //Modify shape color

        // Initialize preferences
        type = Illumination.values()[preferences.getInt("type", Illumination.NONE.ordinal())];
        mode = Mode.values()[preferences.getInt("mode", Mode.DEFAULT.ordinal())];

        // Request ads
        List<String> testDevices = new ArrayList<>();
        testDevices.add(AdRequest.DEVICE_ID_EMULATOR);
        RequestConfiguration requestConfiguration = new RequestConfiguration.Builder().build();

        if (BuildConfig.DEBUG)
            requestConfiguration = new RequestConfiguration.Builder().setTestDeviceIds(testDevices).build();
        MobileAds.setRequestConfiguration(requestConfiguration);
        adRequest = new AdRequest.Builder().build();

        // Keep screen on
        layoutParams = getWindow().getAttributes();
        Screen.setContinuance(MainActivity.this, true);
        getWindow().setAttributes(layoutParams);

        // Define dialog
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View v = layoutInflater.inflate(R.layout.alert_about, null);

        TextView textview = v.findViewById(R.id.alert_text);
        textview.setText(fromHtml(getString(R.string.about)));
        textview.setMovementMethod(LinkMovementMethod.getInstance());

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.dialog_title);
        builder.setView(v);
        builder.setNeutralButton("OK", null);
        dialog = builder.create();

        // Set listeners
        llLeftSquare.setOnClickListener(view -> {
            showViews();

            if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
                type = type.next();
            else
                type = type == Illumination.NONE ? Illumination.SCREEN : Illumination.NONE;
            changeIlluminationType(type);
        });

        llMiddleSquare.setOnClickListener(view -> {
            showViews();
            showPicker();
        });

        llRightSquare.setOnClickListener(view -> {
            showViews();
            mode = mode.next();
            changeScreenMode(mode);
        });

        ivDismiss.setOnClickListener(view -> destroyAd());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, 0, Menu.NONE, R.string.menu_about);
        menu.add(Menu.NONE, 1, Menu.NONE, R.string.action_exit);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                dialog.show();
                return true;
            case 1:
                finish();
                System.exit(0);
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);

        rlMain.post(() -> {
            int color = preferences.getInt("color", Color.WHITE);
            rlMain.setBackgroundColor(color);
            setShapeColor(color);
            changeIlluminationType(type);
            changeScreenMode(mode);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        showViews();

        // Restart lights
        if (type == Illumination.FLASH || type == Illumination.ALL)
            FlashLight.turnOn(MainActivity.this);

        // Show ads
        prepareAds(false);

        if (thread == null) {
            thread = new Thread(() -> {
                while (running) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (!busy) {
                        if (milliseconds[0] < 5000) milliseconds[0]++;
                        else {
                            runOnUiThread(() -> {
                                busy = true;
                                hideViews();
                                busy = false;
                            });
                        }

                        if (mode == Mode.SOS) {
                            if (milliseconds[1] < 6800) {
                                defined = true;

                                if (SOS_SEQUENCE.containsKey(milliseconds[1]))
                                    colorInteger = SOS_SEQUENCE.get(milliseconds[1]);
                                else defined = false;

                                if (defined && colorInteger != null) {
                                    runOnUiThread(() -> {
                                        busy = true;
                                        rlMain.setBackgroundColor(colorInteger);
                                        setShapeColor(colorInteger);
                                        busy = false;
                                    });
                                }
                                milliseconds[1]++;
                            } else milliseconds[1] = 0;
                        } else {
                            milliseconds[1] = 0;
                            final int color = preferences.getInt("color", Color.WHITE);

                            runOnUiThread(() -> {
                                busy = true;
                                rlMain.setBackgroundColor(color);
                                setShapeColor(color);
                                busy = false;
                            });
                        }
                    }
                }
            });
            running = true;
            thread.start();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (thread != null && thread.isAlive()) {
            running = false;
            milliseconds[0] = 0;
            thread.interrupt();
            thread = null;
        }

        // Stop lights
        FlashLight.turnOff(MainActivity.this);
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        prepareAds(true);

        if (picker != null && picker.isShowing()) showPicker();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        showViews();
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        showViews();

        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                return super.onKeyDown(keyCode, event);
            case KeyEvent.KEYCODE_BACK:
                finish();
                System.exit(0);
                return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
    }

    public void changeIlluminationType(Illumination type) {
        if (locked) return;
        locked = true;

        switch (type) {
            case NONE -> {
                ivLight.setImageResource(R.drawable.ic_turned_off);
                layoutParams.screenBrightness = 0.0f;
                getWindow().setAttributes(layoutParams);
                FlashLight.turnOff(MainActivity.this);
            }
            case SCREEN -> {
                ivLight.setImageResource(R.drawable.ic_brightness);
                layoutParams.screenBrightness = 1.0f;
                getWindow().setAttributes(layoutParams);
                FlashLight.turnOff(MainActivity.this);
            }
            case FLASH -> {
                ivLight.setImageResource(R.drawable.ic_mobile_phone);
                layoutParams.screenBrightness = 0.0f;
                getWindow().setAttributes(layoutParams);
                FlashLight.turnOn(MainActivity.this);
            }
            case ALL -> {
                ivLight.setImageResource(R.drawable.ic_turned_on);
                layoutParams.screenBrightness = 1.0f;
                getWindow().setAttributes(layoutParams);
                FlashLight.turnOn(MainActivity.this);
            }
            default -> {
            }
        }
        preferences.edit().putInt("type", type.ordinal()).apply();
        locked = false;
    }

    public void changeScreenMode(Mode mode) {
        switch (mode) {
            case DEFAULT -> {
                ivPattern.setImageResource(R.drawable.ic_pantone);
                removeGrayFilter(ivCube);
                llMiddleSquare.setClickable(true);
                llMiddleSquare.setEnabled(true);
            }
            case SOS -> {
                ivPattern.setImageResource(R.drawable.ic_help);
                setGrayFilter(ivCube);
                llMiddleSquare.setClickable(false);
                llMiddleSquare.setEnabled(false);
            }
            default -> {
            }
        }
        preferences.edit().putInt("mode", mode.ordinal()).apply();
    }

    public void showPicker() {
        int color = preferences.getInt("color", Color.WHITE);

        if (picker != null && picker.isShowing()) picker.dismiss();
        picker = new ColorPicker(MainActivity.this, Color.red(color), Color.green(color), Color.blue(color)); // Define default color for ColorPicker

        // Set listener
        picker.setCallback(pickedColor -> {
            preferences.edit().putInt("color", pickedColor).apply();
            rlMain.setBackgroundColor(pickedColor);
            setShapeColor(pickedColor);
        });
        picker.show();
    }

    public void showViews() {
        Screen.unlockScreenOrientation(MainActivity.this);
        Screen.setContinuance(MainActivity.this, false);
        llLeftSquare.setVisibility(View.VISIBLE);
        llMiddleSquare.setVisibility(View.VISIBLE);
        llRightSquare.setVisibility(View.VISIBLE);
        ivCursor.setVisibility(View.VISIBLE);
        milliseconds[0] = 0;
    }

    public void hideViews() {
        Screen.lockScreenOrientation(MainActivity.this);
        Screen.setContinuance(MainActivity.this, true);
        llLeftSquare.setVisibility(View.GONE);
        llMiddleSquare.setVisibility(View.GONE);
        llRightSquare.setVisibility(View.GONE);
        ivCursor.setVisibility(View.INVISIBLE);
        milliseconds[0] = 0;
    }

    public void setShapeColor(int color) {
        int clearColor = color | 0xFF000000;
        double a = 1 - (0.299 * Color.red(clearColor) + 0.587 * Color.green(clearColor) + 0.114 * Color.blue(clearColor)) / 255;

        if (a < 0.5) {
            ((GradientDrawable) llLeftSquare.getBackground()).setColor(ColorUtils.setAlphaComponent(Color.BLACK, 26));
            ((GradientDrawable) llMiddleSquare.getBackground()).setColor(ColorUtils.setAlphaComponent(Color.BLACK, 26));
            ((GradientDrawable) llRightSquare.getBackground()).setColor(ColorUtils.setAlphaComponent(Color.BLACK, 26));
        } else {
            ((GradientDrawable) llLeftSquare.getBackground()).setColor(ColorUtils.setAlphaComponent(Color.WHITE, 26));
            ((GradientDrawable) llMiddleSquare.getBackground()).setColor(ColorUtils.setAlphaComponent(Color.WHITE, 26));
            ((GradientDrawable) llRightSquare.getBackground()).setColor(ColorUtils.setAlphaComponent(Color.WHITE, 26));
        }
    }

    private void prepareAds(boolean restarted) {
        if (restarted) destroyAd();

        if (!adAdded) {
            adView = new AdView(MainActivity.this);
            adView.setAdSize(AdSize.BANNER);
            adView.setAdUnitId(AdUnitId.getBannerId());

            // Set listener
            adView.setAdListener(new AdListener() {
                public void onAdLoaded() {
                    llAdContent.addView(adView);
                    rlAdContainer.setVisibility(View.VISIBLE);
                    adAdded = true;
                }

                public void onAdFailedToLoad(LoadAdError error) {
                    destroyAd();
                }
            });

            try {
                adView.loadAd(adRequest);
            } catch (Exception e) {
                destroyAd();
            }
        }
    }

    private void destroyAd() {
        if (adView != null) {
            adView.destroy();
            adView.destroyDrawingCache();
            rlAdContainer.setVisibility(View.GONE);
            llAdContent.removeAllViews();
            adView = null;
            adAdded = false;
        }
    }

    private Spanned fromHtml(String html) {
        return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
    }

    private void setGrayFilter(ImageView imageView) {
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
        imageView.setColorFilter(filter);
    }

    private void removeGrayFilter(ImageView imageView) {
        imageView.clearColorFilter();
        imageView.invalidate();
    }
}
