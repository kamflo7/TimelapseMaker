package com.loony.timelapsemaker.camera;

import android.content.Context;
import android.hardware.Camera;
import android.view.SurfaceHolder;

import com.loony.timelapsemaker.camera.exceptions.CameraNotAvailableException;

import java.io.IOException;
import java.util.List;

/**
 * Created by Kamil on 7/20/2017.
 */

public class CameraImplV1 implements com.loony.timelapsemaker.camera.Camera {
    private Camera camera;

    private OnCameraStateChangeListener onCameraStateChangeListener;
    private OnPhotoCaptureListener onPhotoCaptureListener;

    @Override
    public void prepare(Context context) throws CameraNotAvailableException {
        camera = getCameraInstance();
        if(camera == null) throw new CameraNotAvailableException();
    }

    public void openForPreview(SurfaceHolder surfaceHolder) {
        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
            //camera.lock();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void openForCapturing(OnCameraStateChangeListener onCameraStateChangeListener, OnPhotoCaptureListener onPhotoCaptureListener) {
        this.onCameraStateChangeListener = onCameraStateChangeListener;
        this.onPhotoCaptureListener = onPhotoCaptureListener;
    }

    @Override
    public void capturePhoto() {

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
        //todo: Need to do this ofc
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
