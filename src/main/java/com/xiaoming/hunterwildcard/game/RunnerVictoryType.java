package com.xiaoming.hunterwildcard.game;

import com.xiaoming.hunterwildcard.util.HunterWildcardText;

import java.util.Locale;

public enum RunnerVictoryType {
    DRAGON("config.runner_victory.dragon"),
    SURVIVE_TIME("config.runner_victory.survive_time"),
    REACH_LOCATION("config.runner_victory.reach_location"),
    COLLECT_ITEM("config.runner_victory.collect_item");

    private final String keyPath;

    RunnerVictoryType(String keyPath) {
        this.keyPath = keyPath;
    }

    public String getTranslationKey() {
        return HunterWildcardText.key(keyPath);
    }

    public String getDisplayName() {
        return getTranslationKey();
    }

    public static RunnerVictoryType fromConfig(String value, RunnerVictoryType fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        try {
            return RunnerVictoryType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }
}
