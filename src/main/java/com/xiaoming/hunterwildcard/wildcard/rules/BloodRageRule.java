package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;

public class BloodRageRule implements WildcardRule {
    private static final int REFRESH_INTERVAL_TICKS = 20;
    private static final int EFFECT_TICKS = 45;

    @Override
    public String getName() {
        return "血怒时刻";
    }

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        if (remainingTicks <= 0 || remainingTicks % REFRESH_INTERVAL_TICKS != 0) {
            return;
        }

        for (ServerPlayerEntity player : context.getParticipants()) {
            float ratio = player.getHealth() / Math.max(1.0F, player.getMaxHealth());
            if (ratio < 0.2F) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, EFFECT_TICKS, 1, false, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, EFFECT_TICKS, 1, false, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, EFFECT_TICKS, 2, false, false, true));
            } else if (ratio < 0.3F) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, EFFECT_TICKS, 1, false, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, EFFECT_TICKS, 1, false, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, EFFECT_TICKS, 1, false, false, true));
            } else if (ratio < 0.5F) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, EFFECT_TICKS, 0, false, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, EFFECT_TICKS, 0, false, false, true));
            } else if (ratio < 0.8F) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, EFFECT_TICKS, 0, false, false, true));
            }
        }
    }
}
