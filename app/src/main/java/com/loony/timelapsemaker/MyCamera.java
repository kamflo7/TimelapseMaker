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
import android.util.Size;
import android.view.Surface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

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

    private List<Surface> surfaces;
    private SurfaceTexture dummyPreview = new SurfaceTexture(1);
    private int currentNumberPhoto;

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

    public void makeAPhoto(int number) {
        if(!cameraFound) return;
        currentNumberPhoto = number;

        try {
            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    Util.log("openCamera() onOpened");
                    createCaptureSession(camera);
                }

                @Override
                public void onDisconnected(CameraDevice camera) {
                    Util.log("openCamera() onDisconnected");
                }

                @Override
                public void onError(CameraDevice camera, int error) {
                    Util.log("openCamera() got onError: " + error);
                }
            }, null);
        } catch(SecurityException e) {
            Util.log("SecurityException catched");
        }
        catch(CameraAccessException e) {
            Util.log("CameraAccessException catched");
        }
    }

    private void createCaptureSession(final CameraDevice camera) {
        surfaces = new ArrayList<>();

        StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        int[] formats = map.getOutputFormats();

        if(formats.length == 0) {
            Util.log("Zero dostepnych formatow, fatal error");
            return;
        }

        boolean foundValidFormat = false;
        for(int format : formats) {
//            Util.log("Format Dec: %d | Hex: %d", format, Integer.valueOf(String.valueOf(format), 16));
            if(format == IMAGE_FORMAT)
                foundValidFormat = true;
        }
        if(!foundValidFormat) {
            Util.log("Did not find valid format (JPEG)");
            return;
        }

        Size[] sizes = map.getOutputSizes(IMAGE_FORMAT);
        if(sizes.length == 0) {
            Util.log("Zero dostepnych sizeów, fatal error");
            return;
        }
        for(Size size : sizes) {
            Util.log("Size: %d/%d", size.getWidth(), size.getHeight());
        }

        ImageReader reader = ImageReader.newInstance(sizes[0].getWidth(), sizes[0].getHeight(), IMAGE_FORMAT, 2);
        reader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Util.log("createCaptureSession::ImageReader->OnImageAvailable --> saveImageToDisk");
                Image img = reader.acquireLatestImage();
                saveImageToDisk(img);
                img.close();
            }
        }, null);

        Surface surface = reader.getSurface();
//        Surface surface = new Surface(dummyPreview);
        if(surface == null) Util.log("Surface jest nullem!");

        surfaces.add(surface);

        try {
            camera.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    Util.log("openCamera()->onOpened()->onConfigured");
                    capturePhoto(session, camera);
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    Util.log("openCamera()->onOpened()->onConfigureFailed");
                }
            }, null);
        }
        catch(CameraAccessException e) {

        }
    }

    // #4
    private void capturePhoto(CameraCaptureSession session, final CameraDevice camera) {
        try {
            CaptureRequest.Builder builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            builder.addTarget(surfaces.get(0));
//            builder.set(CaptureRequest.JPEG_GPS_LOCATION, null);
//            builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
//            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
//            builder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO);
            builder.set(CaptureRequest.JPEG_QUALITY, (byte) 90);
            CaptureRequest captureRequest = builder.build();

            Util.log("Robie foto!");

            session.capture(captureRequest, new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                    Util.log("onCaptureStarted!");
                }

                @Override
                public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
                    Util.log("onCaptureProgressed!");
                }

                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    session.close();
                    Util.log("Zrobiono foto!");
                }

                @Override
                public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
                    Util.log("onCaptureFailed!");
                }

                @Override
                public void onCaptureSequenceCompleted(CameraCaptureSession session, int sequenceId, long frameNumber) {
                    Util.log("onCaptureSequenceCompleted!");
                }

                @Override
                public void onCaptureSequenceAborted(CameraCaptureSession session, int sequenceId) {
                    Util.log("onCaptureSequenceAborted!");
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        //session.capture
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
