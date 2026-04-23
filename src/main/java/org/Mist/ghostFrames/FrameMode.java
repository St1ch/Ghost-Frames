package org.Mist.ghostFrames;

public enum FrameMode {
    NORMAL("Normal"),
    GHOST("Ghost"),
    LOCKED("Locked"),
    PROTECTED("Protected");

    private final String displayName;

    FrameMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public FrameMode next() {
        FrameMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static FrameMode fromName(String input) {
        for (FrameMode mode : values()) {
            if (mode.name().equalsIgnoreCase(input)) {
                return mode;
            }
        }
        return null;
    }
}
