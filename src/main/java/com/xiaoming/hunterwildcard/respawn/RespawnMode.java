package com.xiaoming.hunterwildcard.respawn;

import java.util.Locale;

public enum RespawnMode {
    NO_RESPAWN,
    LIMITED_LIVES,
    INFINITE;

    public static RespawnMode fromConfig(String value, RespawnMode fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        try {
            return RespawnMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }
}
