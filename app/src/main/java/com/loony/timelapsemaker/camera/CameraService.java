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
import com.loony.timelapsemaker.NewActivity;
import com.loony.timelapsemaker.R;
import com.loony.timelapsemaker.Util;
import com.loony.timelapsemaker.camera.exceptions.CameraNotAvailableException;

/**
 * Created by Kamil on 7/19/2017.
 */

public class CameraService extends Service {
    private static final int NOTIFICATION_ID = 1;
    private static final int NOTIFICATION_TYPE_START = 1;
    private static final int NOTIFICATION_TYPE_CAPTURE = 2;

    private final IBinder mBinder = new LocalBinder();
    private Worker worker;

    private TimelapseController timelapseController;
    private TimelapseConfig timelapseConfig;

    public enum TimelapseState {
        NOT_FINISHED,
        FINISHED_FAIL,
        FINISHED
    }

    private TimelapseState timelapseState = TimelapseState.NOT_FINISHED;

    public TimelapseState getTimelapseState() {
        return timelapseState;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(NOTIFICATION_ID, getMyNotification(NOTIFICATION_TYPE_START, -1));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) { // THREAD == MAIN
        if(intent == null) {
            Util.log("CameraService::onStartCommand is null, fatal exception; have a look at this");
            stopSelf();
            return START_NOT_STICKY;
        }

        Intent i = getSendingMessageIntent(Util.BROADCAST_MESSAGE_FINISHED);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);

        timelapseState = TimelapseState.NOT_FINISHED;
        timelapseConfig = intent.getExtras().getParcelable(NewActivity.PARCEL_TIMELAPSE_CONFIG);

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    timelapseController = new TimelapseController(getApplicationContext(), timelapseConfig);
                    timelapseController.start(new OnTimelapseStateChangeListener() {
                        @Override
                        public void onComplete() {
                            timelapseState = TimelapseState.FINISHED;
                            timelapseController.stop();
                            Intent i = getSendingMessageIntent(Util.BROADCAST_MESSAGE_FINISHED);
                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
                        }

                        @Override
                        public void onFail() {
                            timelapseState = TimelapseState.FINISHED_FAIL;
                            timelapseController.stop();
                            Intent i = getSendingMessageIntent(Util.BROADCAST_MESSAGE_FINISHED_FAILED);
                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
                        }

                        @Override
                        public void onProgress() {

                        }
                    });
                } catch (CameraNotAvailableException e) {
                    e.printStackTrace();
                }
            }
        };

        worker = new Worker("WorkerThread");
        worker.start();
        worker.waitUntilReady();
        worker.handler.post(runnable);
        return super.onStartCommand(intent, flags, startId);
    }

    private Notification getMyNotification(int type, int additionalArg) {
        Intent notificationIntent = new Intent(this, NewActivity.class);
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

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    private Intent getSendingMessageIntent(String message) {
        Intent intent = new Intent(Util.BROADCAST_FILTER);
        intent.putExtra(Util.BROADCAST_MESSAGE, message);
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
        if(timelapseController != null) {
            timelapseController.stop();
        }

        stopForeground(true);
        if(worker != null) worker.quit();
        super.onDestroy();
    }

    public class LocalBinder extends Binder {
        public CameraService getService() {
            return CameraService.this;
        }
    }
}
