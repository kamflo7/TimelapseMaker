package com.loony.timelapsemaker;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
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
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Size;
import android.util.SizeF;
import android.view.Surface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private String[] permissionsNedded = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private static final int REQUEST_PERMISSIONS = 0x1;
    private static final int IMAGE_FORMAT = 256; // JPEG
    private List<Surface> surfaces;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(!checkPermissions())
            makePermissions();
        else
            cameraProcedure();

    }

    private void cameraProcedure() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameras = manager.getCameraIdList();


            if(cameras != null) {
                for(String camera : cameras) {
                    CameraCharacteristics characteristics = manager.getCameraCharacteristics(camera);
//                    printDebugInfoAboutCameras(characteristics);

                    if(characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                        Util.log("Rear camera has been found, trying to connect..");
                        openCamera(manager, camera, characteristics);
                    }
                }
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera(CameraManager manager, String camera, CameraCharacteristics cameraCharacteristics) {
        final CameraCharacteristics _cameraCharacteristics = cameraCharacteristics;

        try {
            manager.openCamera(camera, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    Util.log("openCamera() onOpened");

                    createCaptureSession(camera, _cameraCharacteristics);
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
        }
        catch(SecurityException e) {
            Util.log("SecurityException catched");
        }
        catch(CameraAccessException e) {
            Util.log("CameraAccessException catched");
        }
    }

    private void createCaptureSession(final CameraDevice camera, CameraCharacteristics cameraCharacteristics) {
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

        ImageReader reader = ImageReader.newInstance(sizes[0].getWidth(), sizes[0].getHeight(), IMAGE_FORMAT, 1);
        reader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Util.log("createCaptureSession::ImageReader->OnImageAvailable --> saveImageToDisk");
                Image img = reader.acquireLatestImage();
                saveImageToDisk(img);
            }
        }, null);
        Surface surface = reader.getSurface();
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

    private void saveImageToDisk(Image image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        File photo = new File(Environment.getExternalStorageDirectory(), "myPhoto.jpg");

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

    private void capturePhoto(CameraCaptureSession session, final CameraDevice camera) {
        try {
            CaptureRequest.Builder builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_MANUAL);
            builder.addTarget(surfaces.get(0));
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

    private void printDebugInfoAboutCameras(CameraCharacteristics characteristics) {
        boolean flash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        SizeF sizeF = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
        int lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);

        String result = String.format("[Camera] Flash: %s; Size: %f/%f; LensFacing: %s",
                flash?"YES":"NO",
                sizeF.getWidth(), sizeF.getHeight(),
                lensFacing == CameraCharacteristics.LENS_FACING_BACK ? "BACK" : (lensFacing == CameraCharacteristics.LENS_FACING_FRONT ? "FRONT" : "OTHER"));

        Util.log(result);
    }

    private boolean checkPermissions() {
        for(String permission : permissionsNedded) {
            int permissionCheckResult = ContextCompat.checkSelfPermission(this, permission);
            Util.log("Permission check for camera: " + (permissionCheckResult == PermissionChecker.PERMISSION_DENIED ? "DENIED" : "GRANTED"));
            if(permissionCheckResult == PermissionChecker.PERMISSION_DENIED) {
                return false;
            }
        }
//        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
//        Util.log("Permission check for camera: " + (permissionCheck == PermissionChecker.PERMISSION_DENIED ? "DENIED" : "GRANTED"));
//        if(permissionCheck == PermissionChecker.PERMISSION_DENIED) {
//            return false;
//        }

        return true;
    }

    private void makePermissions() {
        ActivityCompat.requestPermissions(this, permissionsNedded, REQUEST_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_PERMISSIONS) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Util.log("You've got permission!");
                cameraProcedure();
            }
        }
    }
}
