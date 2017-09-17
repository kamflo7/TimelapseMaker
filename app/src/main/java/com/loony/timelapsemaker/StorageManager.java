package com.loony.timelapsemaker;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Created by Kamil on 7/23/2017.
 */

/*
Environment.getExternalStorageDirectory();                                      ->  /storage/emulated/0
Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);  ->  /storage/emulated/0/Pictures
*/

public class StorageManager {
    private static final String APPLICATION_STORAGE_PATH = "TimelapseMaker";
    private static final String REAL_EXT_SD_CARD_ABSOLUTE_PATH = "/storage/4542-1EE5/";

    private Context ctx;
    private File sdPath;

    public StorageManager(Context ctx) {
        this.ctx = ctx;

        File[] fs = ctx.getExternalFilesDirs(Environment.DIRECTORY_PICTURES);
        for(File f : fs) {
            if(!f.getAbsolutePath().contains("emulated"))
                sdPath = f;
        }
    }

    public boolean isExternalStorageAvailable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public boolean isRealExternalStorageAvailable() {
//        boolean available = new File(REAL_EXT_SD_CARD_ABSOLUTE_PATH).exists();
//        return available;
        return sdPath != null;
    }

    /**
     *
     * @return  the directory name, or null if failed to create directory
     */
    public String createDirectory() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String dirName = sdf.format(new Date());

        File absolutePath = new File(getPath(dirName));

        boolean canWrite = absolutePath.canWrite();
        boolean canRead = absolutePath.canRead();
        //Util.log("createDirectory -> " + dirName + " | " + absolutePath.getAbsolutePath());

        boolean creatingResult = absolutePath.mkdirs();

        if(creatingResult)
            return dirName;

        return null;
    }

    /**
     *
     * @param directory directory name
     * @param imageName image name without extension
     * @param imageBytes image
     * @return true if successfully saved, false if was any problem
     */
    public boolean saveImage(String directory, String imageName, byte[] imageBytes) {
        if(NewActivity.DEBUG_doNotSaveImageInStorageWhenCaptured)
            return true;

        File photo = new File(String.format("%s/%s.jpg", getPath(directory), imageName));

        try {
            FileOutputStream fos = new FileOutputStream(photo.getPath());
            fos.write(imageBytes);
            fos.close();
            Util.log("StorageManager::savingImage to " + photo.getAbsolutePath());
        } catch(IOException e) {
            Util.log("StorageManager::saveImage() exception -> " + e.getMessage());
            return false;
        }
        return true;
    }

    private String getPath(String directoryName) {
//        String normalPath = String.format("%s/%s/%s",
//                Environment.getExternalStorageDirectory(),
//                APPLICATION_STORAGE_PATH,
//                directoryName);

//        String customPathForExtSD = String.format("%s/%s/%s",
//                REAL_EXT_SD_CARD_ABSOLUTE_PATH,
//                APPLICATION_STORAGE_PATH,
//                directoryName);

        String customPathForExtSD = String.format("%s/%s",
                sdPath.getAbsolutePath(),
//                APPLICATION_STORAGE_PATH,
                directoryName);

        return customPathForExtSD;
    }

    private static final Pattern DIR_SEPARATOR = Pattern.compile("/");

}
