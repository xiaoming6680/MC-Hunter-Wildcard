package com.xiaoming.hunterwildcard.wildcard;

import com.xiaoming.hunterwildcard.game.GameContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public interface WildcardRule {
    String getName();

    default void onStart(GameContext context) {
    }

    default void onTick(GameContext context, int remainingTicks) {
    }

    default void onPlayerDeath(GameContext context, ServerPlayerEntity player) {
    }

    default void onStop(GameContext context) {
    }

    default BlockPos modifyCompassTarget(GameContext context, ServerPlayerEntity hunter, ServerPlayerEntity runner, BlockPos realTarget) {
        return realTarget;
    }
}
