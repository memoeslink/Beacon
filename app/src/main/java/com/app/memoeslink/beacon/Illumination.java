package com.app.memoeslink.beacon;

public enum Illumination {
    NONE,
    SCREEN,
    FLASH,
    ALL;

    private static final Illumination[] VALUES = values();

    public Illumination previous() {
        return VALUES[(this.ordinal() + -1 + VALUES.length) % VALUES.length];
    }

    public Illumination next() {
        return VALUES[(this.ordinal() + 1) % VALUES.length];
    }
}
