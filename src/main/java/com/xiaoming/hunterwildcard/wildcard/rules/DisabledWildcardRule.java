package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class DisabledWildcardRule implements WildcardRule {
    @Override
    public String getName() {
        return "暂时停用";
    }

    @Override
    public void onStart(GameContext context) {
        for (ServerPlayerEntity player : context.getParticipants()) {
            player.sendMessage(Text.literal("暂时停用：本轮外卡没有额外效果。"), false);
        }
    }
}
