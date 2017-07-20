package com.loony.timelapsemaker.camera;

/**
 * Created by Kamil on 7/20/2017.
 */

public class Resolution {
    public int width;
    public int height;

    public Resolution(int width, int height) {
        this.width = width;
        this.height = height;
    }

    // I know, little redundant, but this is because suddenly it turned out that android.util.Size is since API 21, and just for compatibility it is
    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
