package com.loony.timelapsemaker;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Kamil on 7/30/2017.
 */

public class MySharedPreferences {
    private static String FILE_KEY = "com.loony.timelapsemaker.settingsFile";
    private static String KEY_WEB_PASSWORD = "com.loony.timelapsemaker.settingsFile.webpassword";

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
}
