package com.loony.timelapsemaker;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class HttpService extends Service {

    private final IBinder mBinder = new HttpService.LocalBinder();

    public HttpService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public class LocalBinder extends Binder {
        HttpService getService() {
            return HttpService.this;
        }
    }
}
