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

//    private List<Surface> surfaces;
    private int currentNumberPhoto;
    CaptureRequest.Builder mPreviewRequestBuilder;
    private CameraDevice mCameraDevice;
    private ImageReader mImageReader;
    private Size largestSize;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest mPreviewRequest;

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
//                        openCamera(manager, camera, characteristics);
                        cameraId = camera;
                        cameraCharacteristics = characteristics;
                        cameraFound = true;
                    }
                }
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // #1
    public void makeAPhoto(int number) {
        if(!cameraFound) return;
        currentNumberPhoto = number;

        try {
            setUpCameraOutputs();
            manager.openCamera(cameraId, mStateCallback, null);
        } catch(SecurityException e) {
        }
        catch(CameraAccessException e) {
        }
    }

    // #2
    private void setUpCameraOutputs() {
        StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if(!checkForFormatExistence(IMAGE_FORMAT, map.getOutputFormats())) {
            Util.log("Fatal error, no format");
            return;
        }

        largestSize = map.getOutputSizes(IMAGE_FORMAT)[0];
        mImageReader = ImageReader.newInstance(largestSize.getWidth(), largestSize.getHeight(), IMAGE_FORMAT, 2);
        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, null);
    }

    // #4
    private void createCameraPreviewSession() {
        SurfaceTexture dummySurface = new SurfaceTexture(10);
        dummySurface.setDefaultBufferSize(largestSize.getWidth(), largestSize.getHeight());
        Surface previewSurface = new Surface(dummySurface);

        try {
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(previewSurface);

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
                    try {
                        mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }

//                    try {
//                        Thread.sleep(2000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
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
            // This is how to tell the camera to lock focus.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_LOCK;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void unlockFocus() {
        Util.log("unlockFocus() called");
        try {
            // Reset the auto-focus trigger
//            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
//            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW;
//            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);
            mCaptureSession.abortCaptures();

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
                    Util.log("STATE_WAITING_LOCK");
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
    };

    private void captureStillPicture() {
        final CaptureRequest.Builder captureBuilder;
        try {
            captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

            Surface surfaceTarget = mImageReader.getSurface();
            captureBuilder.addTarget(surfaceTarget);
            captureBuilder.set(CaptureRequest.JPEG_QUALITY, (byte) 90);

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    Util.log("captureStillPicture::onCaptureCompleted");
                    unlockFocus();
                    session.close();
                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();// delete
            Util.log("Now will be capturing final image");
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE;
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
//            reader.close();
        }
    };

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
