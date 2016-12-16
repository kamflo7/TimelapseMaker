package com.loony.timelapsemaker;

import java.util.Arrays;

/**
 * Created by Kamil on 12/16/2016.
 */

public class InfinityFixedList<T extends Number> {
    private T[] elements;
    private boolean isFull;
    private int currentIndex = 0;

    public InfinityFixedList(int size) {
        elements = newArray(size);
    }

    public void add(T t) {
        elements[currentIndex++] = t;

        if(currentIndex == elements.length) {
            currentIndex = 0;
            if(!isFull) isFull = true;
        }
    }

    private int getCount() {
        return isFull ? elements.length : currentIndex;
    }

    public float getAverage() {
        float sum = 0f;

        int i = 0, count = getCount();
        while(i < count) {
            sum += elements[i++].floatValue();
        }
        return sum / count;
    }

    static <T> T[] newArray(int length, T... array) {
        return Arrays.copyOf(array, length);
    }
}
