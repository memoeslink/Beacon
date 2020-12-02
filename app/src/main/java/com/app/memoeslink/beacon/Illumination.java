package com.app.memoeslink.beacon;

public enum Illumination {
    NONE,
    SCREEN,
    FLASH,
    ALL;

    private static Illumination[] values = values();

    public Illumination previous() {
        return values[(this.ordinal() + -1 + values.length) % values.length];
    }

    public Illumination next() {
        return values[(this.ordinal() + 1) % values.length];
    }
}
