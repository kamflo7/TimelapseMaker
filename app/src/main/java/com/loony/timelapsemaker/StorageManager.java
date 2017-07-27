package com.loony.timelapsemaker;

import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Kamil on 7/23/2017.
 */

/*
Environment.getExternalStorageDirectory();                                      ->  /storage/emulated/0
Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);  ->  /storage/emulated/0/Pictures
*/

public class StorageManager {
    private static final String APPLICATION_STORAGE_PATH = "TimelapseMaker";

    public StorageManager() {}

    public boolean isExternalStorageAvailable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /**
     *
     * @return  the directory name, or null if failed to create directory
     */
    public String createDirectory() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String dirName = sdf.format(new Date());

        File absolutePath = new File(getPath(dirName));
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
        return String.format("%s/%s/%s",
                Environment.getExternalStorageDirectory(),
                APPLICATION_STORAGE_PATH,
                directoryName);
    }
}
