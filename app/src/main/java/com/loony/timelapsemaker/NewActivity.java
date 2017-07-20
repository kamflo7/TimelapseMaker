package com.loony.timelapsemaker;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.loony.timelapsemaker.camera.Camera;
import com.loony.timelapsemaker.camera.CameraService;
import com.loony.timelapsemaker.camera.OnCameraStateChangeListener;
import com.loony.timelapsemaker.camera.Resolution;
import com.loony.timelapsemaker.camera.TimelapseConfig;
import com.loony.timelapsemaker.camera.exceptions.CameraNotAvailableException;

public class NewActivity extends AppCompatActivity {
    public static final int REQUEST_PERMISSIONS = 0x1;
    public static final String PARCEL_TIMELAPSE_CONFIG = "parcelTimelapseConfig";

    private Camera camera;
    private SurfaceView surfaceView;
    private SurfaceHolder.Callback surfaceHolderCallback;
    private ImageButton btnStartTimelapse;

    private boolean isDoingTimelapse;

    // camera service
    private boolean cameraServiceBound;
    private CameraService cameraService;
    private ServiceConnection cameraConnection;

    private Resolution pictureSize;

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
            camera.close();
            camera = null;

            TimelapseConfig config = new TimelapseConfig();
            config.setPhotosLimit(20);
            config.setMilisecondsInterval(3000L);
            config.setPictureSize(pictureSize);

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

        if(surfaceHolderCallback != null) {
            surfaceView.getHolder().removeCallback(surfaceHolderCallback);
        }

        surfaceHolderCallback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                camera = Util.getAppropriateCamera();
                try {
                    camera.prepare(NewActivity.this);
                    Resolution[] sizes = camera.getSupportedPictureSizes();
                    Resolution choosenSize = sizes[0];
                    pictureSize = choosenSize;
                    camera.setOutputSize(choosenSize);
                    surfaceView.getHolder().setFixedSize(choosenSize.getWidth(), choosenSize.getHeight());
                    camera.openForPreview(surfaceView.getHolder());
                } catch (CameraNotAvailableException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) { }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) { }
        };

        surfaceView.getHolder().addCallback(surfaceHolderCallback);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Util.log("___onPause");

        if(camera != null) {
            camera.close();
            camera = null;
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
