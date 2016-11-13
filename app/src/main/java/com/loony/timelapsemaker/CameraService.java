package com.loony.timelapsemaker;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

/**
 * Created by Kamil on 11/13/2016.
 */

public class CameraService extends Service {

    private final IBinder mBinder = new LocalBinder();

    private int number = 10;
    private MyCamera camera;

    public void clickSth() {
        camera.makeAPhoto(number++);

//        MyCamera c = new MyCamera(getApplicationContext());
//        c.makeAPhoto(number++);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Util.log("CameraService::onBind()!");
        camera = new MyCamera(getApplicationContext());
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Util.log("CameraService::onUnbind()!");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Util.log("CameraService::onDestroy()!");
        super.onDestroy();
    }

    public class LocalBinder extends Binder {
        CameraService getService() {
            return CameraService.this;
        }
    }

}
