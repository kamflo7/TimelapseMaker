package com.loony.timelapsemaker.camera;

import android.view.Surface;

import com.loony.timelapsemaker.camera.exceptions.CameraNotAvailableException;

/**
 * Created by Kamil on 7/20/2017.
 */

public interface Camera {

    void prepare() throws CameraNotAvailableException;
    void openForPreview(Surface surface);


}
