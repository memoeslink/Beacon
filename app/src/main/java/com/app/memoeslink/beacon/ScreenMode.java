package com.app.memoeslink.beacon;

public enum ScreenMode {
    DEFAULT,
    SOS,
    RAINBOW,
    BLINK;

    public ScreenMode previous() {
        return values()[(this.ordinal() + values().length - 1) % values().length];
    }

    public ScreenMode next() {
        return values()[(this.ordinal() + 1) % values().length];
    }
}
