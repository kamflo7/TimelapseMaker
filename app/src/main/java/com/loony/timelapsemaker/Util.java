package com.loony.timelapsemaker;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.loony.timelapsemaker.camera.Camera;
import com.loony.timelapsemaker.camera.CameraImplV1;
import com.loony.timelapsemaker.camera.CameraImplV2;
import com.loony.timelapsemaker.camera.CameraVersion;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by Kamil on 11/13/2016.
 */

public class Util {
    public static final String BROADCAST_FILTER = "com.loony.timelapsemaker.camera.CameraService";
    public static final String BROADCAST_MESSAGE = "action";

    public static final String BROADCAST_MESSAGE_INIT_TIMELAPSE_CONTROLLER = "initControllerTimelapse";
    public static final String BROADCAST_MESSAGE_FINISHED = "finished";
    public static final String BROADCAST_MESSAGE_FINISHED_FAILED = "finishedWithFail";

    public static final String BROADCAST_MESSAGE_CAPTURED_PHOTO = "capturedPhoto";
    public static final String BROADCAST_MESSAGE_CAPTURED_PHOTO_AMOUNT = "capturedPhotoAmount";


    public static String getApplicationVersion(Context context) {
        String version = "";

        PackageInfo pInfo = null;
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return version;
    }

    public static String makeBasicAuthPassword(String username, String password) {
        String s = username + ":" + password;
        String result = "Basic " + Base64.encodeToString(s.getBytes(), Base64.NO_WRAP);
        return result;
    }

    public static Camera getAppropriateCamera() {
        return getAppropriateCamera(CameraVersion.API_1);
    }

    public static Camera getAppropriateCamera(CameraVersion cameraVersion) {
        Camera camera = cameraVersion == CameraVersion.API_1 ? new CameraImplV1() : new CameraImplV2();
        Util.log("[Util::getAppropriateCamera] Returned camera: " + camera.getClass().toString());
        return camera;
    }

    public static String[] NECESSARY_PERMISSIONS_START_APP = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.WAKE_LOCK
    };

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
            if(!service.service.getClassName().contains("com.loony.timelapsemaker"))
                continue;

            Util.log("[Util::isMyServiceRunning][forEach] [className: %s] [process: %s] [pid: %d] [activeSince: %d]",
                    service.service.getClassName(), service.process, service.pid, service.activeSince);
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static <K, V> String mapToString(final String label, final String newline, Map<K, V> map) {
        String output = String.format("%s: %s", label, newline);

        for(Map.Entry<K, V> entry : map.entrySet()) {
            log("Map.Entry key: '%s'; value: '%s'", entry.getKey(), entry.getValue());
            output += String.format("- '%s' -> '%s'%s", entry.getKey(), entry.getValue(), newline);
        }

        return output;
    }

    public static String getLocalIpAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }

    public static float getBatteryLevel(Context context) {
        Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        // Error checking that probably isn't needed but I added just in case.
        if(level == -1 || scale == -1) {
            return 50.0f;
        }

        return ((float)level / (float)scale) * 100.0f;
    }

    public static String secondsToTime(int seconds) {
        int minutes = seconds / 60;
        int newSeconds = seconds % 60;

        return String.format("%02dm:%02ds", minutes, newSeconds);
    }

    public static boolean checkPermissions(String[] permissions, Context context) {
        for(String permission : permissions) {
            int permissionCheckResult = ContextCompat.checkSelfPermission(context, permission);
            if(permissionCheckResult == PermissionChecker.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    public interface Function {
        void execute();
    }
}
