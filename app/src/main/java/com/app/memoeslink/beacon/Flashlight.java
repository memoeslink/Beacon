package com.app.memoeslink.beacon;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;

public final class Flashlight {
    private static boolean illuminating = false;

    private Flashlight() {
    }

    public static void turnOn(Context context) {
        if (!illuminating) toggle(context, true);
    }

    public static void turnOff(Context context) {
        if (illuminating) toggle(context, false);
    }

    public static void toggle(Context context, boolean activated) {
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH))
            return;
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        String cameraId;
        int size;

        try {
            size = cameraManager.getCameraIdList().length;
        } catch (CameraAccessException e) {
            size = 0;
        }
        boolean successful = false;

        for (int n = 0; n < size; n++) {
            try {
                cameraId = cameraManager.getCameraIdList()[n];
                cameraManager.setTorchMode(cameraId, activated);
                successful = true;
                break;
            } catch (Exception ignored) {
            }
        }
        illuminating = activated && successful;
    }
}
