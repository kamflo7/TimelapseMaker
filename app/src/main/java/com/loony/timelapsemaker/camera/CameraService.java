package com.loony.timelapsemaker.camera;

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

import com.loony.timelapsemaker.InfinityFixedList;
import com.loony.timelapsemaker.MainActivity;
import com.loony.timelapsemaker.R;
import com.loony.timelapsemaker.Util;

/**
 * Created by Kamil on 11/13/2016.
 */

public class CameraService extends Service {

    public static String BROADCAST_FILTER = "com.loony.timelapsemaker.camera.CameraService";
    public static String BROADCAST_MESSAGE = "message";

    public static String BROADCAST_MSG_CAPTURED_PHOTO = "capturedPhoto";
    public static String BROADCAST_MSG_CAPTURED_PHOTO_AMOUNT = "capturedPhotoAmount";
    public static String BROADCAST_MSG_CAPTURED_PHOTO_DURATION_MS = "capturedPhotoDurationMs";
    public static String BROADCAST_MSG_FINISH = "finish";

    private static final int NOTIFICATION_ID = 1;
    private static final int NOTIFICATION_TYPE_START = 1;
    private static final int NOTIFICATION_TYPE_CAPTURE = 2;


    private final IBinder mBinder = new LocalBinder();
    private Worker worker;
    private TimelapseConfig timelapseConfig;
    private TimelapseController timelapseController;
    private InfinityFixedList<Integer> lastCaptureDurationList;

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(NOTIFICATION_ID, getMyNotification(NOTIFICATION_TYPE_START, -1));
        lastCaptureDurationList = new InfinityFixedList<>(10);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent == null) {
            Util.log("CameraService::onStartCommand is null, fatal exception; have a look at this");
            stopSelf();
            return START_NOT_STICKY;
        }

        timelapseConfig = intent.getExtras().getParcelable(MainActivity.PARCEL_TIMELAPSE_CONFIG);

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                timelapseController = new TimelapseController(timelapseConfig, getApplicationContext(), new TimelapseController.StateChange() {
                    @Override
                    public void onSessionCrash() {
                        CameraService.this.stopSelf();
                    }

                    @Override
                    public void onPhotoCapture() {
                        Intent i = getSendingMessageIntent(BROADCAST_MSG_CAPTURED_PHOTO);
                        i.putExtra(BROADCAST_MSG_CAPTURED_PHOTO_AMOUNT, timelapseController.getCapturedAmount());

                        lastCaptureDurationList.add(timelapseController.getLastCaptureDurationMiliseconds().intValue());

                        i.putExtra(BROADCAST_MSG_CAPTURED_PHOTO_DURATION_MS, lastCaptureDurationList.getAverage());
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
                        updateNotificationMadePhotos(timelapseController.getCapturedAmount());
                    }

                    @Override
                    public void onSessionEnd() {
                        Intent intentFinish = getSendingMessageIntent(BROADCAST_MSG_FINISH);
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intentFinish);
                        CameraService.this.stopSelf();
                    }
                });
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
        return mBinder;
    }

    private Intent getSendingMessageIntent(String message) {
        Intent intent = new Intent(BROADCAST_FILTER);
        intent.putExtra(BROADCAST_MESSAGE, message);
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
        if(timelapseController != null)
            timelapseController.close();

        stopForeground(true);
        worker.quit();
        super.onDestroy();
    }

    public class LocalBinder extends Binder {
        public CameraService getService() {
            return CameraService.this;
        }
    }
}
