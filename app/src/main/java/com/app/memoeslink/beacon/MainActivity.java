package com.app.memoeslink.beacon;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
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
    private static final HashMap<Integer, Integer> SOS_SEQUENCE = new HashMap<Integer, Integer>() {{
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
    private RelativeLayout layout;
    private RelativeLayout adContainer;
    private LinearLayout leftSquare;
    private LinearLayout middleSquare;
    private LinearLayout rightSquare;
    private ImageView cube;
    private ImageView light;
    private ImageView pattern;
    private ImageView cursor;
    private ColorPicker picker;
    private boolean flashlightEnabled = false;
    private boolean running = true;
    private boolean adAdded = false;
    private boolean permissionGranted = false;
    private boolean busy = false;
    private boolean defined = false;
    private boolean illuminating = false;
    private boolean locked = false;
    private int[] milliseconds = {0, 0};
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
        layout = findViewById(R.id.main);
        adContainer = findViewById(R.id.ad_container);
        leftSquare = findViewById(R.id.left_square);
        middleSquare = findViewById(R.id.middle_square);
        rightSquare = findViewById(R.id.right_square);
        cube = findViewById(R.id.cube_icon);
        light = findViewById(R.id.light_icon);
        pattern = findViewById(R.id.pattern_icon);
        cursor = findViewById(R.id.cursor);
        setShapeColor(getColor()); //Modify shape color

        //Initialize preferences
        type = Illumination.values()[preferences.getInt("type", Illumination.NONE.ordinal())];
        mode = Mode.values()[preferences.getInt("mode", Mode.DEFAULT.ordinal())];

        //Request ads
        List<String> testDevices = new ArrayList<>();
        testDevices.add(AdRequest.DEVICE_ID_EMULATOR);
        RequestConfiguration requestConfiguration = new RequestConfiguration.Builder().build();

        if (BuildConfig.DEBUG)
            requestConfiguration = new RequestConfiguration.Builder().setTestDeviceIds(testDevices).build();
        MobileAds.setRequestConfiguration(requestConfiguration);
        adRequest = new AdRequest.Builder().build();

        //Keep screen on
        layoutParams = getWindow().getAttributes();
        layoutParams.flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        getWindow().setAttributes(layoutParams);

        //Define dialog
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

        //Set listeners
        leftSquare.setOnClickListener(view -> {
            showViews();

            if (flashlightEnabled && permissionGranted) {
                type = type.next();
                startType();
            } else {
                if (type == Illumination.NONE) type = type.next();
                else type = Illumination.NONE;
            }
        });

        middleSquare.setOnClickListener(view -> {
            showViews();
            showPicker();
        });

        rightSquare.setOnClickListener(view -> {
            showViews();
            mode = mode.next();
            startMode();
        });

        SensorEventListener listener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };
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
        flashlightEnabled = hasFlash();
        System.out.println("Has flashlight: " + flashlightEnabled);

        layout.post(() -> {
            int color = getColor();
            layout.setBackgroundColor(color);
            setShapeColor(color);
            startType();
            startMode();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        showViews();

        //Restart lights
        if (!illuminating && (type == Illumination.FLASH || type == Illumination.ALL))
            turnOnLights();

        //Show ads
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
                                        layout.setBackgroundColor(colorInteger);
                                        setShapeColor(colorInteger);
                                        busy = false;
                                    });
                                }
                                milliseconds[1]++;
                            } else milliseconds[1] = 0;
                        } else {
                            milliseconds[1] = 0;
                            final int color = getColor();

                            runOnUiThread(() -> {
                                busy = true;
                                layout.setBackgroundColor(color);
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

        //Stop lights
        turnOffLights();
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 0)
            permissionGranted = grantResults != null && grantResults[0] == PackageManager.PERMISSION_GRANTED;
    }

    public void startType() {
        if (locked) return;
        locked = true;

        switch (type) {
            case NONE:
                light.setImageResource(R.drawable.ic_turned_off);
                layoutParams.screenBrightness = 0.0f;
                getWindow().setAttributes(layoutParams);
                turnOffLights();
                break;
            case SCREEN:
                light.setImageResource(R.drawable.ic_brightness);
                layoutParams.screenBrightness = 1.0f;
                getWindow().setAttributes(layoutParams);
                turnOffLights();
                break;
            case FLASH:
                light.setImageResource(R.drawable.ic_mobile_phone);
                layoutParams.screenBrightness = 0.0f;
                getWindow().setAttributes(layoutParams);
                turnOnLights();
                break;
            case ALL:
                light.setImageResource(R.drawable.ic_turned_on);
                layoutParams.screenBrightness = 1.0f;
                getWindow().setAttributes(layoutParams);

                if (!illuminating) turnOnLights();
                break;
            default:
                break;
        }
        preferences.edit().putInt("type", type.ordinal()).apply();
        locked = false;
    }

    public void startMode() {
        switch (mode) {
            case DEFAULT:
                pattern.setImageResource(R.drawable.ic_pantone);
                removeGrayFilter(cube);
                middleSquare.setClickable(true);
                middleSquare.setEnabled(true);
                break;
            case SOS:
                pattern.setImageResource(R.drawable.ic_help);
                setGrayFilter(cube);
                middleSquare.setClickable(false);
                middleSquare.setEnabled(false);
                break;
            default:
                break;
        }
        preferences.edit().putInt("mode", mode.ordinal()).apply();
    }

    public void showPicker() {
        int color = getColor();

        if (picker != null && picker.isShowing()) picker.dismiss();
        picker = new ColorPicker(MainActivity.this, Color.red(color), Color.green(color), Color.blue(color)); //Define default color for ColorPicker

        //Set listener
        picker.setCallback(color1 -> {
            setColor(color1);
            layout.setBackgroundColor(color1);
            setShapeColor(color1);
        });
        picker.show();
    }

    public void showViews() {
        unlockScreenOrientation();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        leftSquare.setVisibility(View.VISIBLE);
        middleSquare.setVisibility(View.VISIBLE);
        rightSquare.setVisibility(View.VISIBLE);
        cursor.setVisibility(View.VISIBLE);
        milliseconds[0] = 0;
    }

    public void hideViews() {
        lockScreenOrientation();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        leftSquare.setVisibility(View.GONE);
        middleSquare.setVisibility(View.GONE);
        rightSquare.setVisibility(View.GONE);
        cursor.setVisibility(View.INVISIBLE);
        milliseconds[0] = 0;
    }

    public int getColor() {
        return preferences.getInt("color", Color.WHITE);
    }

    public void setColor(int color) {
        preferences.edit().putInt("color", color).apply();
    }

    public void setShapeColor(int color) {
        int clearColor = color | 0xFF000000;
        double a = 1 - (0.299 * Color.red(clearColor) + 0.587 * Color.green(clearColor) + 0.114 * Color.blue(clearColor)) / 255;

        if (a < 0.5) {
            ((GradientDrawable) leftSquare.getBackground()).setColor(ColorUtils.setAlphaComponent(Color.BLACK, 26));
            ((GradientDrawable) middleSquare.getBackground()).setColor(ColorUtils.setAlphaComponent(Color.BLACK, 26));
            ((GradientDrawable) rightSquare.getBackground()).setColor(ColorUtils.setAlphaComponent(Color.BLACK, 26));
        } else {
            ((GradientDrawable) leftSquare.getBackground()).setColor(ColorUtils.setAlphaComponent(Color.WHITE, 26));
            ((GradientDrawable) middleSquare.getBackground()).setColor(ColorUtils.setAlphaComponent(Color.WHITE, 26));
            ((GradientDrawable) rightSquare.getBackground()).setColor(ColorUtils.setAlphaComponent(Color.WHITE, 26));
        }
    }

    public boolean hasFlash() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
        else permissionGranted = true;
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    private void turnOnLights() {
        if (permissionGranted) toggleLights(true);
    }

    private void turnOffLights() {
        if (permissionGranted) toggleLights(false);
    }

    private void toggleLights(boolean activated) {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        String cameraId;
        int size;

        try {
            size = cameraManager.getCameraIdList().length;
        } catch (CameraAccessException e) {
            size = 0;
        }
        boolean successful = false;

        for (int n = -1; ++n < size; ) {
            try {
                cameraId = cameraManager.getCameraIdList()[n];
                cameraManager.setTorchMode(cameraId, activated);
                successful = true;
                n = size;
            } catch (Exception ignored) {
            }
        }
        illuminating = activated && successful;
    }

    public void lockScreenOrientation() {
        WindowManager windowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        Configuration configuration = this.getResources().getConfiguration();
        int rotation = windowManager.getDefaultDisplay().getRotation();

        // Search for the natural position of the device
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) || configuration.orientation == Configuration.ORIENTATION_PORTRAIT && (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270)) {
            switch (rotation) { //Natural position is Landscape
                case Surface.ROTATION_0:
                    this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    break;
                case Surface.ROTATION_90:
                    this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                    break;
                case Surface.ROTATION_180:
                    this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                    break;
                case Surface.ROTATION_270:
                    this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    break;
            }
        } else {
            switch (rotation) { //Natural position is Portrait
                case Surface.ROTATION_0:
                    this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    break;
                case Surface.ROTATION_90:
                    this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    break;
                case Surface.ROTATION_180:
                    this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                    break;
                case Surface.ROTATION_270:
                    this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                    break;
            }
        }
    }

    public void unlockScreenOrientation() {
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    private void prepareAds(boolean restarted) {
        if (restarted) destroyAd();

        if (!adAdded) {
            adView = new AdView(MainActivity.this);
            adView.setAdSize(AdSize.BANNER);
            adView.setAdUnitId(AdUnitId.getBannerId());

            //Set listener
            adView.setAdListener(new AdListener() {
                public void onAdLoaded() {
                    if (!adAdded) {
                        RelativeLayout.LayoutParams params;
                        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                        params.addRule(RelativeLayout.CENTER_IN_PARENT);
                        adContainer.addView(adView, params);
                        adContainer.setVisibility(View.VISIBLE);

                        //Define button to close ad
                        params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                        ImageView imageView = new ImageView(MainActivity.this);
                        imageView.setAlpha(0.8F);
                        imageView.setImageResource(R.drawable.cancel);
                        adContainer.addView(imageView, params);

                        //Set listener
                        imageView.setOnClickListener(view -> destroyAd());
                        adAdded = true;
                    }
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
            adContainer.setVisibility(View.GONE);
            adContainer.removeAllViews();
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
