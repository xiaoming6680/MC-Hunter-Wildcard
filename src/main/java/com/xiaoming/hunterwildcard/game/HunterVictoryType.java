package com.xiaoming.hunterwildcard.game;

import com.xiaoming.hunterwildcard.util.HunterWildcardText;

import java.util.Locale;

public enum HunterVictoryType {
    RUNNERS_OUT("config.hunter_victory.runners_out"),
    RUNNER_KILL_COUNT("config.hunter_victory.runner_kill_count");

    private final String keyPath;

    HunterVictoryType(String keyPath) {
        this.keyPath = keyPath;
    }

    public String getTranslationKey() {
        return HunterWildcardText.key(keyPath);
    }

    public String getDisplayName() {
        return getTranslationKey();
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
