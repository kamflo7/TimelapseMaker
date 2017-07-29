package com.loony.timelapsemaker.http_server;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;

import com.loony.timelapsemaker.NewActivity;
import com.loony.timelapsemaker.Util;
import com.loony.timelapsemaker.camera.CameraService;
import com.loony.timelapsemaker.camera.TimelapseConfig;

import java.io.IOException;

/**
 * Created by Kamil on 7/27/2017.
 */

public class HttpService extends Service {

    private final IBinder mBinder = new HttpService.LocalBinder();
    private HttpServer server;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent == null) {
            Util.log("HttpService::onStartCommand is null, fatal exception; have a look at this");
            stopSelf();
            return START_NOT_STICKY;
        }

        Util.log("~~HttpService started!~~");

        TimelapseConfig timelapseConfig = intent.getExtras().getParcelable(NewActivity.PARCEL_TIMELAPSE_CONFIG);

        server = new HttpServer(getApplicationContext(), 9090, timelapseConfig);
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        if(server != null)
            server.stop();

        super.onDestroy();
    }

    public class LocalBinder extends Binder {
        public HttpService getService() {
            return HttpService.this;
        }
    }

    private class Worker extends HandlerThread {
        public Handler handler;

        public Worker(String name) {
            super(name);
        }

        public synchronized void waitUntilReady() {
            handler = new Handler(getLooper());
        }
    }
}
