package com.dod.hub.core.geometry;

public class HubPoint {
    public final int x;
    public final int y;

    public HubPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return String.format("(%d, %d)", x, y);
    }
}
