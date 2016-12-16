package com.loony.timelapsemaker.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Size;
import android.view.Surface;

import com.loony.timelapsemaker.Util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by Kamil on 11/13/2016.
 */

public class MyCamera {
    private static final int IMAGE_FORMAT = ImageFormat.JPEG;//256; // JPEG
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAITING_LOCK = 1;
    private static final int STATE_WAITING_PRECAPTURE = 2;
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;
    private static final int STATE_PICTURE_TAKEN = 4;

    private Context context;
    private CameraManager mCameraManager;
    private CameraCharacteristics mCameraCharacteristics;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CameraDevice mCameraDevice;
    private ImageReader mImageReader;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest mPreviewRequest;

    private String cameraId;
    private Size largestSize;
    private StreamConfigurationMap map;
    private SurfaceTexture dummySurface;
    private Surface previewSurface;
    private boolean cameraFound = false;
    private int currentNumberPhoto;
    private int mState = STATE_PREVIEW;
    private boolean startedRepeatedRequest;
    private int waitingLockCount;
    private float lastLens;
    private long timeCapture;

    public interface MyCameraStateChange {
        void onCameraOpen();
        void onCameraDisconnectOrError(boolean isError, int error);
    }

    public interface MyOnPhotoCaptureListener {
        void onCreated();
        void onFailed();
    }

    private MyCameraStateChange listenerStateChange;
    private MyOnPhotoCaptureListener listenerPhotoCapture;

    public MyCamera(Context context, MyCameraStateChange listener) throws CameraAccessException, CameraNotFoundException, CameraImageFormatNotFoundException, SecurityException {
        this.context = context;
        this.listenerStateChange = listener;
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

        for(String camera : mCameraManager.getCameraIdList()) {
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(camera);
            if(characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                cameraId = camera;
                mCameraCharacteristics = characteristics;
                cameraFound = true;
            }
        }

        map = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if(!cameraFound) throw new CameraNotFoundException("Camera not found");
        if(!checkForFormatExistence(IMAGE_FORMAT, map.getOutputFormats())) throw new CameraImageFormatNotFoundException("Image format not found");

        largestSize = map.getOutputSizes(IMAGE_FORMAT)[0];
        mImageReader = ImageReader.newInstance(largestSize.getWidth(), largestSize.getHeight(), IMAGE_FORMAT, 2);
        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, null);

        dummySurface = new SurfaceTexture(10);
        dummySurface.setDefaultBufferSize(largestSize.getWidth(), largestSize.getHeight());
        previewSurface = new Surface(dummySurface);
        mCameraManager.openCamera(cameraId, mStateCallback, null);
    }

    private void createCameraPreviewSession() throws CameraAccessException {
        mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        mPreviewRequestBuilder.addTarget(previewSurface);
        setCameraOrientation(mPreviewRequestBuilder);

        mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(CameraCaptureSession session) {
                mCaptureSession = session;
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                mPreviewRequest = mPreviewRequestBuilder.build();
                try {
                    startedRepeatedRequest = false;
                    mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, null);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConfigureFailed(CameraCaptureSession session) {
                listenerStateChange.onCameraDisconnectOrError(false, -1);
                Util.log("createCaptureSession::onConfigureFailed");
            }
        }, null);
    }

    public void makePhoto(int number, MyOnPhotoCaptureListener l) {
        Util.log("makePhoto(%d) called", number);
        currentNumberPhoto = number;
        this.listenerPhotoCapture = l;
        waitingLockCount = 0;

        timeCapture = System.currentTimeMillis();

        try {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START); // This is how to tell the camera to lock focus.
            mState = STATE_WAITING_LOCK; // Tell #mCaptureCallback to wait for the lock.
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, null);
        } catch(CameraAccessException e) {
            listenerStateChange.onCameraDisconnectOrError(false, -1);
        }
    }

    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;

            try {
                createCameraPreviewSession();
            } catch (CameraAccessException e) {
                listenerStateChange.onCameraDisconnectOrError(false, -1);
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
            listenerStateChange.onCameraDisconnectOrError(false, -1);
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
            listenerStateChange.onCameraDisconnectOrError(true, error);
            Util.log("Camera mStateCallback error: " + error);
        }
    };

    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() { // listenerStateChange.onCameraOpen();
        private void process(CaptureResult result) throws CameraAccessException {
            if(!startedRepeatedRequest) {
                listenerStateChange.onCameraOpen();
                startedRepeatedRequest = true;
            }

            switch(mState) {
                case STATE_PREVIEW: {
                    break;
                }
                case STATE_WAITING_LOCK: {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    Float lensDistance = result.get(CaptureResult.LENS_FOCUS_DISTANCE);

                    if(lensDistance != null) lastLens = lensDistance.floatValue();
                    waitingLockCount++;

                    if (afState == null) {
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE); // CONTROL_AE_STATE can be null on some devices
                        if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
//                    Util.log("STATE_WAITING_PRECAPTURE");
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE); // CONTROL_AE_STATE can be null on some devices
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
//                    Util.log("STATE_WAITING_NON_PRECAPTURE");
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE); // CONTROL_AE_STATE can be null on some devices
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
            }
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
            try {
                process(partialResult);
            }
            catch(CameraAccessException e) {
                listenerStateChange.onCameraDisconnectOrError(false, -1);
            }
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            try {
                process(result);
            }
            catch(CameraAccessException e) {
                listenerStateChange.onCameraDisconnectOrError(false, -1);
            }
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
            Util.log("Capture failed in MyCamera[Request: %s][Failure: Frame %d; Reason: %d; SequenceID: %d; wasImgCaptured: %d]", request.toString(),
                    failure.getFrameNumber(), failure.getReason(), failure.getSequenceId(), failure.wasImageCaptured() ? 1 : 0);

            listenerPhotoCapture.onFailed();

            super.onCaptureFailed(session, request, failure);
        }

        @Override
        public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
        }
    };

    private void captureStillPicture() throws CameraAccessException {
        final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

        Surface surfaceTarget = mImageReader.getSurface();
        captureBuilder.addTarget(surfaceTarget);
        captureBuilder.set(CaptureRequest.JPEG_QUALITY, (byte) 90);
        setCameraOrientation(captureBuilder);
        captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);// Use the same AE and AF modes as the preview.
        CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
//                    Util.log("captureStillPicture::onCaptureCompleted");

                // ##
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
                try {
                    mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, null);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
                // ##

                mState = STATE_PREVIEW;
//                    session.close();
//                    mCameraDevice.close();
            }

            @Override
            public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
                Util.log("Capture failed FINAL in MyCamera[Request: %s][Failure: Frame %d; Reason: %d; SequenceID: %d; wasImgCaptured: %d]", request.toString(),
                        failure.getFrameNumber(), failure.getReason(), failure.getSequenceId(), failure.wasImageCaptured() ? 1 : 0);

                mState = STATE_PREVIEW;
                session.close();
//                    startedRepeatedRequest = false;
                listenerPhotoCapture.onFailed();

                super.onCaptureFailed(session, request, failure);
            }
        };

        mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
    }

    private void runPrecaptureSequence() throws CameraAccessException {
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START); // This is how to tell the camera to trigger.
        mState = STATE_WAITING_PRECAPTURE; // Tell #mCaptureCallback to wait for the precapture sequence to be set.
        mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, null);
    }

    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Util.log("CaptureCompleted; waitingLockCount: %d; lastLens: %.003f; time capture photo: %d", waitingLockCount, lastLens, System.currentTimeMillis()-timeCapture);

            Image image = reader.acquireLatestImage();
            saveImageToDisk(image);
            image.close();

            if(listenerPhotoCapture != null)
                listenerPhotoCapture.onCreated();
        }
    };

    private void setCameraOrientation(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.JPEG_ORIENTATION, 0);
//        Util.log("SensorOrientation: " + mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION));
    }

    private boolean checkForFormatExistence(int targetFormat, @NonNull int[] formats) {
        for(int format : formats) {
            if(format == targetFormat)
                return true;
        }
        return false;
    }

    private void saveImageToDisk(Image image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        File photo = new File(Environment.getExternalStorageDirectory(), "myPhoto"+currentNumberPhoto+".jpg");

        if(photo.exists())
            photo.delete();

        try {
            FileOutputStream fos = new FileOutputStream(photo.getPath());

            fos.write(bytes);
            fos.close();

            Util.log("Saved image to %s", photo.getPath());
        } catch(IOException e) {
            Util.log("Problem with saving image " + e.getMessage());
        }
    }

    public void closeCamera() {
        mCaptureSession.close();

        if(mCameraDevice != null) {
            mCameraDevice.close();
        }
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
