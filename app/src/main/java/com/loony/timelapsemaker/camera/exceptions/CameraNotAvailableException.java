package com.loony.timelapsemaker.camera.exceptions;


/**
 * Created by Kamil on 7/20/2017.
 */

public class CameraNotAvailableException extends Exception {
    public CameraNotAvailableException() {
        super();
    }

    public CameraNotAvailableException(String msg) {
        super(msg);
    }
}
