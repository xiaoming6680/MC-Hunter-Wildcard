package com.xiaoming.hunterwildcard.game;

import java.util.Locale;

public enum HunterVictoryType {
    RUNNERS_OUT("使逃亡者出局"),
    RUNNER_KILL_COUNT("累计击杀逃亡者");

    private final String displayName;

    HunterVictoryType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static HunterVictoryType fromConfig(String value, HunterVictoryType fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        try {
            return HunterVictoryType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }
}
