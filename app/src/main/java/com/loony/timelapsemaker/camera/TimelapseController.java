package com.loony.timelapsemaker.camera;

import android.content.Context;
import android.os.PowerManager;

import com.loony.timelapsemaker.StorageManager;
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

    private OnTimelapseStateChangeListener onTimelapseStateChangeListener;
    private int capturedPhotos;

    private StorageManager storageManager;
    private String directoryForPhotos;

    public TimelapseController(Context context, TimelapseConfig timelapseConfig) throws CameraNotAvailableException {
        this.context = context;
        this.timelapseConfig = timelapseConfig;
        camera = Util.getAppropriateCamera();
        camera.prepare(context);
        camera.setOutputSize(timelapseConfig.getPictureSize());
    }

    public void start(OnTimelapseStateChangeListener onTimelapseStateChangeListener) throws CameraNotAvailableException {
        this.onTimelapseStateChangeListener = onTimelapseStateChangeListener;

        storageManager = new StorageManager(context);
//        if(!storageManager.isExternalStorageAvailable()) {
//            this.onTimelapseStateChangeListener.onFail();
//            return;
//        }
        if(!storageManager.isRealExternalStorageAvailable()) {
            this.onTimelapseStateChangeListener.onFail();
            return;
        }

        directoryForPhotos = storageManager.createDirectory();
        if(directoryForPhotos == null) {
            this.onTimelapseStateChangeListener.onFail();
            return;
        }

        this.onTimelapseStateChangeListener.onInit(directoryForPhotos);

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
            storageManager.saveImage(directoryForPhotos, String.format("photo%d", capturedPhotos), img);

            try {
                capturedPhotos++;

                if(capturedPhotos == timelapseConfig.getPhotosLimit()) {
                    onTimelapseStateChangeListener.onComplete();
                    stop();
                    return;
                } else {
                    onTimelapseStateChangeListener.onProgress(capturedPhotos, img);
                }

                Thread.sleep(timelapseConfig.getMilisecondsInterval());

                if(camera == null) {
                    Util.log("onPhotoCaptureListener::onFail");
                    onTimelapseStateChangeListener.onFail();
                    return;
                }

                camera.capturePhoto();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFail() {
            if(camera != null) {
                stop();
                onTimelapseStateChangeListener.onFail();
            }
        }
    };

    public void stop() {
        if(wakeLock != null && wakeLock.isHeld())
            wakeLock.release();

        if(camera != null) {
            camera.close();
            camera = null;
        }
    }
}
