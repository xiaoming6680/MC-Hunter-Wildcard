package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.server.network.ServerPlayerEntity;

public class DisabledWildcardRule implements WildcardRule {
    @Override
    public void onStart(GameContext context) {
        for (ServerPlayerEntity player : context.getParticipants()) {
            player.sendMessage(HunterWildcardText.translatable("msg.wildcard.disabled_no_effect"), false);
        }
    }
}
