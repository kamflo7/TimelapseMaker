package com.loony.timelapsemaker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.loony.timelapsemaker.camera.CameraVersion;

/**
 * Created by Kamil on 7/30/2017.
 */

public class MySharedPreferences {
    private static String FILE_KEY = "com.loony.timelapsemaker.settingsFile";
    private static String KEY_WEB_PASSWORD = "com.loony.timelapsemaker.settingsFile.webpassword";
    private static String KEY_WEB_ENABLED = "com.loony.timelapsemaker.settingsFile.webenabled";
    private static String KEY_CAMERA_API = "com.loony.timelapsemaker.settingsFile.cameraapi";

    SharedPreferences sharedPref;

    public MySharedPreferences(Context context) {
        sharedPref = context.getSharedPreferences(FILE_KEY, Context.MODE_PRIVATE);
    }

    public String readWebPassword() {
        return sharedPref.getString(KEY_WEB_PASSWORD, "null");
    }

    public void setWebPassword(String password) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(KEY_WEB_PASSWORD, password);
        editor.commit();
    }

    public void setWebEnabled(boolean enabled) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(KEY_WEB_ENABLED, enabled);
        editor.commit();
    }

    public boolean getWebEnabled() {
        return sharedPref.getBoolean(KEY_WEB_ENABLED, true);
    }

    public void setCameraApi(CameraVersion version) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(KEY_CAMERA_API, version == CameraVersion.API_1 ? 1 : 2);
        editor.commit();
    }

    public CameraVersion getCameraApi() {
//        return sharedPref.getInt(KEY_CAMERA_API, Build.VERSION.SDK_INT >= 21 ? 2 : 1) == 2 ? CameraVersion.API_2 : CameraVersion.API_1;
        return sharedPref.getInt(KEY_CAMERA_API, 1) == 2 ? CameraVersion.API_2 : CameraVersion.API_1;
        // Always, when var is not initialized, it returns Camera V1 regardless of Android SDK lvl, because I am too lazy to handle situations,
        // when user has Camera API v2 (sdk >= 21) but also has unofficial system (e.g. Cyanogen) and app will be crasing
        // But user always has possibiity to manually change camera API in settings
    }
}
