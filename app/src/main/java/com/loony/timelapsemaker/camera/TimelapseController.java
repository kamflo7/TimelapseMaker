package com.loony.timelapsemaker.camera;

import android.content.Context;
import android.os.PowerManager;

import com.loony.timelapsemaker.Util;
import com.loony.timelapsemaker.camera.exceptions.CameraNotAvailableException;

import static android.content.Context.POWER_SERVICE;

/**
 * Created by Kamil on 7/20/2017.
 */

public class TimelapseController {
    private Context context;
    private TimelapseConfig timelapseConfig;

    private Camera camera;

    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;

    private int capturedPhotos;

    public TimelapseController(Context context, TimelapseConfig timelapseConfig) throws CameraNotAvailableException {
        this.context = context;
        this.timelapseConfig = timelapseConfig;
        camera = Util.getAppropriateCamera();
        camera.prepare(context);
        camera.setOutputSize(timelapseConfig.getPictureSize());
    }

    public void start() throws CameraNotAvailableException {
        powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "myWakeLockTag");
        wakeLock.acquire();

        camera.openForCapturing(new OnCameraStateChangeListener() {
            @Override
            public void onCameraOpen() {
                capturedPhotos = 0;
                camera.capturePhoto();
            }

            @Override
            public void onCameraDisconnectOrError() {
                TimelapseController.this.stop();
            }
        }, onPhotoCaptureListener);
    }

    private OnPhotoCaptureListener onPhotoCaptureListener = new OnPhotoCaptureListener() {
        @Override
        public void onCreate(byte[] img) {
            // todo: save
        }

        @Override
        public void onFail() {

        }
    };

    public void stop() {
        if(wakeLock.isHeld())
            wakeLock.release();

        if(camera != null) {
            camera.close();
            camera = null;
        }
    }
}
