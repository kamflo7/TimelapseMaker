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
import android.os.PowerManager;
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
    private TimelapseSessionConfig timelapseSessionConfig;
    private int calculatedFrequencySleep;

    private PowerManager powerManager; // todo usunac to potem; a nawet nie, bo trzeba wakeLocka zainstancjonowac :D
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        Util.log("CameraService::onCreate");
        camera = new MyCamera(getApplicationContext());

        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        startForeground(NOTIFICATION_ID, getMyNotification(NOTIFICATION_TYPE_START, -1));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        timelapseSessionConfig = intent.getExtras().getParcelable("timelapseSessionConfigParcel");
        calculatedFrequencySleep = (int) (timelapseSessionConfig.calculateCaptureFrequency() * 1000);
        Util.log("CameraService::onStartCommand [freq(sec): %d]", calculatedFrequencySleep);

        Runnable runnable = new Runnable() {
            private int number;
            private int amount = timelapseSessionConfig.calculateFramesAmount();
//            private long time;

            private MyCamera.OnPhotoCreatedListener listener = new MyCamera.OnPhotoCreatedListener() {
                @Override
                public void onCreated() {
//                    time = System.currentTimeMillis();
                    Intent i = getSendingMessageIntent(Util.BROADCAST_MSG_CAPTURED_PHOTO);
                    i.putExtra(Util.BROADCAST_MSG_CAPTURED_PHOTO_AMOUNT, number);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
                    updateNotificationMadePhotos(number);

                    if(number == amount) {
                        Util.log("Koniec sesji");
                        if(wakeLock.isHeld())
                            wakeLock.release();
                        CameraService.this.stopSelf();
                        return;
                    }

                    try {
                        Thread.sleep(calculatedFrequencySleep);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }

//                    Util.logEx("testTime", "Time between saved photo and call to make new photo: %d ms [Should be %d][ScreenOn: %s]",
//                            System.currentTimeMillis()-time, calculatedFrequencySleep,
//                            powerManager.isInteractive() ? "Yep" : "Nope");
                    camera.makePhoto(number++, listener);
                }
            };

            @Override
            public void run() {
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "myWakeLockTag");
                wakeLock.acquire();

                number = timelapseSessionConfig.photoStartIdx;
                camera.makePhoto(number++, listener);
            }
        };

        worker = new Worker("WorkerThread");
        worker.start();
        worker.waitUntilReady();
        worker.handler.post(runnable);

        return super.onStartCommand(intent, flags, startId);
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
                .setSmallIcon(R.mipmap.my_icon)
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
//        Util.log("CameraService::onBind[atLeastOnceBounded=%1$B][StartIdx: %d; AmountFrames: %d; FPS: %d; Frequency(s): %.1f; InputMinutes: %d; OutputSeconds: %d]", atLeastOnceBounded, timelapseSessionConfig.photoStartIdx, timelapseSessionConfig.calculateFramesAmount(), timelapseSessionConfig.fps, timelapseSessionConfig.calculateCaptureFrequency(), timelapseSessionConfig.inputMinutes, timelapseSessionConfig.outputSeconds);

//        if(!atLeastOnceBounded) {
//            timelapseSessionConfig = intent.getExtras().getParcelable("timelapseSessionConfigParcel");
//            calculatedFrequencySleep = (int) (timelapseSessionConfig.calculateCaptureFrequency() * 1000);
//
//            worker = new Worker("WorkerThread");
//            worker.start();
//            worker.waitUntilReady();
//            worker.handler.post(runnable);
//
//            atLeastOnceBounded = true;
//        }
//        return mBinder;
        return null;
    }

    private Intent getSendingMessageIntent(String message) {
        Intent intent = new Intent(Util.BROADCAST_FILTER);
        intent.putExtra(Util.BROADCAST_MSG, message);
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
        if(wakeLock.isHeld())
            wakeLock.release();

        stopForeground(true);
        worker.quit();
        camera.forceStopCameraDevice();
        super.onDestroy();
    }

    public class LocalBinder extends Binder {
        CameraService getService() {
            return CameraService.this;
        }
    }
}
