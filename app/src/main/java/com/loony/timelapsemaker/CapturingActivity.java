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
import com.loony.timelapsemaker.http_server.*;

public class CapturingActivity extends AppCompatActivity {

    CameraService mCameraService;
    boolean mCameraServiceBound = false;

    HttpService mHttpService;
    private boolean mHttpServiceBound = false;

    private TextView textViewInfo;
    private ProgressBar progressBar;
    private Button dismiss;

    private TimelapseSessionConfig timelapseSessionConfig;
    private int lastCapturedAmount;
    private float frequency;
    private boolean serviceMadeAtLeastOnePhoto = false;
    private boolean needRefreshUIAfterCrash = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capturing);
        initUI();
//        Util.logEx("lifecycle", "CapturingActivity:onCreate()");

        timelapseSessionConfig = getIntent().getExtras().getParcelable("timelapseSessionConfigParcel");
        progressBar.setMax(timelapseSessionConfig.calculateFramesAmount());
        frequency = timelapseSessionConfig.calculateCaptureFrequency();
        updateInformationUI(0, CameraService.DEFAULT_AVERAGE_AF_TIME);

        if(Util.isMyServiceRunning(this, CameraService.class) && !mCameraServiceBound) {
            needRefreshUIAfterCrash = true;
            Intent i = new Intent(this, CameraService.class);
            bindService(i, mConnection, 0);
        } else {    // start camera service
            Intent intent = new Intent(this, CameraService.class);
            intent.putExtra("timelapseSessionConfigParcel", timelapseSessionConfig);
            startService(intent);
            bindService(intent, mConnection, 0);
        }

        if(Util.isMyServiceRunning(this, HttpService.class) && !mHttpServiceBound) {
            bindService(new Intent(this, HttpService.class), mConnectionHttp, 0);
        } else {
            Intent i = new Intent(this, HttpService.class);
            startService(i);
            bindService(i, mConnectionHttp, 0);
        }
    }

    private void initUI() {
        textViewInfo = (TextView) findViewById(R.id.textViewInfo);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        dismiss = (Button) findViewById(R.id.buttonDismiss);

        dismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unbindService(mConnection);
                unbindService(mConnectionHttp);
                mCameraServiceBound = false;
                mHttpServiceBound = false;
                stopService(new Intent(CapturingActivity.this, CameraService.class));
                stopService(new Intent(CapturingActivity.this, HttpService.class));
                finish();
            }
        });
    }

    private void updateInformationUI(int currentCaptured, long afAverageTime) {
        int seconds = calcRemainingTimeAsSeconds(afAverageTime);
//        seconds += afAverageTime/1000;
        int minutes = seconds / 60;
        seconds -= minutes * 60;

        textViewInfo.setText(String.format("Currently captured %d photos of all %d\nEvery photo is captured every %.1f seconds\nEstimated remaining time: %d minutes %02d seconds",
                currentCaptured, timelapseSessionConfig.calculateFramesAmount(), frequency, minutes, seconds));
    }

    private void updateInformationUIFinished() {
        textViewInfo.setText(String.format("Currently captured %d photos of all %d\nEvery photo is captured every %.1f seconds\nEstimated remaining time: %d minutes %02d seconds",
                timelapseSessionConfig.calculateFramesAmount(), timelapseSessionConfig.calculateFramesAmount(), frequency, 0, 0));

        progressBar.setProgress(progressBar.getMax());
    }

    private int calcRemainingTimeAsSeconds(long afAverageTime) {
        int differenceFrames = timelapseSessionConfig.calculateFramesAmount() - lastCapturedAmount;
        int afAvgSec = (int) afAverageTime / 1000;
        int res = (int) Math.ceil(differenceFrames * (frequency + afAvgSec) - afAvgSec);
        return res < 0 ? 0 : res;
    }

    private void updateProgressBar(int currentCaptured) {
        progressBar.setProgress(currentCaptured);
    }

    private void getServiceDataAndUpdate() {
        lastCapturedAmount = mCameraService.getCapturedPhotos();

        updateInformationUI(mCameraService.getCapturedPhotos(), mCameraService.getAFAverageTime());
        updateProgressBar(lastCapturedAmount);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
//            Util.logEx("lifecycle", "CapturingActivity BINDING SUCCESS");
            CameraService.LocalBinder binder = (CameraService.LocalBinder) service;
            mCameraService = binder.getService();
            mCameraServiceBound = true;

            if(needRefreshUIAfterCrash) {
                needRefreshUIAfterCrash = false;
                getServiceDataAndUpdate();
            }
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
            String msg = intent.getStringExtra(CameraService.BROADCAST_MSG);
            if(msg != null) {
//                Util.log("[CapturingActivity::onReceive] msg=%s", msg);
                if(msg.equals(CameraService.BROADCAST_MSG_CAPTURED_PHOTO)) {
//                    lastCapturedAmount = intent.getIntExtra(Util.BROADCAST_MSG_CAPTURED_PHOTO_AMOUNT, -1);
                    if(mCameraServiceBound) {
                        serviceMadeAtLeastOnePhoto = true;
                        getServiceDataAndUpdate();
                    }
                } else if(msg.equals(CameraService.BROADCAST_MSG_FINISH)) {
                    Toast.makeText(CapturingActivity.this, "Capturing photos has been finished", Toast.LENGTH_LONG).show();
                    updateInformationUIFinished();
                }
            }
        }
    };

    @Override
    protected void onResume() {
//        Util.logEx("lifecycle", "CapturingActivity::onResume()");

        // Service had been making photos > CapturingActivity destroyed > Service has finished work, stop > CapturingActivity creates and resumes now:
        if(serviceMadeAtLeastOnePhoto && !Util.isMyServiceRunning(this, CameraService.class)) {
            Toast.makeText(CapturingActivity.this, "Capturing photos has been finished", Toast.LENGTH_LONG).show();
            updateInformationUIFinished();
        }

        if(mCameraServiceBound)
            getServiceDataAndUpdate();

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
//        Util.logEx("lifecycle", "CapturingActivity::onDestroy()");
        if(mCameraServiceBound)
            unbindService(mConnection);

        if(mHttpServiceBound)
            unbindService(mConnectionHttp);

        super.onDestroy();
    }
}
