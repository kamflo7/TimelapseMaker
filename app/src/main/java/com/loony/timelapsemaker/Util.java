package com.loony.timelapsemaker;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by Kamil on 11/13/2016.
 */

public class Util {
    public static void log(String str, Object... params) {
        String formated = params.length == 0 ? str : String.format(str, params);

        if(params.length == 0)
            Log.d("test", formated);
        else
            Log.d("test", formated);
    }

    public static void logEx(String tag, String str, Object... params) {
        String formated = params.length == 0 ? str : String.format(str, params);

        if(params.length == 0)
            Log.d(tag, formated);
        else
            Log.d(tag, formated);
    }

    public static void logToast(Context context, String str, Object... params) {
        String formated = params.length == 0 ? str : String.format(str, params);
        Log.d("test", formated);
        Toast.makeText(context, formated, Toast.LENGTH_SHORT).show();
    }

    public static boolean isMyServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
