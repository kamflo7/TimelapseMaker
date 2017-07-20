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

    /*public void setPictureSize(int width, int height) {
        setPictureSize(new Resolution(width, height));
    }*/

    public Resolution getPictureSize() {
        return pictureSize;
    }

    public long getMilisecondsInterval() {
        return this.milisecondsInterval;
    }

    public int getPhotosLimit() {
        return this.photosLimit;
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
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(milisecondsInterval);
        dest.writeInt(photosLimit);
        dest.writeInt(pictureSize.getWidth());
        dest.writeInt(pictureSize.getHeight());
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
