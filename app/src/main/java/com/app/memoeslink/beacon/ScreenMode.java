package com.app.memoeslink.beacon;

public enum ScreenMode {
    DEFAULT,
    SOS,
    RAINBOW;

    public ScreenMode previous() {
        return values()[(this.ordinal() + -1 + values().length) % values().length];
    }

    public ScreenMode next() {
        return values()[(this.ordinal() + 1) % values().length];
    }
}
