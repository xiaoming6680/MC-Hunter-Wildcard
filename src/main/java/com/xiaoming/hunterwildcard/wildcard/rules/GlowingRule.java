package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;

public class GlowingRule implements WildcardRule {
    @Override
    public String getName() {
        return "Glowing";
    }

    @Override
    public void onStart(GameContext context) {
        apply(context, context.getConfig().wildcardDurationTicks + 40);
    }

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        if (remainingTicks % 100 == 0) {
            apply(context, Math.max(120, remainingTicks + 40));
        }
    }

    @Override
    public void onStop(GameContext context) {
        for (ServerPlayerEntity player : context.getParticipants()) {
            player.removeStatusEffect(StatusEffects.GLOWING);
        }
    }

    private void apply(GameContext context, int duration) {
        for (ServerPlayerEntity player : context.getParticipants()) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, duration, 0, false, false, true));
        }
    }
}
