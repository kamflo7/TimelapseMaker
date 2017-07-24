package com.loony.timelapsemaker.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.ImageReader;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.loony.timelapsemaker.Util;
import com.loony.timelapsemaker.camera.exceptions.CameraNotAvailableException;

import java.io.IOException;
import java.util.List;

/**
 * Created by Kamil on 7/20/2017.
 */

public class CameraImplV1 implements com.loony.timelapsemaker.camera.Camera {
    private Context context;
    private Camera camera;

    private OnCameraStateChangeListener onCameraStateChangeListener;
    private OnPhotoCaptureListener onPhotoCaptureListener;

    // surfaces
    private SurfaceTexture dummySurface;
    private Surface previewSurface;
    private ImageReader imageReader;
    private Resolution outputSize;
    private SurfaceView surfaceView;

    @Override
    public void prepare(Context context) throws CameraNotAvailableException {
        this.context = context;
        camera = getCameraInstance();
        if(camera == null) throw new CameraNotAvailableException();
    }

    public void openForPreview(SurfaceHolder surfaceHolder) {
        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void openForCapturing(OnCameraStateChangeListener onCameraStateChangeListener, OnPhotoCaptureListener onPhotoCaptureListener) throws CameraNotAvailableException {
        this.onCameraStateChangeListener = onCameraStateChangeListener;
        this.onPhotoCaptureListener = onPhotoCaptureListener;

        if(outputSize == null) // should be another name for this exception, but this is small simple project, so who cares
            throw new CameraNotAvailableException("Output size is not defined");

        dummySurface = new SurfaceTexture(10);

        try {
            camera.setPreviewTexture(dummySurface);
            onCameraStateChangeListener.onCameraOpen();
        } catch (IOException e) {
            throw new CameraNotAvailableException("camera.setPreviewDisplay() -> " + e.getMessage());
        }
    }

    @Override
    public void capturePhoto() {
        camera.startPreview();

        camera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {
                camera.stopPreview();
                Util.log("CameraImplV1::capturePhoto() -> onPictureTaken");
                CameraImplV1.this.onPhotoCaptureListener.onCreate(bytes);
            }
        });
    }

    @Override
    public Resolution[] getSupportedPictureSizes() {
        List<Camera.Size> list = camera.getParameters().getSupportedPictureSizes();

        Resolution[] resolutions = new Resolution[list.size()];
        for(int i=0; i<list.size(); i++)
            resolutions[i] = new Resolution(list.get(i).width, list.get(i).height);

        return resolutions;
    }

    @Override
    public void setOutputSize(Resolution size) {
        this.outputSize = size;
    }

    @Override
    public void close() {
        if(camera != null) {
            camera.stopPreview();
            //camera.unlock();
            camera.release();
            camera = null;
        }
    }

    private Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
        }
        return c; // returns null if camera is unavailable
    }
}
