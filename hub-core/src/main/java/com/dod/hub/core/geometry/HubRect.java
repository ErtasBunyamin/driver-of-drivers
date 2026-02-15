package com.dod.hub.core.geometry;

public class HubRect {
    public final int x;
    public final int y;
    public final int width;
    public final int height;

    public HubRect(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public HubPoint getPoint() {
        return new HubPoint(x, y);
    }

    public HubSize getSize() {
        return new HubSize(width, height);
    }

    @Override
    public String toString() {
        return String.format("Rect[x=%d, y=%d, w=%d, h=%d]", x, y, width, height);
    }
}
