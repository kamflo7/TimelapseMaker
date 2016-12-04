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

import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.loony.timelapsemaker.http_server.MyServerExample;

import java.io.IOException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 0x1;

    private String[] permissionsNedded = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.WAKE_LOCK
    };
    private boolean permissionsGranted;

    private EditText editTextFps, editTextOutputTime, editTextInputTime, editTextFileNaming;
    private TextView textViewResult;
    private TimelapseSessionConfig timelapseSessionConfig = new TimelapseSessionConfig();


    // HttpService
//    private MyServerExample server; //httpd

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI();

        if(!checkPermissions())
            makePermissions();
//        else
//            afterCheckPermission();
    }

    private void initUI() {
        TextWatcher listener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                updateTimelapseSessionConfigModel();
                updateViewSessionParameters();
            }
        };

        editTextFps = (EditText) findViewById(R.id.editTextFps);
        editTextOutputTime = (EditText) findViewById(R.id.editTextOutputTime);
        editTextInputTime = (EditText) findViewById(R.id.editTextInputTime);
        editTextFileNaming = (EditText) findViewById(R.id.editTextFileNaming);
        textViewResult = (TextView) findViewById(R.id.textViewResult);
        findViewById(R.id.btnStartCapture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!permissionsGranted) {
                    Toast.makeText(MainActivity.this, "You need to provide necessary permissions!", Toast.LENGTH_LONG).show();
                    makePermissions();
                } else {
                    Intent i = new Intent(MainActivity.this, CapturingActivity.class);
                    i.putExtra("timelapseSessionConfigParcel", timelapseSessionConfig);
                    startActivity(i);
                }
            }
        });

        editTextFps.addTextChangedListener(listener);
        editTextOutputTime.addTextChangedListener(listener);
        editTextInputTime.addTextChangedListener(listener);
        editTextFileNaming.addTextChangedListener(listener);

        updateTimelapseSessionConfigModel();
        updateViewSessionParameters();
    }

    private void updateTimelapseSessionConfigModel() {
        try {
            timelapseSessionConfig.fps = Integer.parseInt(editTextFps.getText().toString());
            timelapseSessionConfig.inputMinutes = Integer.parseInt(editTextInputTime.getText().toString());
            timelapseSessionConfig.outputSeconds = Integer.parseInt(editTextOutputTime.getText().toString());
            timelapseSessionConfig.photoStartIdx = Integer.parseInt(editTextFileNaming.getText().toString());
        }
        catch (NumberFormatException e) {
            Toast.makeText(this, "Number format exception! :<", Toast.LENGTH_LONG).show();
        }
    }

    private void updateViewSessionParameters() {
        textViewResult.setText(String.format("It will take %d frames and photo will be capturing every %.1f second",
                timelapseSessionConfig.calculateFramesAmount(), timelapseSessionConfig.calculateCaptureFrequency()));
    }

    private boolean checkPermissions() {
        for(String permission : permissionsNedded) {
            int permissionCheckResult = ContextCompat.checkSelfPermission(this, permission);
            Util.log("Permission check for camera: " + (permissionCheckResult == PermissionChecker.PERMISSION_DENIED ? "DENIED" : "GRANTED"));
            if(permissionCheckResult == PermissionChecker.PERMISSION_DENIED) {
                return false;
            }
        }
        permissionsGranted = true;
        return true;
    }

    private void makePermissions() {
        ActivityCompat.requestPermissions(this, permissionsNedded, REQUEST_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_PERMISSIONS) {
            if(grantResults.length > 0) {
                permissionsGranted = true;
                for(int grantResult : grantResults)
                    if(grantResult != PackageManager.PERMISSION_GRANTED)
                        permissionsGranted = false;

                Util.log("You've got permissions!");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
//        try {
//            server = new MyServerExample(this); //httpd
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public void onPause() {
        super.onPause();
//        if(server != null) {
//            server.stop(); //httpd
//        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        Util.log("Activity::onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
