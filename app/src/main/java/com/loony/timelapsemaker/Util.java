package com.loony.timelapsemaker;

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

//        if(textView != null)
//            textView.append(formated + "\n");
    }

    public static void logEx(String tag, String str, Object... params) {
        String formated = params.length == 0 ? str : String.format(str, params);

        if(params.length == 0)
            Log.d(tag, formated);
        else
            Log.d(tag, formated);

//        if(textView != null)
//            textView.append(formated + "\n");
    }

    public static void logToast(Context context, String str, Object... params) {
        String formated = params.length == 0 ? str : String.format(str, params);
        Log.d("test", formated);
        Toast.makeText(context, formated, Toast.LENGTH_SHORT).show();

//        if(textView != null)
//            textView.append(formated + "\n");
    }
}
