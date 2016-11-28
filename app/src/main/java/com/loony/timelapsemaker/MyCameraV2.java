package com.loony.timelapsemaker;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.support.annotation.NonNull;
import android.util.Size;

/**
 * Created by Kamil on 11/24/2016.
 */

public class MyCameraV2 {
    private static final int IMAGE_FORMAT = ImageFormat.JPEG;//256; // JPEG

    private Context context;
    private CameraManager manager;
    private String cameraId;
    private CameraCharacteristics cameraCharacteristics;
    private boolean cameraFound = false;
    private StreamConfigurationMap map;
    private Size largestSize;

    public MyCameraV2(Context context) throws CameraAccessException, CameraNotFoundException, CameraImageFormatNotFoundException {
        this.context = context;

        manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

        try {
            String[] cameras = manager.getCameraIdList();

            if(cameras != null) {
                for(String camera : cameras) {
                    CameraCharacteristics characteristics = manager.getCameraCharacteristics(camera);
                    if(characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                        cameraId = camera;
                        cameraCharacteristics = characteristics;
                        cameraFound = true;
                    }
                }
            }
        } catch (CameraAccessException e) {
            throw e;
        }

        if(!cameraFound)
            throw new CameraNotFoundException("Back camera not found");

        map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if(!checkForFormatExistence(IMAGE_FORMAT, map.getOutputFormats())) {
            throw new CameraImageFormatNotFoundException("MyCamera: Format not found");
        }

        largestSize = map.getOutputSizes(IMAGE_FORMAT)[0];
    }

    private boolean checkForFormatExistence(int targetFormat, @NonNull int[] formats) {
        for(int format : formats) {
            if(format == targetFormat)
                return true;
        }
        return false;
    }

    public class CameraNotFoundException extends Exception {
        public CameraNotFoundException(String msg) {
            super(msg);
        }
    }

    public class CameraImageFormatNotFoundException extends Exception {
        public CameraImageFormatNotFoundException(String msg) {
            super(msg);
        }
    }
}
