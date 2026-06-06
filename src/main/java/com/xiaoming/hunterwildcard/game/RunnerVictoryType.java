package com.xiaoming.hunterwildcard.game;

import java.util.Locale;

public enum RunnerVictoryType {
    DRAGON("击败末影龙"),
    SURVIVE_TIME("存活指定时间"),
    REACH_LOCATION("到达指定坐标"),
    COLLECT_ITEM("收集指定物品");

    private final String displayName;

    RunnerVictoryType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
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
