package com.loony.timelapsemaker;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

/**
 * Created by Kamil on 11/13/2016.
 */

public class CameraService extends Service {

    private final IBinder mBinder = new LocalBinder();
    private MyCamera camera;
    private Worker worker;

    public void clickSth() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.unnamed)
                .setContentTitle("My awesome TimelapseMaker!")
                .setContentText("Pretending that sth it's doing")
                .setContentIntent(pendingIntent).build();

        startForeground(5, notification);
        //Util.log("Runnable::run %s | %d", Thread.currentThread().getName(), Thread.currentThread().getId());

        camera = new MyCamera(getApplicationContext());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Util.log("CameraService::onBind()!");

        Runnable runnable = new Runnable() {

            private int number = 51;
            private int amount = 240;

            private MyCamera.OnPhotoCreatedListener listener = new MyCamera.OnPhotoCreatedListener() {
                @Override
                public void onCreated() {
                    if(number == amount) {
                        Util.log("Koniec sesji");
                        return;
                    }

                    try {
                        Thread.sleep(15000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }

                    camera.makePhoto(number++, listener);
                }
            };

            @Override
            public void run() {
                camera.makePhoto(number++, listener);
            }
        };

        worker = new Worker("WorkerThread");
        worker.start();
        worker.waitUntilReady();
        worker.handler.post(runnable);

        return mBinder;
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


    @Override
    public void onDestroy() {
        Util.log("CameraService::onDestroy()!");
        worker.quitSafely();
        super.onDestroy();
    }

    public class LocalBinder extends Binder {
        CameraService getService() {
            return CameraService.this;
        }
    }
}
