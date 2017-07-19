package com.loony.timelapsemaker.camera;

/**
 * Created by Kamil on 7/19/2017.
 */

public interface OnCameraStateChangeListener {
    void onCameraOpen();
    void onCameraDisconnectOrError();
}
