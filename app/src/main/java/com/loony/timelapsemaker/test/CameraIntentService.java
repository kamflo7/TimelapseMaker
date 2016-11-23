package com.loony.timelapsemaker.test;

import android.app.IntentService;
import android.content.Intent;

import com.loony.timelapsemaker.MyCamera;
import com.loony.timelapsemaker.Util;

import java.util.Random;

/**
 * Created by Kamil on 11/23/2016.
 */

public class CameraIntentService extends IntentService {

    public CameraIntentService() {
        super("CameraIntentService hello");
    }

    @Override
    public void onCreate() {
        Util.log("CameraIntentService::onCreate %s", this.toString());
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Util.log("CameraIntentService::onStartCommand %s | %d", intent.toString(), startId);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Random r = new Random();
        try {
            Thread.sleep(r.nextInt(500) + 100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Util.log("CameraIntentService::onHandleIntent!! %s", intent.getExtras().getCharSequence("special"));
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        Util.log("CameraIntentService::onDestroy %s", this.toString());
        super.onDestroy();
    }
}
