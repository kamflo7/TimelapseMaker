package com.loony.timelapsemaker;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.loony.timelapsemaker.camera.CameraV1;
import com.loony.timelapsemaker.camera.CameraV2;
import com.loony.timelapsemaker.camera.CameraService;
import com.loony.timelapsemaker.camera.OnCameraStateChangeListener;
import com.loony.timelapsemaker.camera.TimelapseConfig;
import com.loony.timelapsemaker.camera.exceptions.CameraNotAvailableException;

public class NewActivity extends AppCompatActivity {
    public static final int REQUEST_PERMISSIONS = 0x1;
    public static final String PARCEL_TIMELAPSE_CONFIG = "parcelTimelapseConfig";

    private CameraV2 cameraV2;
    private SurfaceView surfaceView;
    private ImageButton btnStartTimelapse;

    private boolean isDoingTimelapse;

    // cameraV2 service
    private boolean cameraServiceBound;
    private CameraService cameraService;
    private ServiceConnection cameraConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_new);
        surfaceView = (SurfaceView) findViewById(R.id.surface);
        btnStartTimelapse = (ImageButton) findViewById(R.id.btnStartTimelapse);

        if(!Util.checkPermissions(Util.NECESSARY_PERMISSIONS_START_APP, this)) {
            ActivityCompat.requestPermissions(this, Util.NECESSARY_PERMISSIONS_START_APP, REQUEST_PERMISSIONS);
        }
    }

    public void btnStartTimelapse(View v) {
        Intent intentCamera = new Intent(this, CameraService.class);

        if(!isDoingTimelapse) {
            cameraV2.close();
            cameraV2 = null;

            TimelapseConfig config = new TimelapseConfig();
            config.setPhotosLimit(20);
            config.setMilisecondsInterval(3000L);

            intentCamera.putExtra(PARCEL_TIMELAPSE_CONFIG, config);
            startService(intentCamera);

            cameraConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName componentName, IBinder service) {
                    CameraService.LocalBinder binder = (CameraService.LocalBinder) service;
                    cameraService = binder.getService();
                    cameraServiceBound = true;
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {
                    cameraServiceBound = false;
                }
            };
            bindService(intentCamera, cameraConnection, 0);
            isDoingTimelapse = true;
            btnStartTimelapse.setImageResource(R.drawable.stop);
            Util.log("Started timelapse");
        } else {
            stopService(intentCamera);
            unbindService(cameraConnection);
            isDoingTimelapse = false;
            btnStartTimelapse.setImageResource(R.drawable.record);
            Util.log("Stopped timelapse");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_PERMISSIONS) {
            if(grantResults.length > 0) {
                for(int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "You have to provide permissions to use app.", Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Util.log("___onStart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Util.log("___onStop");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Util.log("___onResume");

        /*surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                try {
                    cameraV2 = new CameraV2(NewActivity.this, new OnCameraStateChangeListener() {
                        @Override
                        public void onCameraOpen() {

                        }

                        @Override
                        public void onCameraDisconnectOrError() {

                        }
                    });

                    Size[] sizes = cameraV2.getAvailableSizes();
                    Size choosenSize = sizes[0];
                    cameraV2.setOutputSize(choosenSize);
                    surfaceView.getHolder().setFixedSize(choosenSize.getWidth(), choosenSize.getHeight());
                    cameraV2.openForPreview(surfaceView.getHolder().getSurface());

                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

            }
        });*/

        //todo: test
        //surfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        if(callback != null) {
            surfaceView.getHolder().removeCallback(callback);
        }

        callback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                try {
                    cameraV1 = new CameraV1();
                    cameraV1.openForPreview(surfaceHolder);
                } catch (CameraNotAvailableException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

            }
        };
        surfaceView.getHolder().addCallback(callback);
    }

    private CameraV1 cameraV1; // todo: test
    private SurfaceHolder.Callback callback; //todo: test

    @Override
    protected void onPause() {
        super.onPause();
        Util.log("___onPause");

        if(cameraV2 != null) {
            cameraV2.close();
            cameraV2 = null;
        }

        if(cameraV1 != null) {

            cameraV1.close();
            cameraV1 = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Util.log("___onDestroy");

        if(cameraServiceBound)
            unbindService(cameraConnection);
    }
}
