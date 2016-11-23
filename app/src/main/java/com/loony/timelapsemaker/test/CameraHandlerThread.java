package com.loony.timelapsemaker.test;

import android.content.Context;
import android.os.HandlerThread;

import com.loony.timelapsemaker.MyCamera;
import com.loony.timelapsemaker.Util;

/**
 * Created by Kamil on 11/22/2016.
 */

public class CameraHandlerThread extends HandlerThread {

    public CameraHandlerThread(String name, Context context) {
        super(name);
        this.context = context;
    }

    private Context context;
    private MyCamera camera;
    private int number;

    @Override
    public void run() {
        camera = new MyCamera(context);

        for(int i=0; i<5; i++) {
            camera.makeAPhoto(number++, null);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Util.log("End execution of making photos");
    }
}
