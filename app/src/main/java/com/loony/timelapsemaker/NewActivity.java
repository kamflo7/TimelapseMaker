package com.loony.timelapsemaker;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
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
    //private boolean isPreviewing;

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
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(Util.BROADCAST_FILTER));

        if(savedInstanceState == null) { // FIRST START APP; OTHERWISE AFTER CRASH
            if(!Util.checkPermissions(Util.NECESSARY_PERMISSIONS_START_APP, this)) {
                ActivityCompat.requestPermissions(this, Util.NECESSARY_PERMISSIONS_START_APP, REQUEST_PERMISSIONS);
            }
        } else {
            Util.log("NewActivity::onCreate, savedInstanceState not null=crash 1/3");
            if(Util.isMyServiceRunning(this, CameraService.class)) {
                Util.log("NewActivity::onCreate, savedInstanceState not null=crash, CameraService is running 2/3");
                bindToCameraService();
                if(cameraService.getTimelapseState() == CameraService.TimelapseState.NOT_FINISHED) {
                    Util.log("NewActivity::onCreate, savedInstanceState not null=crash, CameraService is running, TimelapseController is also running 3/3");
                    isDoingTimelapse = true;
                    btnStartTimelapse.setImageResource(R.drawable.stop);
                }
            }
        }
    }

    @Override // empty
    protected void onStart() {
        super.onStart();
        Util.log("___onStart");
    }

    @Override // empty
    protected void onStop() {
        super.onStop();
        Util.log("___onStop");
    }

    @Override // just startPreview() in SurfaceView callback
    protected void onResume() {
        super.onResume();
        Util.log("___onResume");

        if(!isDoingTimelapse) {
            if (surfaceHolderCallback != null) {
                surfaceView.getHolder().removeCallback(surfaceHolderCallback);
            }

            surfaceHolderCallback = new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder surfaceHolder) {
                    startPreview(surfaceHolder);
                }

                @Override
                public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                }
            };

            surfaceView.getHolder().addCallback(surfaceHolderCallback);
        }
    }

    @Override // just stopPreview() if previewing
    protected void onPause() {
        super.onPause();
        Util.log("___onPause");

        if(camera != null) {
            stopPreview();
        }
    }

    // just startTimelapse() or stopTimelapse() call, depending on the 'isDoingTimelapse' value
    public void btnStartTimelapse(View v) {
        if(!isDoingTimelapse) {
            TimelapseConfig config = new TimelapseConfig();
            config.setPhotosLimit(2);
            config.setMilisecondsInterval(3000L);
            config.setPictureSize(pictureSize);

            startTimelapse(config);
        } else {
            stopTimelapse();
        }
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String msg = intent.getStringExtra(Util.BROADCAST_MESSAGE);
            if(msg != null) {
                if(msg.equals(Util.BROADCAST_MESSAGE_FINISHED)) {
                    Util.log("Timelapse work is done");
                    stopTimelapse();
                    startPreview(surfaceView.getHolder());
                    Toast.makeText(NewActivity.this, "Timelapse has been done", Toast.LENGTH_LONG).show();
                } else if(msg.equals(Util.BROADCAST_MESSAGE_FINISHED_FAILED)) {
                    Util.log("Timelapse work is done (but with fail)");
                    stopTimelapse();
                    startPreview(surfaceView.getHolder());
                }
            }
        }
    };

    // stopPreview(), startCameraService(), bindToCameraService(), set 'isDoingTimelapse' flag, UI: change icon
    private void startTimelapse(TimelapseConfig timelapseConfig) {
        stopPreview();
        startCameraService(timelapseConfig);
        bindToCameraService();

        isDoingTimelapse = true;
        btnStartTimelapse.setImageResource(R.drawable.stop);
        Util.log("Started timelapse");
    }

    // stopService(), unbindService(), set 'isDoingTimelapse' flag, UI: change icon
    private void stopTimelapse() {
        Intent intentCamera = new Intent(this, CameraService.class);
        stopService(intentCamera);
        unbindService(cameraConnection);
        isDoingTimelapse = false;
        btnStartTimelapse.setImageResource(R.drawable.record);
        Util.log("Trying to stop timelapse session");
    }

    private void startPreview(SurfaceHolder surfaceHolder) {
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

    private void stopPreview() {
        try {
            camera.close();
            camera = null;
        } catch(NullPointerException e) {}
    }

    private void startCameraService(TimelapseConfig timelapseConfig) {
        Intent intentCamera = new Intent(this, CameraService.class);
        intentCamera.putExtra(PARCEL_TIMELAPSE_CONFIG, timelapseConfig);
        startService(intentCamera);
    }

    private void bindToCameraService() {
        Intent intentCamera = new Intent(this, CameraService.class);
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
    }

    private void stopCameraService() {
        Intent intentCamera = new Intent(this, CameraService.class);
        stopService(intentCamera);
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Util.log("___onDestroy");

        if(isDoingTimelapse) {
            stopCameraService();
            isDoingTimelapse = false;
        }

        if(cameraServiceBound)
            unbindService(cameraConnection);
    }
}
