package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public class NightHuntRule implements WildcardRule {
    @Override
    public String getName() {
        return "NightHunt";
    }

    @Override
    public void onStart(GameContext context) {
        forceNight(context);
        giveNightVision(context, context.getConfig().wildcardDurationTicks + 40);
    }

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        if (remainingTicks % 200 == 0) {
            forceNight(context);
            giveNightVision(context, Math.max(240, remainingTicks + 40));
        }
    }

    @Override
    public void onStop(GameContext context) {
        for (ServerPlayerEntity hunter : context.getHunters()) {
            hunter.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
    }

    private void forceNight(GameContext context) {
        for (ServerWorld world : context.getServer().getWorlds()) {
            world.setTimeOfDay(18000);
        }
    }

    private void giveNightVision(GameContext context, int duration) {
        for (ServerPlayerEntity hunter : context.getHunters()) {
            hunter.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, duration, 0, false, false, true));
        }
    }
}
