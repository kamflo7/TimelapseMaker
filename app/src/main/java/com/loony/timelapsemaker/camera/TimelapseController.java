package com.loony.timelapsemaker.camera;

import android.content.Context;
import android.os.PowerManager;

/**
 * Created by Kamil on 7/20/2017.
 */

public class TimelapseController {
    private Context context;
    private TimelapseConfig timelapseConfig;

    private CameraImplV2 cameraImplV2;

    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;

    private int capturedPhotos;

    public TimelapseController(Context context, TimelapseConfig timelapseConfig) {
        this.context = context;
        this.timelapseConfig = timelapseConfig;
    }
}
