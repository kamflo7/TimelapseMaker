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
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.loony.timelapsemaker.Util;
import com.loony.timelapsemaker.camera.exceptions.CameraNotAvailableException;

import java.util.Arrays;

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

        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
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

        dummySurface = new SurfaceTexture(10);
        dummySurface.setDefaultBufferSize(outputSize.getWidth(), outputSize.getHeight());
        //previewSurface = new Surface(dummySurface);
        previewSurface = surfaceHolder.getSurface();
        startBackgroundThread();

        imageReader = ImageReader.newInstance(outputSize.getWidth(), outputSize.getHeight(), IMAGE_FORMAT, 2);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader imageReader) {
                Image image = imageReader.acquireLatestImage();
                image.close();
                Util.log("OnImageAvailableListener called");
            }
        }, null);

        try {
            cameraManager.openCamera(cameraID, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice cameraDevice) {
                    CameraImplV2.this.cameraDevice = cameraDevice;

                    try {
                        createCameraPreviewSession();
                    } catch (CameraAccessException e) {
                        close();
                        onCameraStateChangeListener.onCameraDisconnectOrError();
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                    cameraDevice.close();
                    CameraImplV2.this.cameraDevice = null;
                    onCameraStateChangeListener.onCameraDisconnectOrError();
                }

                @Override
                public void onError(@NonNull CameraDevice cameraDevice, int i) {
                    cameraDevice.close();
                    CameraImplV2.this.cameraDevice = null;
                    onCameraStateChangeListener.onCameraDisconnectOrError();
                }
            }, null);
        } catch(CameraAccessException | SecurityException e) {
            throw new CameraNotAvailableException();
        }
    }

    public void openForCapturing(OnCameraStateChangeListener onCameraStateChangeListener) {
        this.onCameraStateChangeListener = onCameraStateChangeListener;
        //todo: do it
    }

    private void createCameraPreviewSession() throws CameraAccessException {
        previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        previewRequestBuilder.addTarget(previewSurface);
        setCameraOrientation(previewRequestBuilder);

        cameraDevice.createCaptureSession(Arrays.asList(previewSurface, imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                CameraImplV2.this.captureSession = cameraCaptureSession;
                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                previewRequest = previewRequestBuilder.build();

                //startedRepeatedRequest = false;
                try {
                    captureSession.setRepeatingRequest(previewRequest, captureCallback, backgroundHandler);
                    onCameraStateChangeListener.onCameraOpen(); // todo: Not sure whether this is a best place
                } catch (CameraAccessException e) {
                    close();
                    onCameraStateChangeListener.onCameraDisconnectOrError();
                }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                close();
                onCameraStateChangeListener.onCameraDisconnectOrError();
            }
        }, null);
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

                    break;
                }
                case STATE_WAITING_PRECAPTURE: {

                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {

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

    @Override
    public void close() {
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
