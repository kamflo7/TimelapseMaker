package com.loony.timelapsemaker.http_server;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import com.loony.timelapsemaker.MainActivity;
import com.loony.timelapsemaker.camera.CameraService;
import com.loony.timelapsemaker.camera.TimelapseConfig;
import com.loony.timelapsemaker.Util;

import java.io.IOException;

public class HttpService extends Service {

    private final IBinder mBinder = new HttpService.LocalBinder();
    private MyServerExample server;
    private TimelapseConfig timelapseSessionConfig;

    public HttpService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent == null) {
            Util.log("HttpService::onStartCommand is null, fatal exception; have a look at this");
            stopSelf();
            return START_NOT_STICKY;
        }

        timelapseSessionConfig = intent.getExtras().getParcelable(MainActivity.PARCEL_TIMELAPSE_CONFIG);

        try {
            server = new MyServerExample(getApplicationContext(), timelapseSessionConfig);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Util.log("HttpService::onStartCommand; probably started http server");
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(CameraService.BROADCAST_FILTER));
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public HttpService getService() {
            return HttpService.this;
        }
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String msg = intent.getStringExtra(CameraService.BROADCAST_MESSAGE);
            if(msg != null) {
                if(msg.equals(CameraService.BROADCAST_MSG_CAPTURED_PHOTO)) {
                    server.setReceiveCapturedPhotosAmount(intent.getIntExtra(CameraService.BROADCAST_MSG_CAPTURED_PHOTO_AMOUNT, -1));
                    server.setReceiveCapturePhotoAverageDurationSeconds(intent.getFloatExtra(CameraService.BROADCAST_MSG_CAPTURED_PHOTO_DURATION_MS, 500f) / 1000f);
                }
            }
        }
    };

    private class Worker extends HandlerThread {
        public Handler handler;

        public Worker(String name) {
            super(name);
        }

        public synchronized void waitUntilReady() {
            handler = new Handler(getLooper());
        }
    }

    @Override
    public void onDestroy() {
        server.stop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }
}
