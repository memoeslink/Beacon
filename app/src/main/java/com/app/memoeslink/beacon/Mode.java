package com.app.memoeslink.beacon;

public enum Mode {
    DEFAULT,
    SOS;

    private static Mode[] values = values();

    public Mode previous() {
        return values[(this.ordinal() + -1 + values.length) % values.length];
    }

    public Mode next() {
        return values[(this.ordinal() + 1) % values.length];
    }
}
