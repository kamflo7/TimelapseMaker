package com.loony.timelapsemaker.camera;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Kamil on 11/29/2016.
 */

public class TimelapseConfig implements Parcelable {
    private int photosAmount;
    private long frequencyCaptureMiliseconds;
    public Calculator calculator;


    public void setPhotosAmount(int amount) {
        this.photosAmount = amount;
    }

    public void setFrequencyCaptureMiliseconds(long ms) {
        this.frequencyCaptureMiliseconds = ms;
    }

    public int getPhotosAmount() {
        return photosAmount;
    }

    public long getFrequencyCaptureMiliseconds() {
        return frequencyCaptureMiliseconds;
    }

    public TimelapseConfig() {
        calculator = new Calculator();
    }

    public class Calculator {
        public int getTotalSecondsTimeToCaptureAll(int amount, long captureDelayMs) {
            float freqSec = frequencyCaptureMiliseconds/1000f;
            float delaySec = captureDelayMs / 1000f;

            return Math.round((amount-1)*(freqSec+delaySec));
        }

        public int getTotalSecondsTimeToCaptureAll(long captureDelayMs) {
            return getTotalSecondsTimeToCaptureAll(photosAmount, captureDelayMs);
        }

        public float getFps(int videoLengthSeconds) {
            return photosAmount / (float) videoLengthSeconds;
        }

        public float getFps(int videoLengthMinutes, int videoLengthSeconds) {
            return getFps(videoLengthMinutes*60 + videoLengthSeconds);
        }
    }

    // ### Parcel region below ###

    @Override
    public int describeContents() {
        return 0;
    }

    public TimelapseConfig(Parcel in) {
        this();
        photosAmount = in.readInt();
        frequencyCaptureMiliseconds = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(photosAmount);
        dest.writeLong(frequencyCaptureMiliseconds);
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
