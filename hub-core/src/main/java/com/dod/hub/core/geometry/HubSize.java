package com.dod.hub.core.geometry;

public class HubSize {
    public final int width;
    public final int height;

    public HubSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public String toString() {
        return String.format("(%d, %d)", width, height);
    }
}
