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
    public void onTick(GameContext context, int remainingTicks) {
        if (remainingTicks <= 0 || remainingTicks % REFRESH_INTERVAL_TICKS != 0) {
            return;
        }

        for (ServerPlayerEntity player : context.getParticipants()) {
            float ratio = player.getHealth() / Math.max(1.0F, player.getMaxHealth());
            if (ratio <= 0.15F) {
                applyEffects(player, 2, 2, 2, true);
            } else if (ratio <= 0.30F) {
                applyEffects(player, 1, 1, 1, false);
            } else if (ratio <= 0.50F) {
                applyEffects(player, 1, 0, 0, false);
            } else if (ratio <= 0.70F) {
                applyEffects(player, 0, 0, -1, false);
            } else if (ratio <= 0.90F) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, EFFECT_TICKS, 0, false, false, true));
            }
        }
    }

    private void applyEffects(ServerPlayerEntity player, int speedAmplifier, int strengthAmplifier, int resistanceAmplifier, boolean nightVision) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, EFFECT_TICKS, speedAmplifier, false, false, true));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, EFFECT_TICKS, strengthAmplifier, false, false, true));
        if (resistanceAmplifier >= 0) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, EFFECT_TICKS, resistanceAmplifier, false, false, true));
        }
        if (nightVision) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, EFFECT_TICKS, 0, false, false, true));
        }
    }
}
