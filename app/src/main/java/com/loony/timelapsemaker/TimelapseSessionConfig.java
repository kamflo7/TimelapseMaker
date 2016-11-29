package com.loony.timelapsemaker;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Kamil on 11/29/2016.
 */

public class TimelapseSessionConfig implements Parcelable {
    public int fps, outputSeconds, inputMinutes, photoStartIdx;

    public int calculateFramesAmount() {
        return fps * outputSeconds;
    }

    public float calculateCaptureFrequency() {
        return (inputMinutes*60f)/calculateFramesAmount();
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
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(fps);
        dest.writeInt(outputSeconds);
        dest.writeInt(inputMinutes);
        dest.writeInt(photoStartIdx);
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
