package com.xiaoming.hunterwildcard.team;

import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import net.minecraft.text.Text;

public enum PlayerRole {
    HUNTER("role.hunter"),
    RUNNER("role.runner");

    private final String keyPath;

    PlayerRole(String keyPath) {
        this.keyPath = keyPath;
    }

    public String getTranslationKey() {
        return HunterWildcardText.key(keyPath);
    }

    public String getDisplayName() {
        return getTranslationKey();
    }

    public Text getDisplayText() {
        return Text.translatable(getTranslationKey());
    }
}
