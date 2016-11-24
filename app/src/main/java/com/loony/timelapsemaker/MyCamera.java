package com.loony.timelapsemaker;

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
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Size;
import android.view.Surface;

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

    private Context context;

    private CameraManager manager;
    private String cameraId;
    private CameraCharacteristics cameraCharacteristics;
    private boolean cameraFound = false;

    private int currentNumberPhoto;
    CaptureRequest.Builder mPreviewRequestBuilder;
    private CameraDevice mCameraDevice;
    private ImageReader mImageReader;
    private Size largestSize;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest mPreviewRequest;
    private StreamConfigurationMap map;

    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAITING_LOCK = 1;
    private static final int STATE_WAITING_PRECAPTURE = 2;
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;
    private static final int STATE_PICTURE_TAKEN = 4;

    private int mState = STATE_PREVIEW;

    public MyCamera(Context context) {
        this.context = context;

        init();
    }

    private void init() {
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
            e.printStackTrace();
        }

        if(!cameraFound) {
            throw new RuntimeException("MyCamera: Camera not found"); // better to throw custom exception like a CameraNotFoundException?
        }

        map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if(!checkForFormatExistence(IMAGE_FORMAT, map.getOutputFormats())) {
            throw new RuntimeException("MyCamera: Format not found"); // jak wyżej
        }
    }

    public interface OnPhotoCreatedListener {
        void onCreated();
    }

    private OnPhotoCreatedListener listener;

    // #1
    public void makePhoto(int number, OnPhotoCreatedListener l) {
        Util.log("makePhoto(%d) called", number);
        if(!cameraFound) return;
        currentNumberPhoto = number;
        this.listener = l;

        try {
            setUpCameraOutputs();
            manager.openCamera(cameraId, mStateCallback, null);
        } catch(SecurityException e) {
            throw new RuntimeException("MyCamera: " + e.getMessage());
        }
        catch(CameraAccessException e) {
            throw new RuntimeException("MyCamera: " + e.getMessage());
        }
    }

    // #2
    private void setUpCameraOutputs() {
        largestSize = map.getOutputSizes(IMAGE_FORMAT)[0];
        mImageReader = ImageReader.newInstance(largestSize.getWidth(), largestSize.getHeight(), IMAGE_FORMAT, 2);
        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, null);
    }

    private SurfaceTexture dummySurface;
    private Surface previewSurface;

    // #4
    private void createCameraPreviewSession() {
        dummySurface = new SurfaceTexture(10);
        dummySurface.setDefaultBufferSize(largestSize.getWidth(), largestSize.getHeight());
        previewSurface = new Surface(dummySurface);

        try {
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(previewSurface);
            setCameraOrientation(mPreviewRequestBuilder);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    if(mCameraDevice == null) {
                        Util.log("error: mCameraDevice is null");
                        return;
                    }

                    mCaptureSession = session;

                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                    mPreviewRequest = mPreviewRequestBuilder.build();
                    Util.log("CreateCameraPreviewSession[Request: %s]", mPreviewRequest.toString());
                    try {
                        mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }

                    lockFocus();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Util.log("createCaptureSession::onConfigureFailed");
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private void lockFocus() {
        Util.log("lockFocus called");
        try {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START); // This is how to tell the camera to lock focus.
            mState = STATE_WAITING_LOCK; // Tell #mCaptureCallback to wait for the lock.
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void unlockFocus() {
        Util.log("unlockFocus() called");
        try {
            mState = STATE_PREVIEW;
            mCaptureSession.abortCaptures();
//            mCaptureSession.close();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // #3
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
        }
    };

    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        private void process(CaptureResult result) {
            switch(mState) {
                case STATE_PREVIEW: {
                    Util.log("STATE_PREVIEW");
                    break;
                }
                case STATE_WAITING_LOCK: {
//                    Util.log("STATE_WAITING_LOCK");
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
                        Util.log("afState is null");
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        Util.log("afState is " + afState.intValue());
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
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
                    Util.log("STATE_WAITING_PRECAPTURE");
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    Util.log("STATE_WAITING_NON_PRECAPTURE");
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
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
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            process(result);
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
            Util.log("Capture failed in MyCamera[Request: %s][Failure: Frame %d; Reason: %d; SequenceID: %d; wasImgCaptured: %d]", request.toString(),
                    failure.getFrameNumber(), failure.getReason(), failure.getSequenceId(), failure.wasImageCaptured() ? 1 : 0);
            super.onCaptureFailed(session, request, failure);
        }
    };

    private void captureStillPicture() {
        final CaptureRequest.Builder captureBuilder;
        try {
            captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

            Surface surfaceTarget = mImageReader.getSurface();
            captureBuilder.addTarget(surfaceTarget);
            captureBuilder.set(CaptureRequest.JPEG_QUALITY, (byte) 90);
            setCameraOrientation(captureBuilder);

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    Util.log("captureStillPicture::onCaptureCompleted");
                    unlockFocus();
                    session.close();
                    mCameraDevice.close();
                }

                @Override
                public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
                    Util.log("Capture failed FINAL in MyCamera[Request: %s][Failure: Frame %d; Reason: %d; SequenceID: %d; wasImgCaptured: %d]", request.toString(),
                            failure.getFrameNumber(), failure.getReason(), failure.getSequenceId(), failure.wasImageCaptured() ? 1 : 0);
                    super.onCaptureFailed(session, request, failure);
                }
            };

            mCaptureSession.stopRepeating();
//            mCaptureSession.close();
            mCaptureSession.abortCaptures();
            Util.log("Now will be capturing final image");
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void runPrecaptureSequence() {
        try {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START); // This is how to tell the camera to trigger.
            mState = STATE_WAITING_PRECAPTURE; // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireLatestImage();
            Util.log("onImageAvailable, height: " + image.getHeight());
            saveImageToDisk(image);
            image.close();
            reader.setOnImageAvailableListener(null, null);
            reader.close();
            mImageReader.close();
            mImageReader = null;

            dummySurface.release();
            previewSurface.release();
            dummySurface = null;
            previewSurface = null;

            if(listener != null)
                listener.onCreated();
        }
    };

    private void setCameraOrientation(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.JPEG_ORIENTATION, cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION));
    }

    private boolean checkForFormatExistence(int targetFormat, @NonNull int[] formats) {
        for(int format : formats) {
            if(format == targetFormat)
                return true;
        }
        return false;
    }

    // #5
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
}
