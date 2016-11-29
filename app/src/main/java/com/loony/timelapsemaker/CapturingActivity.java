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

public class CapturingActivity extends AppCompatActivity {

    CameraService mCameraService;
    boolean mCameraServiceBound = false;

    private TextView textViewInfo;
    private ProgressBar progressBar;
    private Button dismiss;

    private TimelapseSessionConfig timelapseSessionConfig;
    private int lastCapturedAmount;
    private float frequency;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capturing);
        initUI();

        timelapseSessionConfig = getIntent().getExtras().getParcelable("timelapseSessionConfigParcel");
        progressBar.setMax(timelapseSessionConfig.calculateFramesAmount());
        frequency = timelapseSessionConfig.calculateCaptureFrequency();
        updateTextInfo(0);

        createCameraService();
    }

    private void initUI() {
        textViewInfo = (TextView) findViewById(R.id.textViewInfo);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        dismiss = (Button) findViewById(R.id.buttonDismiss);

        dismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void updateTextInfo(int currentCaptured) {
        textViewInfo.setText(String.format("Currently captured %d photos of all %d\nEvery photo is captured every %.1f seconds\nEstimated remaining time: %d minutes",
                currentCaptured, timelapseSessionConfig.calculateFramesAmount(), frequency, calcRemainingTimeAsMinutes()));
    }

    private int calcRemainingTimeAsMinutes() {
        int difference = timelapseSessionConfig.calculateFramesAmount() - lastCapturedAmount;
        return (int) Math.ceil(difference * frequency / 60);
    }

    private void updateProgressBar(int currentCaptured) {
        progressBar.setProgress(currentCaptured);
    }

    private void createCameraService() {
        Intent intent = new Intent(this, CameraService.class);
        intent.putExtra("timelapseSessionConfigParcel", timelapseSessionConfig);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
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

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String msg = intent.getStringExtra(Util.BROADCAST_MSG);
            if(msg != null) {
//                Util.log("[MainActivity::onReceive] msg=%s", msg);
                if(msg.equals(Util.BROADCAST_MSG_CAPTURED_PHOTO)) {
                    lastCapturedAmount = intent.getIntExtra(Util.BROADCAST_MSG_CAPTURED_PHOTO_AMOUNT, -1);
                    updateTextInfo(lastCapturedAmount);
                    updateProgressBar(lastCapturedAmount);
                }
            }
        }
    };

    @Override
    protected void onResume() {
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(Util.BROADCAST_FILTER));
        super.onResume();
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if(mCameraServiceBound)
            unbindService(mConnection);
        super.onDestroy();
    }
}
