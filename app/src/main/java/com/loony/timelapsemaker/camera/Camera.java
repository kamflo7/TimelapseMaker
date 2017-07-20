package com.loony.timelapsemaker.camera;

import android.content.Context;
import android.view.SurfaceHolder;

import com.loony.timelapsemaker.camera.exceptions.CameraNotAvailableException;

/**
 * Created by Kamil on 7/20/2017.
 */

public interface Camera {

    void prepare(Context context) throws CameraNotAvailableException;
    void openForPreview(SurfaceHolder surfaceHolder) throws CameraNotAvailableException;
    void openForCapturing(OnCameraStateChangeListener onCameraStateChangeListener, OnPhotoCaptureListener onPhotoCaptureListener) throws CameraNotAvailableException;
    Resolution[] getSupportedPictureSizes();
    void setOutputSize(Resolution size);
    void capturePhoto();
    void close();
}
