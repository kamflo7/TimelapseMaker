package com.loony.timelapsemaker.camera;

/**
 * Created by Kamil on 7/21/2017.
 */

public interface OnTimelapseStateChangeListener {
    void onProgress(int capturedPhotos, byte[] imageCaptured);
    void onComplete();
    void onFail();
}
