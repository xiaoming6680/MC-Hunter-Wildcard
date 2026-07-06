package com.xiaoming.hunterwildcard.wildcard;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public interface WildcardRule {
    default String getName() {
        return HunterWildcardText.wildcardId(getClass().getSimpleName());
    }

    default String getNameKey() {
        return HunterWildcardText.wildcardNameKey(getName());
    }

    default Text getDisplayName() {
        return HunterWildcardText.wildcardName(getName());
    }

    default String getDescriptionKey() {
        return HunterWildcardText.wildcardDescriptionKey(getName());
    }

    default void onStart(GameContext context) {
    }

    default void onTick(GameContext context, int remainingTicks) {
    }

    default void onPlayerDeath(GameContext context, ServerPlayerEntity player) {
    }

    default void onEntityKilled(GameContext context, ServerPlayerEntity killer, LivingEntity killed) {
    }

    default void onPlayerAttack(GameContext context, ServerPlayerEntity player, Entity target) {
    }

    default void onPlayerAteFood(GameContext context, ServerPlayerEntity player, ItemStack eatenStack) {
    }

    default void onItemUse(GameContext context, ServerPlayerEntity player, Hand hand, ItemStack stack) {
    }

    default void onBlockPlaced(GameContext context, ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state) {
    }

    default void onStop(GameContext context) {
    }

    default BlockPos modifyCompassTarget(GameContext context, ServerPlayerEntity hunter, ServerPlayerEntity runner, BlockPos realTarget) {
        return realTarget;
    }
}
