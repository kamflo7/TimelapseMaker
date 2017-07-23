package com.loony.timelapsemaker.camera;

import android.annotation.TargetApi;
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
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.loony.timelapsemaker.Util;
import com.loony.timelapsemaker.camera.exceptions.CameraNotAvailableException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Kamil on 7/19/2017.
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class CameraImplV2 implements Camera {
    // definitions, consts
    private static final int IMAGE_FORMAT = ImageFormat.JPEG;

    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAITING_LOCK = 1;
    private static final int STATE_WAITING_PRECAPTURE = 2;
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;
    private static final int STATE_PICTURE_TAKEN = 4;

    // general vars
    private Context context;
    private OnCameraStateChangeListener onCameraStateChangeListener;
    private CameraManager cameraManager;

    // camera configuration
    private String cameraID;
    private StreamConfigurationMap map;
    private CameraCharacteristics cameraCharacteristics;
    private Resolution outputSize;

    // surfaces
    private SurfaceTexture dummySurface;
    private Surface previewSurface;
    private ImageReader imageReader;

    // camera stateCallback, onOpen -> obtaining these
    private CameraDevice cameraDevice;

    // then, onConfigured (createCaptureSession)
    private CaptureRequest.Builder previewRequestBuilder;
    private CameraCaptureSession captureSession;
    private CaptureRequest previewRequest;

    // then, CameraCaptureSession.CaptureCallback
    private OnPhotoCaptureListener onPhotoCaptureListener;
    private int captureState;

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    private void startBackgroundThread() { // in openForPreview() starts
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if(backgroundThread == null)
            return;

        //backgroundThread.quitSafely();
        backgroundThread.quit();

        // here was problem in case of capturingPhotos, not previewing
        //backgroundThread.join();

        backgroundThread = null;
        backgroundHandler = null;

    }

    public CameraImplV2() {}

    @Override
    public void prepare(Context context) throws CameraNotAvailableException {
        this.context = context;
        this.cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

        if (!findBackCamera())
            throw new CameraNotAvailableException("Back camera not found");

        map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (!checkForFormatExistence(IMAGE_FORMAT, map.getOutputFormats()))
            throw new CameraNotAvailableException("Image format not found");
    }

    @Override
    public void openForPreview(SurfaceHolder surfaceHolder) throws CameraNotAvailableException {
        if(outputSize == null) // should be another name for this exception, but this is small simple project, so who cares
            throw new CameraNotAvailableException("Output size is not defined");

        //dummySurface = new SurfaceTexture(10);
        //dummySurface.setDefaultBufferSize(outputSize.getWidth(), outputSize.getHeight());
        previewSurface = surfaceHolder.getSurface();
        startBackgroundThread();

        try {
            cameraManager.openCamera(cameraID, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice cameraDevice) {
                    CameraImplV2.this.cameraDevice = cameraDevice;

                    try {
                        createCameraPreviewSession();
                    } catch (CameraAccessException e) {
                        close();
                        if(onCameraStateChangeListener != null) onCameraStateChangeListener.onCameraDisconnectOrError();
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                    cameraDevice.close();
                    CameraImplV2.this.cameraDevice = null;
                    CameraImplV2.this.close();
                    if(onCameraStateChangeListener != null) onCameraStateChangeListener.onCameraDisconnectOrError();
                }

                @Override
                public void onError(@NonNull CameraDevice cameraDevice, int i) {
                    cameraDevice.close();
                    CameraImplV2.this.cameraDevice = null;
                    CameraImplV2.this.close();
                    if(onCameraStateChangeListener != null) onCameraStateChangeListener.onCameraDisconnectOrError();
                }
            }, backgroundHandler);
        } catch(CameraAccessException | SecurityException e) {
            throw new CameraNotAvailableException();
        }
    }

    public void openForCapturing(OnCameraStateChangeListener onCameraStateChangeListener, final OnPhotoCaptureListener onPhotoCaptureListener) throws CameraNotAvailableException {
        this.onCameraStateChangeListener = onCameraStateChangeListener;
        this.onPhotoCaptureListener = onPhotoCaptureListener;

        if(outputSize == null) // should be another name for this exception, but this is small simple project, so who cares
            throw new CameraNotAvailableException("Output size is not defined");

        dummySurface = new SurfaceTexture(10);
        dummySurface.setDefaultBufferSize(outputSize.getWidth(), outputSize.getHeight());
        previewSurface = new Surface(dummySurface);
        startBackgroundThread();

        imageReader = ImageReader.newInstance(outputSize.getWidth(), outputSize.getHeight(), IMAGE_FORMAT, 2);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            // todo: edit to call listener and pass byte[] buffer of image to make this compatible with different versions of android
            @Override
            public void onImageAvailable(ImageReader imageReader) {
                Image image = imageReader.acquireLatestImage();
                //saveImageToDisk(image);

                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);

                image.close();

                onPhotoCaptureListener.onCreate(bytes);
                Util.log("OnImageAvailableListener called");
            }
        }, backgroundHandler);

        try {
            if(backgroundHandler == null || backgroundThread == null) Util.log("MAM NULLA");
            cameraManager.openCamera(cameraID, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice cameraDevice) {
                    CameraImplV2.this.cameraDevice = cameraDevice;

                    try {
                        createCameraPreviewSession(false);
                    } catch (CameraAccessException e) {
                        close();
                        CameraImplV2.this.onCameraStateChangeListener.onCameraDisconnectOrError();
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                    cameraDevice.close();
                    CameraImplV2.this.cameraDevice = null;
                    CameraImplV2.this.onCameraStateChangeListener.onCameraDisconnectOrError();
                }

                @Override
                public void onError(@NonNull CameraDevice cameraDevice, int i) {
                    cameraDevice.close();
                    CameraImplV2.this.cameraDevice = null;
                    CameraImplV2.this.onCameraStateChangeListener.onCameraDisconnectOrError();
                }
            }, backgroundHandler);
        } catch(CameraAccessException | SecurityException e) {
            throw new CameraNotAvailableException();
        }
    }

    private int photoNumberTest;

    /*private void saveImageToDisk(Image image) { // todo: temporary
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        File photo = new File(Environment.getExternalStorageDirectory(), "myPhoto"+(photoNumberTest++)+".jpg");

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
    }*/

    @Override
    public void capturePhoto() {
        try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            captureState = STATE_WAITING_LOCK;
            captureSession.capture(previewRequestBuilder.build(), captureCallback, backgroundHandler);
        } catch(CameraAccessException e) {
            close();
            onCameraStateChangeListener.onCameraDisconnectOrError();
        }
    }

    private void createCameraPreviewSession() throws CameraAccessException {
        createCameraPreviewSession(true);
    }

    private void createCameraPreviewSession(boolean preview) throws CameraAccessException {
        previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        previewRequestBuilder.addTarget(previewSurface);
        setCameraOrientation(previewRequestBuilder);

        List<Surface> surfaceList = preview ? Arrays.asList(previewSurface) : Arrays.asList(previewSurface, imageReader.getSurface());
        cameraDevice.createCaptureSession(surfaceList, new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                CameraImplV2.this.captureSession = cameraCaptureSession;
                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                previewRequest = previewRequestBuilder.build();

                //startedRepeatedRequest = false;
                try {
                    //Util.log("__createCameraPreviewSession (equals?) THREAD_ID: " + Thread.currentThread().getId());
                    captureSession.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler);
                    if(onCameraStateChangeListener != null)
                        onCameraStateChangeListener.onCameraOpen(); // todo: Not sure whether this is a best place
                } catch (CameraAccessException e) {
                    close();
                    if(onCameraStateChangeListener != null)
                        onCameraStateChangeListener.onCameraDisconnectOrError();
                }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                close();
                onCameraStateChangeListener.onCameraDisconnectOrError();
            }
        }, backgroundHandler);
    }

    private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        private void process(CaptureResult result) throws CameraAccessException {
            /*if(!startedRepeatedRequest) {
                listenerStateChange.onCameraOpen();
                startedRepeatedRequest = true;
            }*/

            switch(captureState) {
                case STATE_PREVIEW: {
                    break;
                }
                case STATE_WAITING_LOCK: {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    Float lensDistance = result.get(CaptureResult.LENS_FOCUS_DISTANCE);

                    //if(lensDistance != null) lastLens = lensDistance.floatValue();
                    //waitingLockCount++;

                    if (afState == null) {
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE); // CONTROL_AE_STATE can be null on some devices
                        if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            captureState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE); // CONTROL_AE_STATE can be null on some devices
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        captureState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE); // CONTROL_AE_STATE can be null on some devices
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        captureState = STATE_PICTURE_TAKEN;
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
                onCameraStateChangeListener.onCameraDisconnectOrError();
            }
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            try {
                process(result);
            }
            catch(CameraAccessException e) {
                onCameraStateChangeListener.onCameraDisconnectOrError();
            }
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
            Util.log("Capture failed in MyCamera[Request: %s][Failure: Frame %d; Reason: %d; SequenceID: %d; wasImgCaptured: %d]", request.toString(),
                    failure.getFrameNumber(), failure.getReason(), failure.getSequenceId(), failure.wasImageCaptured() ? 1 : 0);
            //listenerPhotoCapture.onFailed();
            super.onCaptureFailed(session, request, failure);
        }

        @Override
        public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
        }
    };

    private void captureStillPicture() throws CameraAccessException {
        //Util.log("__captureStillPicture (final): another thread? THREAD_ID: " + Thread.currentThread().getId());
        final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

        Surface surfaceTarget = imageReader.getSurface();
        captureBuilder.addTarget(surfaceTarget);
        captureBuilder.set(CaptureRequest.JPEG_QUALITY, (byte) 90);
        setCameraOrientation(captureBuilder);
        captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);// Use the same AE and AF modes as the preview.

        CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
                try {
                    captureSession.capture(previewRequestBuilder.build(), captureCallback, null);
                } catch (CameraAccessException e) {
                    close();
                    onCameraStateChangeListener.onCameraDisconnectOrError();
                    return;
                }

                captureState = STATE_PREVIEW;
            }

            @Override
            public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
                Util.log("Capture failed FINAL in MyCamera[Request: %s][Failure: Frame %d; Reason: %d; SequenceID: %d; wasImgCaptured: %d]", request.toString(),
                        failure.getFrameNumber(), failure.getReason(), failure.getSequenceId(), failure.wasImageCaptured() ? 1 : 0);

                captureState = STATE_PREVIEW;
                close();
                onPhotoCaptureListener.onFail();
                super.onCaptureFailed(session, request, failure);
            }
        };

        captureSession.capture(captureBuilder.build(), CaptureCallback, backgroundHandler);
    }

    private void runPrecaptureSequence() throws CameraAccessException {
        previewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START); // This is how to tell the camera to trigger.
        captureState = STATE_WAITING_PRECAPTURE; // Tell #mCaptureCallback to wait for the precapture sequence to be set.
        captureSession.capture(previewRequestBuilder.build(), captureCallback, backgroundHandler);
    }

    @Override
    public void close() {

        if(captureSession != null)
            captureSession.close();

        if(cameraDevice != null)
            cameraDevice.close();

        stopBackgroundThread();
    }

    private void setCameraOrientation(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.JPEG_ORIENTATION, 0);
    }

    private boolean findBackCamera() throws CameraNotAvailableException {
        try {
            for(String camera : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(camera);
                if(characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraID = camera;
                    cameraCharacteristics = characteristics;
                    return true;
                }
            }
        } catch (CameraAccessException e) {
            throw new CameraNotAvailableException();
        }
        return false;
    }

    private boolean checkForFormatExistence(int targetFormat, @NonNull int[] formats) {
        for(int format : formats) {
            if(format == targetFormat)
                return true;
        }
        return false;
    }

    @Override
    public Resolution[] getSupportedPictureSizes() {
        Size[] sizesSrc = map.getOutputSizes(IMAGE_FORMAT);
        Resolution[] resolutions = new Resolution[sizesSrc.length];

        for(int i=0; i<sizesSrc.length; i++)
            resolutions[i] = new Resolution(sizesSrc[i].getWidth(), sizesSrc[i].getHeight());

        return resolutions;
    }

    @Override
    public void setOutputSize(Resolution size) {
        this.outputSize = size;
    }
}
