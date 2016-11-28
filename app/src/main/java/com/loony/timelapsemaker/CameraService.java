package com.loony.timelapsemaker;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Created by Kamil on 11/13/2016.
 */

public class CameraService extends Service {

    private static final int NOTIFICATION_ID = 1;
    private static final int NOTIFICATION_TYPE_START = 1;
    private static final int NOTIFICATION_TYPE_CAPTURE = 2;

    private final IBinder mBinder = new LocalBinder();
    private MyCamera camera;
    private Worker worker;

    public void clickSth() {
    }

    @Override
    public void onCreate() {
        super.onCreate();


        camera = new MyCamera(getApplicationContext());
        startForeground(NOTIFICATION_ID, getMyNotification(NOTIFICATION_TYPE_START, -1));
    }

    private Notification getMyNotification(int type, int additionalArg) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        String text = "";
        switch(type) {
            case NOTIFICATION_TYPE_START:
                text = "Preparing camera to capture..";
                break;
            case NOTIFICATION_TYPE_CAPTURE:
                text = "Captured " + additionalArg + " photos";
                break;
        }

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.unnamed)
                .setContentTitle("TimelapseMaker")
                .setContentText(text)
                .setContentIntent(pendingIntent).build();
        return notification;
    }

    private void updateNotificationMadePhotos(int amount) {
        Notification notification = getMyNotification(NOTIFICATION_TYPE_CAPTURE, amount);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Util.log("CameraService::onBind()!");

        Runnable runnable = new Runnable() {

            private int number = 0;
            private int amount = 20;

            private MyCamera.OnPhotoCreatedListener listener = new MyCamera.OnPhotoCreatedListener() {
                @Override
                public void onCreated() {
                    Intent i = getSendingMessageIntent(MainActivity.BROADCAST_MSG_CAPTURED_PHOTO);
                    i.putExtra(MainActivity.BROADCAST_MSG_CAPTURED_PHOTO_AMOUNT, number);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
                    updateNotificationMadePhotos(number);

                    if(number == amount) {
                        Util.log("Koniec sesji");
                        return;
                    }

                    try {
                        Thread.sleep(2000);
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

    private Intent getSendingMessageIntent(String message) {
        Intent intent = new Intent(MainActivity.BROADCAST_FILTER);
        intent.putExtra(MainActivity.BROADCAST_MSG, message);
        return intent;
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
        stopForeground(true);
        worker.quitSafely();
        super.onDestroy();
    }

    public class LocalBinder extends Binder {
        CameraService getService() {
            return CameraService.this;
        }
    }
}
