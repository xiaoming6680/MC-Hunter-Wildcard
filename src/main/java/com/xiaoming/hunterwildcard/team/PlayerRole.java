package com.xiaoming.hunterwildcard.team;

public enum PlayerRole {
    HUNTER("猎人"),
    RUNNER("逃亡者");

    private final String displayName;

    PlayerRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
