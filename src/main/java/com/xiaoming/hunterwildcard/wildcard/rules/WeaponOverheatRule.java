package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WeaponOverheatRule implements WildcardRule {
    private static final int WINDOW_TICKS = 60;
    private static final int WEAKNESS_ATTACK_COUNT = 2;
    private static final int SLOWNESS_ATTACK_COUNT = 3;
    private static final int SEVERE_ATTACK_COUNT = 4;
    private static final int EFFECT_REFRESH_TICKS = 3;

    private final Map<UUID, Deque<Integer>> attackTicks = new HashMap<>();
    private final Map<UUID, Integer> lastSyncedHeat = new HashMap<>();
    private int ticks;

    @Override
    public String getName() {
        return "武器过热";
    }

    @Override
    public void onStart(GameContext context) {
        ticks = 0;
        attackTicks.clear();
        lastSyncedHeat.clear();
        syncAllStatus(context);
    }

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        ticks++;
        syncAllStatus(context);
    }

    @Override
    public void onPlayerAttack(GameContext context, ServerPlayerEntity player, Entity target) {
        UUID playerId = player.getUuid();
        Deque<Integer> recent = attackTicks.computeIfAbsent(playerId, ignored -> new ArrayDeque<>());
        recent.addLast(ticks);
        int attackCount = pruneAndCount(recent);
        updatePlayerStatus(player, attackCount);
    }

    @Override
    public void onStop(GameContext context) {
        for (ServerPlayerEntity player : context.getParticipants()) {
            HunterWildcardPackets.clearWeaponOverheatStatus(player);
        }
        attackTicks.clear();
        lastSyncedHeat.clear();
    }

    private void syncAllStatus(GameContext context) {
        for (ServerPlayerEntity player : context.getParticipants()) {
            UUID playerId = player.getUuid();
            Deque<Integer> recent = attackTicks.get(playerId);
            int attackCount = recent == null ? 0 : pruneAndCount(recent);
            updatePlayerStatus(player, attackCount);
        }
    }

    private int pruneAndCount(Deque<Integer> recent) {
        while (!recent.isEmpty() && ticks - recent.peekFirst() > WINDOW_TICKS) {
            recent.removeFirst();
        }
        return recent.size();
    }

    private void syncStatus(ServerPlayerEntity player, int heat) {
        UUID playerId = player.getUuid();
        if (lastSyncedHeat.getOrDefault(playerId, -1) == heat) {
            return;
        }

        lastSyncedHeat.put(playerId, heat);
        HunterWildcardPackets.sendWeaponOverheatStatus(player, heat, SEVERE_ATTACK_COUNT);
    }

    private void updatePlayerStatus(ServerPlayerEntity player, int attackCount) {
        int heat = Math.min(attackCount, SEVERE_ATTACK_COUNT);
        applyHeatEffects(player, heat);
        syncStatus(player, heat);
    }

    private void applyHeatEffects(ServerPlayerEntity player, int heat) {
        if (heat >= SEVERE_ATTACK_COUNT) {
            applyOverheat(player, 1, 1);
        } else if (heat >= SLOWNESS_ATTACK_COUNT) {
            applyOverheat(player, 0, 0);
        } else if (heat >= WEAKNESS_ATTACK_COUNT) {
            applyOverheat(player, 0, -1);
            clearShortOverheatEffect(player, StatusEffects.SLOWNESS);
        } else {
            clearShortOverheatEffect(player, StatusEffects.WEAKNESS);
            clearShortOverheatEffect(player, StatusEffects.SLOWNESS);
        }
    }

    private void applyOverheat(ServerPlayerEntity player, int weaknessAmplifier, int slownessAmplifier) {
        applyOrReplaceShortOverheatEffect(player, StatusEffects.WEAKNESS, weaknessAmplifier);
        if (slownessAmplifier >= 0) {
            applyOrReplaceShortOverheatEffect(player, StatusEffects.SLOWNESS, slownessAmplifier);
        }
    }

    private void applyOrReplaceShortOverheatEffect(ServerPlayerEntity player, RegistryEntry<StatusEffect> effect, int amplifier) {
        StatusEffectInstance currentEffect = player.getStatusEffect(effect);
        if (currentEffect != null && currentEffect.getDuration() <= EFFECT_REFRESH_TICKS && currentEffect.getAmplifier() != amplifier) {
            player.removeStatusEffect(effect);
        }
        player.addStatusEffect(new StatusEffectInstance(effect, EFFECT_REFRESH_TICKS, amplifier, false, false, true));
    }

    private void clearShortOverheatEffect(ServerPlayerEntity player, RegistryEntry<StatusEffect> effect) {
        StatusEffectInstance currentEffect = player.getStatusEffect(effect);
        if (currentEffect != null && currentEffect.getDuration() <= EFFECT_REFRESH_TICKS) {
            player.removeStatusEffect(effect);
        }
    }
}
