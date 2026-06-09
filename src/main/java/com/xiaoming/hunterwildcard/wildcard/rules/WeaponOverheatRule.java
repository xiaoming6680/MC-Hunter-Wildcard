package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
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
    private static final int EFFECT_REFRESH_TICKS = 16;
    private static final int STATUS_SYNC_INTERVAL_TICKS = 5;

    private final Map<UUID, Deque<Integer>> attackTicks = new HashMap<>();
    private int ticks;

    @Override
    public String getName() {
        return "武器过热";
    }

    @Override
    public void onStart(GameContext context) {
        ticks = 0;
        attackTicks.clear();
        syncAllStatus(context);
    }

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        ticks++;
        if (ticks % STATUS_SYNC_INTERVAL_TICKS == 0) {
            syncAllStatus(context);
        }
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

    private void syncStatus(ServerPlayerEntity player, int attackCount) {
        HunterWildcardPackets.sendWeaponOverheatStatus(player, Math.min(attackCount, SEVERE_ATTACK_COUNT), SEVERE_ATTACK_COUNT);
    }

    private void updatePlayerStatus(ServerPlayerEntity player, int attackCount) {
        applyHeatEffects(player, attackCount);
        syncStatus(player, attackCount);
    }

    private void applyHeatEffects(ServerPlayerEntity player, int attackCount) {
        if (attackCount >= SEVERE_ATTACK_COUNT) {
            applyOverheat(player, 1, 1);
        } else if (attackCount >= SLOWNESS_ATTACK_COUNT) {
            applyOverheat(player, 0, 0);
        } else if (attackCount >= WEAKNESS_ATTACK_COUNT) {
            applyOverheat(player, 0, -1);
        }
    }

    private void applyOverheat(ServerPlayerEntity player, int weaknessAmplifier, int slownessAmplifier) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, EFFECT_REFRESH_TICKS, weaknessAmplifier, false, false, true));
        if (slownessAmplifier >= 0) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, EFFECT_REFRESH_TICKS, slownessAmplifier, false, false, true));
        }
    }
}
