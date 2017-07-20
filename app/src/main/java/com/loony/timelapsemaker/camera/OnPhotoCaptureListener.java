package com.loony.timelapsemaker.camera;

/**
 * Created by Kamil on 7/20/2017.
 */

public interface OnPhotoCaptureListener {
    void onCreate(byte[] img);
    void onFail();
}
