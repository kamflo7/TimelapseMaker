package com.loony.timelapsemaker.camera;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Kamil on 7/20/2017.
 */

public class TimelapseConfig implements Parcelable {
    private long milisecondsInterval;
    private int photosLimit;
    private Resolution pictureSize;
    private CameraVersion cameraApiVersion;

    public TimelapseConfig() {}

    public boolean setMilisecondsInterval(long milisecondsInterval) {
        this.milisecondsInterval = milisecondsInterval;
        return true;
    }

    public boolean setPhotosLimit(int photosLimit) {
        if(photosLimit > 0 || photosLimit == -1) {
            this.photosLimit = photosLimit;
            return true;
        }
        return false;
    }

    public void setPictureSize(Resolution resolution) {
        this.pictureSize = resolution;
    }

    public Resolution getPictureSize() {
        return pictureSize;
    }

    public long getMilisecondsInterval() {
        return this.milisecondsInterval;
    }

    public int getPhotosLimit() {
        return this.photosLimit;
    }

    public CameraVersion getCameraApiVersion() {
        return cameraApiVersion;
    }

    public void setCameraApiVersion(CameraVersion cameraApiVersion) {
        this.cameraApiVersion = cameraApiVersion;
    }

    // parcelable:
    @Override
    public int describeContents() {
        return 0;
    }

    public TimelapseConfig(Parcel in) {
        this();
        milisecondsInterval = in.readLong();
        photosLimit = in.readInt();
        pictureSize = new Resolution(in.readInt(), in.readInt());
        cameraApiVersion = in.readInt() == 1 ? CameraVersion.API_1 : CameraVersion.API_2;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(milisecondsInterval);
        dest.writeInt(photosLimit);
        dest.writeInt(pictureSize.getWidth());
        dest.writeInt(pictureSize.getHeight());
        dest.writeInt(cameraApiVersion == CameraVersion.API_1 ? 1 : 2);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public TimelapseConfig createFromParcel(Parcel in) {
            return new TimelapseConfig(in);
        }

        public TimelapseConfig[] newArray(int size) {
            return new TimelapseConfig[size];
        }
    };
}
