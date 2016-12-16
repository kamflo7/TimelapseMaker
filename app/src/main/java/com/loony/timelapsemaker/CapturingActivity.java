package com.loony.timelapsemaker;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.loony.timelapsemaker.camera.CameraService;
import com.loony.timelapsemaker.camera.TimelapseConfig;
import com.loony.timelapsemaker.http_server.*;

import java.util.Locale;

public class CapturingActivity extends AppCompatActivity {
    private static final String INSTANCE_STATE_TIMELAPSE_CONFIG = "timelapseconfig";
    private static final float CAPTURE_PHOTO_DEFAULT_AVERAGE_DURATION_SECONDS = 0.5f;

    private CameraService mCameraService;
    private boolean mCameraServiceBound = false;
    private HttpService mHttpService;
    private boolean mHttpServiceBound = false;

    private TextView textViewInfo;
    private ProgressBar progressBar;
    private Button dismiss;

    private TimelapseConfig timelapseConfig;

    private int receiveCapturedPhotosAmount = 0;
    private float receiveCapturePhotoAverageDurationSeconds = CAPTURE_PHOTO_DEFAULT_AVERAGE_DURATION_SECONDS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capturing);
        initUI();
//        Util.logEx("lifecycle", "CapturingActivity:onCreate(Bundle %s)", savedInstanceState != null ? "exists" : "doesn't exist");

        Intent intentCamera = new Intent(this, CameraService.class);
        Intent intentHttp = new Intent(this, HttpService.class);

        if(savedInstanceState == null) { // TL;DR: ONLY ON FIRST START; assumes that null is only once, when start. Later if something crashes Activity, here should be something
            timelapseConfig = getIntent().getExtras().getParcelable(MainActivity.PARCEL_TIMELAPSE_CONFIG);

            intentCamera.putExtra(MainActivity.PARCEL_TIMELAPSE_CONFIG, timelapseConfig);
            startService(intentCamera);
            bindService(intentCamera, mConnection, 0);

            intentHttp.putExtra(MainActivity.PARCEL_TIMELAPSE_CONFIG, timelapseConfig);
            startService(intentHttp);
            bindService(intentHttp, mConnectionHttp, 0);
        } else {
            timelapseConfig = savedInstanceState.getParcelable(INSTANCE_STATE_TIMELAPSE_CONFIG);
            bindService(intentCamera, mConnection, 0);
            bindService(intentHttp, mConnectionHttp, 0);
        }

        progressBar.setMax(timelapseConfig.getPhotosAmount());
        updateInformationUI();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(INSTANCE_STATE_TIMELAPSE_CONFIG, timelapseConfig);
    }

    private void initUI() {
        textViewInfo = (TextView) findViewById(R.id.textViewInfo);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        dismiss = (Button) findViewById(R.id.buttonDismiss);

        dismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mCameraServiceBound)
                    unbindService(mConnection);
                if(mHttpServiceBound)
                    unbindService(mConnectionHttp);

                mCameraServiceBound = false;
                mHttpServiceBound = false;
                stopService(new Intent(CapturingActivity.this, CameraService.class));
                stopService(new Intent(CapturingActivity.this, HttpService.class));
                finish();
            }
        });
    }

    private void updateInformationUI() {
        textViewInfo.setText(String.format(Locale.ENGLISH, "Currently captured %d photos of all %d\nEvery photo is captured every %.1f seconds\nEstimated approximately remaining time: %s\nAverage duration of capture photo: %.1fs",
            receiveCapturedPhotosAmount,
            timelapseConfig.getPhotosAmount(),
            timelapseConfig.getFrequencyCaptureMiliseconds()/1000f,
                Util.secondsToTime(timelapseConfig.calculator.getTotalSecondsTimeToCaptureAll(timelapseConfig.getPhotosAmount()-receiveCapturedPhotosAmount, (long) receiveCapturePhotoAverageDurationSeconds * 1000)),
            receiveCapturePhotoAverageDurationSeconds));

        progressBar.setProgress(receiveCapturedPhotosAmount);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            CameraService.LocalBinder binder = (CameraService.LocalBinder) service;
            mCameraService = binder.getService();
            mCameraServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mCameraServiceBound = false;
        }
    };

    private ServiceConnection mConnectionHttp = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            HttpService.LocalBinder binder = (HttpService.LocalBinder) service;
            mHttpService = binder.getService();
            mHttpServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mHttpServiceBound = false;
        }
    };

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String msg = intent.getStringExtra(CameraService.BROADCAST_MESSAGE);
            if(msg != null) {
                if(msg.equals(CameraService.BROADCAST_MSG_CAPTURED_PHOTO)) {
                    receiveCapturedPhotosAmount = intent.getIntExtra(CameraService.BROADCAST_MSG_CAPTURED_PHOTO_AMOUNT, -1);
                    receiveCapturePhotoAverageDurationSeconds = intent.getFloatExtra(CameraService.BROADCAST_MSG_CAPTURED_PHOTO_DURATION_MS, 500f) / 1000f;
                    updateInformationUI();
                } else if(msg.equals(CameraService.BROADCAST_MSG_FINISH)) {
                    Toast.makeText(CapturingActivity.this, "Capturing photos has been finished", Toast.LENGTH_LONG).show();
                }
            }
        }
    };

    @Override
    protected void onResume() {
//        Util.logEx("lifecycle", "CapturingActivity::onResume()");

//        if(serviceMadeAtLeastOnePhoto && !Util.isMyServiceRunning(this, CameraService.class)) {
//            Toast.makeText(CapturingActivity.this, "Capturing photos has been finished", Toast.LENGTH_LONG).show();
//            updateInformationUIFinished();
//        }
//
//        if(mCameraServiceBound)
//            getServiceDataAndUpdate();

        Toast.makeText(this, "Please wait to get updated values", Toast.LENGTH_LONG).show();

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(CameraService.BROADCAST_FILTER));
        super.onResume();
    }

    @Override
    protected void onPause() {
//        Util.logEx("lifecycle", "CapturingActivity::onPause()");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Util.logEx("lifecycle", "CapturingActivity::onDestroy()");
        if(mCameraServiceBound)
            unbindService(mConnection);

        if(mHttpServiceBound)
            unbindService(mConnectionHttp);

        super.onDestroy();
    }
}
