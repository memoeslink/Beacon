package com.app.memoeslink.beacon;

public enum IlluminationType {
    NONE,
    SCREEN,
    FLASH,
    ALL;

    public IlluminationType previous() {
        return values()[(this.ordinal() + -1 + values().length) % values().length];
    }

    public IlluminationType next() {
        return values()[(this.ordinal() + 1) % values().length];
    }
}
