package com.loony.timelapsemaker.camera;

import android.hardware.Camera;
import android.view.SurfaceHolder;

import com.loony.timelapsemaker.camera.exceptions.CameraNotAvailableException;

import java.io.IOException;

/**
 * Created by Kamil on 7/20/2017.
 */

public class CameraV1 {
    private Camera camera;

    public CameraV1() throws CameraNotAvailableException {
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
