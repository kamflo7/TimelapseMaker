package com.loony.timelapsemaker.camera;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.os.PowerManager;

import static android.content.Context.POWER_SERVICE;

/**
 * Created by Kamil on 12/15/2016.
 */

public class TimelapseController {
    private Context context;
    private TimelapseConfig timelapseConfig;
    private MyCamera camera;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;

    private int capturedPhotos;
    private long lastCaptureDurationMiliseconds = -1;
    private long captureStarTimetMiliseconds;
    private StateChange listener;

    public TimelapseController(TimelapseConfig config, Context context, final StateChange listener) {
        this.timelapseConfig = config;
        this.context = context;
        this.listener = listener;

        powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "myWakeLockTag");
        wakeLock.acquire();

        try {
            camera = new MyCamera(context, new MyCamera.MyCameraStateChange() {
                @Override
                public void onCameraOpen() {
                    capturedPhotos = 0;
                    captureStarTimetMiliseconds = System.currentTimeMillis();
                    camera.makePhoto(capturedPhotos, captureListener);
                }

                @Override
                public void onCameraDisconnectOrError(boolean isError, int error) {
                    close();
                    listener.onSessionCrash();
                }
            });
        } catch (CameraAccessException | MyCamera.CameraNotFoundException | MyCamera.CameraImageFormatNotFoundException e) {
            close();
            listener.onSessionCrash();
        }

    }

    private MyCamera.MyOnPhotoCaptureListener captureListener = new MyCamera.MyOnPhotoCaptureListener() {
        @Override
        public void onCreated() {
            capturedPhotos++;
            lastCaptureDurationMiliseconds = System.currentTimeMillis() - captureStarTimetMiliseconds;

            if(capturedPhotos == timelapseConfig.getPhotosAmount()) {
                listener.onPhotoCapture();
                listener.onSessionEnd();
                close();
            } else {
                listener.onPhotoCapture();

                if(timelapseConfig.getFrequencyCaptureMiliseconds() > 0) {
                    try {
                        Thread.sleep(timelapseConfig.getFrequencyCaptureMiliseconds());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        close();
                        listener.onSessionCrash();
                        return;
                    }
                }

                if(camera == null)
                    return;

                captureStarTimetMiliseconds = System.currentTimeMillis();
                camera.makePhoto(capturedPhotos, captureListener);
            }
        }

        @Override
        public void onFailed() {
            close();
            listener.onSessionCrash();
        }
    };

    public int getCapturedAmount() {
        return capturedPhotos;
    }

    public Long getLastCaptureDurationMiliseconds() {
        if(lastCaptureDurationMiliseconds == -1) return null;
        return lastCaptureDurationMiliseconds;
    }

    public void close() {
        if(wakeLock.isHeld())
            wakeLock.release();

        if(camera != null) {
            camera.closeCamera();
            camera = null;
        }
    }

    public interface StateChange {
        void onSessionCrash();
        void onPhotoCapture();
        void onSessionEnd();
    }
}
