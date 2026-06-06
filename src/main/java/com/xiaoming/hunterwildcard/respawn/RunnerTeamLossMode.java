package com.xiaoming.hunterwildcard.respawn;

import java.util.Locale;

public enum RunnerTeamLossMode {
    ANY_RUNNER_OUT,
    ALL_RUNNERS_OUT;

    public static RunnerTeamLossMode fromConfig(String value, RunnerTeamLossMode fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        try {
            return RunnerTeamLossMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }
}
