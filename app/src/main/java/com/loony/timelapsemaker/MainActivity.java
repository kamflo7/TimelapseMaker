package com.loony.timelapsemaker;

import android.Manifest;
import android.content.Intent;
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
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.loony.timelapsemaker.camera.TimelapseConfig;

public class MainActivity extends AppCompatActivity {
    public static final String PARCEL_TIMELAPSE_CONFIG = "timelapseSessionConfigParcel";
    private static final int REQUEST_PERMISSIONS = 0x1;

    public static String[] NECESSARY_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.WAKE_LOCK
    };

    private EditText editTextPhotosAmount, editTextFrequencySeconds, editTextOutputSeconds;
    private TextView textViewResult;
    private TimelapseConfig timelapseConfig = new TimelapseConfig();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI();

        Util.logEx("lifecycle", "MainActivity::onCreate(Bundle %s);", savedInstanceState != null ? "exists" : "doesn't exist");

        if(savedInstanceState == null && !Util.checkPermissions(NECESSARY_PERMISSIONS, this))
            makePermissions(NECESSARY_PERMISSIONS);
    }

    public void testCapture(View v) { // for test

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

        editTextPhotosAmount = (EditText) findViewById(R.id.editTextPhotosAmount);
        editTextFrequencySeconds = (EditText) findViewById(R.id.editTextFrequencySeconds);
        editTextOutputSeconds = (EditText) findViewById(R.id.editTextOutputSeconds);
        textViewResult = (TextView) findViewById(R.id.textViewResult);
        findViewById(R.id.btnStartCapture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!Util.checkPermissions(NECESSARY_PERMISSIONS, MainActivity.this)) {
                    makePermissions(NECESSARY_PERMISSIONS);
                } else {
                    Intent i = new Intent(MainActivity.this, CapturingActivity.class);
                    i.putExtra(PARCEL_TIMELAPSE_CONFIG, timelapseConfig);
                    startActivity(i);
                }
            }
        });

        editTextPhotosAmount.addTextChangedListener(listener);
        editTextFrequencySeconds.addTextChangedListener(listener);
        editTextOutputSeconds.addTextChangedListener(listener);

        updateTimelapseSessionConfigModel();
        updateViewSessionParameters();
    }

    private void updateTimelapseSessionConfigModel() {
        try {
            timelapseConfig.setPhotosAmount(Integer.parseInt(editTextPhotosAmount.getText().toString()));
            timelapseConfig.setFrequencyCaptureMiliseconds(Integer.parseInt(editTextFrequencySeconds.getText().toString()) * 1000L);
        }
        catch (NumberFormatException e) {
            Toast.makeText(this, "Number format exception! :<", Toast.LENGTH_LONG).show();
        }
    }

    private void updateViewSessionParameters() {
        int outputSeconds;
        try {
            outputSeconds = Integer.parseInt(editTextOutputSeconds.getText().toString());
        } catch(NumberFormatException e) {
            return;
        }

        float fps = timelapseConfig.calculator.getFps(outputSeconds);
        int totalSecondsMin = timelapseConfig.calculator.getTotalSecondsTimeToCaptureAll(500L);
        int totalSecondsMax = timelapseConfig.calculator.getTotalSecondsTimeToCaptureAll(1000L);

        textViewResult.setText(String.format("It will take approximately from %s to %s to capture and will be %.1f fps",
            Util.secondsToTime(totalSecondsMin), Util.secondsToTime(totalSecondsMax), fps));
    }

    private void makePermissions(String[] permissions) {
        ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS);
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
    protected void onStop() {
        super.onStop();
        Util.logEx("lifecycle", "MainActivity::onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Util.logEx("lifecycle", "MainActivity::onDestroy");
    }
}
