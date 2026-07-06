package com.xiaoming.hunterwildcard.wildcard;

import com.xiaoming.hunterwildcard.game.GameContext;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public interface WildcardRule {
    String getName();

    default String getDescription() {
        return switch (getClass().getSimpleName()) {
            case "SpeedRushRule" -> "全员加速。";
            case "FeatherweightRule" -> "跳跃提升，缓慢落地。";
            case "GlowingRule" -> "全员发光。";
            case "NightHuntRule" -> "入夜，猎人夜视。";
            case "ExplosiveDeathRule" -> "死亡或击杀会爆炸。";
            case "SupplyDropRule" -> "落下随机补给箱。";
            case "HunterRadarRule" -> "猎人获得距离提示。";
            case "CompassChaosRule" -> "猎人指南针偏移。";
            case "HungerChaseRule" -> "更易饥饿，进食获得速度效果";
            case "WeaponOverheatRule" -> "连打会过热。";
            case "LightLoadRule" -> "轻甲加速，重甲减速。";
            case "BlockDecayRule" -> "新放方块会消失。";
            case "PearlFrenzyRule" -> "定期获得珍珠，但别随便扔：）";
            case "WindChargeBrawlRule" -> "定期获得风弹。";
            case "BloodRageRule" -> "低血量获得强化。";
            case "DisabledWildcardRule" -> "没有额外效果。";
            default -> "";
        };
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
