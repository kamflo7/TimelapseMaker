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

    public static String BROADCAST_FILTER = "com.loony.timelapsemaker.CameraService";
    public static String BROADCAST_MSG = "message";
    public static String BROADCAST_MSG_CAPTURED_PHOTO = "capturedPhoto";
    public static String BROADCAST_MSG_AF_AVG_TIME = "afavgtime";
    public static String BROADCAST_MSG_CAPTURED_PHOTO_AMOUNT = "capturedPhotoAmount";
    public static String BROADCAST_MSG_FINISH = "finish";

    private static final int NOTIFICATION_ID = 1;
    private static final int NOTIFICATION_TYPE_START = 1;
    private static final int NOTIFICATION_TYPE_CAPTURE = 2;

    public static final long DEFAULT_AVERAGE_AF_TIME = 3000;

    private final IBinder mBinder = new LocalBinder();
    private MyCamera camera;
    private Worker worker;
    private TimelapseSessionConfig timelapseSessionConfig;
    private int calculatedFrequencySleep;

    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;

    private long afAverageTime = DEFAULT_AVERAGE_AF_TIME;
    private int capturedPhotos;


    public long getAFAverageTime() {
        return afAverageTime;
    }

    public int getCapturedPhotos() {
        return capturedPhotos;
    }

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
        if(intent == null) {
            Util.log("CameraService::onStartCommand is null, fatal exception; have a look at this");
            stopSelf();
            return START_NOT_STICKY;
        }

        timelapseSessionConfig = intent.getExtras().getParcelable("timelapseSessionConfigParcel");
        calculatedFrequencySleep = (int) (timelapseSessionConfig.captureFrequency * 1000);
//        Util.log("CameraService::onStartCommand [freq(sec): %d]", calculatedFrequencySleep);

        Runnable runnable = new Runnable() {
            private int number;
            private int amount = timelapseSessionConfig.framesAmount;
            private long autoFocusTimeStart;
            private static final int AF_TIME_ACCURACY = 3;
            private long[] autoFocusTimeAverage = new long[AF_TIME_ACCURACY];

            private long calculateAverageAutoFocusTime() {
                int divider = 0;
                long sum = 0;

                for(long time : autoFocusTimeAverage) {
                    if(time != -1) {
                        divider++;
                        sum += time;
                    }
                }
                return divider == 0 ? DEFAULT_AVERAGE_AF_TIME : (long) (sum/(float)divider);
            }

            private MyCamera.OnPhotoCreatedListener listener = new MyCamera.OnPhotoCreatedListener() {
                private int failsInARowCounter = 0;

                @Override
                public void onFailed() {
                    failsInARowCounter++;

                    if (failsInARowCounter == 2) {
                        Util.log("failsInARowCounter == 2, NOTHING TO DO HERE! Aborting everything..");
                        if (wakeLock.isHeld())
                            wakeLock.release();
                        CameraService.this.stopSelf();
                        return;
                    }

                    Util.log("Failed to create photo. Trying again in a few seconds..");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                    camera.makePhoto(number, listener); // number already incremented ;)
                }

                @Override
                public void onCreated() {
                    failsInARowCounter = 0;

                    autoFocusTimeAverage[number % AF_TIME_ACCURACY] = System.currentTimeMillis() - autoFocusTimeStart;
                    afAverageTime = calculateAverageAutoFocusTime();
//                    Util.log("afAverageTime: " + afAverageTime);

                    capturedPhotos++;
                    Intent i = getSendingMessageIntent(BROADCAST_MSG_CAPTURED_PHOTO);
                    i.putExtra(BROADCAST_MSG_CAPTURED_PHOTO_AMOUNT, number);
                    i.putExtra(BROADCAST_MSG_AF_AVG_TIME, afAverageTime);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
                    updateNotificationMadePhotos(number);

                    if(number == amount) {
                        Util.log("Koniec sesji");

                        Intent intentFinish = getSendingMessageIntent(BROADCAST_MSG_FINISH);
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intentFinish);

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

                    autoFocusTimeStart = System.currentTimeMillis();
                    camera.makePhoto(number++, listener);
                }
            };

            @Override
            public void run() {
                for(int i=0; i<AF_TIME_ACCURACY; i++)
                    autoFocusTimeAverage[i] = -1;

                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "myWakeLockTag");
                wakeLock.acquire();

                number = timelapseSessionConfig.photoStartIdx;
                camera.makePhoto(number++, listener);
                autoFocusTimeStart = System.currentTimeMillis();
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
//        Util.log("CameraService::onBind");
        return mBinder;
    }

    private Intent getSendingMessageIntent(String message) {
        Intent intent = new Intent(BROADCAST_FILTER);
        intent.putExtra(BROADCAST_MSG, message);
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
//        Util.log("CameraService::onDestroy()!");
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
