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
import android.widget.Toast;

import com.loony.timelapsemaker.CameraService;
import com.loony.timelapsemaker.CapturingActivity;
import com.loony.timelapsemaker.TimelapseSessionConfig;
import com.loony.timelapsemaker.Util;

import java.io.IOException;

public class HttpService extends Service {

    private final IBinder mBinder = new HttpService.LocalBinder();
    private MyServerExample server;
    private TimelapseSessionConfig timelapseSessionConfig;

    public HttpService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent == null) {
            Util.log("HttpService::onStartCommand is null, fatal exception; have a look at this");
            stopSelf();
            return START_NOT_STICKY;
        }

        timelapseSessionConfig = intent.getExtras().getParcelable("timelapseSessionConfigParcel");

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
            String msg = intent.getStringExtra(CameraService.BROADCAST_MSG);
            if(msg != null) {
                Util.log("[CapturingActivity::onReceive] msg=%s", msg);
                if(msg.equals(CameraService.BROADCAST_MSG_CAPTURED_PHOTO)) {
                    int lastCapturedAmount = intent.getIntExtra(CameraService.BROADCAST_MSG_CAPTURED_PHOTO_AMOUNT, -1);
                    long avgAFtime = intent.getLongExtra(CameraService.BROADCAST_MSG_AF_AVG_TIME, CameraService.DEFAULT_AVERAGE_AF_TIME);
                    HttpService.this.server.setCapturedPhotoAmount(lastCapturedAmount);
                    HttpService.this.server.setAFAverageTime(avgAFtime);
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
