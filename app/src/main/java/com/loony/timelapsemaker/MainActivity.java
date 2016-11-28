package com.loony.timelapsemaker;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
//import android.hardware.camera2.CameraAccessException;
//import android.hardware.camera2.CameraCaptureSession;
//import android.hardware.camera2.CameraCharacteristics;
//import android.hardware.camera2.CameraDevice;
//import android.hardware.camera2.CameraManager;
//import android.hardware.camera2.CaptureFailure;
//import android.hardware.camera2.CaptureRequest;
//import android.hardware.camera2.CaptureResult;
//import android.hardware.camera2.TotalCaptureResult;
//import android.hardware.camera2.params.StreamConfigurationMap;
//import android.media.Image;
//import android.media.ImageReader;
////import android.os.Environment;
//import android.util.Size;
//import android.util.SizeF;
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.nio.ByteBuffer;
//import java.util.ArrayList;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.view.Surface;
import android.view.View;
import android.widget.TextView;

import com.loony.timelapsemaker.test.CameraIntentService;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static String BROADCAST_FILTER = "com.loony.timelapsemaker.nyancat";
    public static String BROADCAST_MSG = "message";
    public static String BROADCAST_MSG_CAPTURED_PHOTO = "capturedPhoto";
    public static String BROADCAST_MSG_CAPTURED_PHOTO_AMOUNT = "capturedPhotoAmount";

    private static final int REQUEST_PERMISSIONS = 0x1;

    private String[] permissionsNedded = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    CameraService mService;
    boolean mBound = false;

    private TextView textViewHello;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textViewHello = (TextView) findViewById(R.id.textView);

        if(!checkPermissions())
            makePermissions();
        else
            afterCheckPermission();
    }

    public void btnClick(View view) {
        if(mBound) {
            mService.clickSth();
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            CameraService.LocalBinder binder = (CameraService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };

    private void afterCheckPermission() {
        Intent intent = new Intent(this, CameraService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String msg = intent.getStringExtra(BROADCAST_MSG);
            if(msg != null) {
//                Util.log("[MainActivity::onReceive] msg=%s", msg);
                if(msg.equals(BROADCAST_MSG_CAPTURED_PHOTO))
                    textViewHello.setText("Currently captured " + intent.getIntExtra(BROADCAST_MSG_CAPTURED_PHOTO_AMOUNT, -1) + " photos");
            }
        }
    };

    private boolean checkPermissions() {
//        Util.log("MainActivity::checkPermissions()");
        for(String permission : permissionsNedded) {
            int permissionCheckResult = ContextCompat.checkSelfPermission(this, permission);
            Util.log("Permission check for camera: " + (permissionCheckResult == PermissionChecker.PERMISSION_DENIED ? "DENIED" : "GRANTED"));
            if(permissionCheckResult == PermissionChecker.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    private void makePermissions() {
        ActivityCompat.requestPermissions(this, permissionsNedded, REQUEST_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        Util.log("MainActivity::onRequestPermissionsResult()");
        if(requestCode == REQUEST_PERMISSIONS) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Util.log("You've got permission!");
                afterCheckPermission();
            }
        }
    }

    @Override
    protected void onResume() {
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(BROADCAST_FILTER));
        super.onResume();
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();

        Util.log("Activity::onStop");
    }

    @Override
    protected void onDestroy() {
        Util.log("Activity::onDestroy");
        if(mBound)
            unbindService(mConnection);

        super.onDestroy();
    }
}
