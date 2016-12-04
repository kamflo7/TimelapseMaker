package com.loony.timelapsemaker;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Kamil on 11/29/2016.
 */

public class TimelapseSessionConfig implements Parcelable {
    public int fps, outputSeconds, inputMinutes, photoStartIdx;

    public float captureFrequency;
    public int framesAmount;

//    public void calculateFramesAmount() {
//        framesAmount = fps * outputSeconds;
//    }
//
//    public void calculateCaptureFrequency() {
//        captureFrequency = (inputMinutes*60f)/calculateFramesAmount();
//    }

    public void calculate() {
        framesAmount = fps * outputSeconds;
        captureFrequency = (inputMinutes*60f)/framesAmount;
    }

    public TimelapseSessionConfig() { }

    // ### Parcel region below ###

    @Override
    public int describeContents() {
        return 0;
    }

    public TimelapseSessionConfig(Parcel in) {
        fps = in.readInt();
        outputSeconds = in.readInt();
        inputMinutes = in.readInt();
        photoStartIdx = in.readInt();
        captureFrequency = in.readFloat();
        framesAmount = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(fps);
        dest.writeInt(outputSeconds);
        dest.writeInt(inputMinutes);
        dest.writeInt(photoStartIdx);
        dest.writeFloat(captureFrequency);
        dest.writeInt(framesAmount);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public TimelapseSessionConfig createFromParcel(Parcel in) {
            return new TimelapseSessionConfig(in);
        }

        public TimelapseSessionConfig[] newArray(int size) {
            return new TimelapseSessionConfig[size];
        }
    };
}
