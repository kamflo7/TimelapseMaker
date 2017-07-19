package com.loony.timelapsemaker;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import com.loony.timelapsemaker.camera.Camera;
import com.loony.timelapsemaker.camera.OnCameraStateChangeListener;

public class NewActivity extends AppCompatActivity {
    public static final int REQUEST_PERMISSIONS = 0x1;

    private Camera camera;
    private SurfaceView surfaceView;
    private boolean surfaceCreated = false;


    public static String[] NECESSARY_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.WAKE_LOCK
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new);
        surfaceView = (SurfaceView) findViewById(R.id.surface);

        SurfaceHolder sr = NewActivity.this.surfaceView.getHolder();
        sr.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                surfaceCreated = true;
                Util.log("surfaceCreated called");
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

            }
        });

        if(!Util.checkPermissions(NECESSARY_PERMISSIONS, this)) {
            ActivityCompat.requestPermissions(this, NECESSARY_PERMISSIONS, REQUEST_PERMISSIONS);
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

    public void btnActionClick(View v) {
        if(camera != null) {
            Util.log("Close camera");
            camera.close();
            camera = null;
            return;
        }

        try {
            Util.log("Open camera");
            camera = new Camera(this, new OnCameraStateChangeListener() {
                @Override
                public void onCameraOpen() {
                    /*if(surfaceCreated) {
                        Util.log("onCameraOpen > surfaceCreated > setting surface size..");
                        Size size = camera.getSurfaceSize();
                        surfaceView.getHolder().setFixedSize(size.getWidth(), size.getHeight());
                    } else {
                        Util.log("surfaceCreated is not created :(");
                    }*/
                }

                @Override
                public void onCameraDisconnectOrError() {

                }
            });

            Size size = camera.getSurfaceSize();
            surfaceView.getHolder().setFixedSize(size.getWidth(), size.getHeight());

            camera.open(surfaceView.getHolder().getSurface());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}
