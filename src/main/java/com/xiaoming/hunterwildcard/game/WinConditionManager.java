package com.xiaoming.hunterwildcard.game;

import com.xiaoming.hunterwildcard.command.HunterWildcardCommand;
import com.xiaoming.hunterwildcard.config.ModConfig;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class WinConditionManager {
    private int runningTicks;
    private int checkTicks;
    private boolean warnedInvalidDimension;
    private boolean warnedInvalidItem;

    public void start() {
        runningTicks = 0;
        checkTicks = 0;
        warnedInvalidDimension = false;
        warnedInvalidItem = false;
    }

    public String tick(GameContext context) {
        ModConfig config = context.getConfig();
        runningTicks++;
        return switch (config.getRunnerVictoryType()) {
            case DRAGON -> null;
            case SURVIVE_TIME -> runningTicks >= config.getSurviveTimeTicks()
                    ? "逃亡者达成胜利目标：存活 " + config.surviveTimeSeconds + " 秒。"
                    : null;
            case REACH_LOCATION -> shouldRunSlowCheck() ? checkReachLocation(context) : null;
            case COLLECT_ITEM -> shouldRunSlowCheck() ? checkCollectItem(context) : null;
        };
    }

    public String onDragonKilled(GameContext context) {
        if (context.getConfig().getRunnerVictoryType() == RunnerVictoryType.DRAGON) {
            return "逃亡者达成胜利目标：击败末影龙。";
        }

        return null;
    }

    public void clear() {
        runningTicks = 0;
        checkTicks = 0;
        warnedInvalidDimension = false;
        warnedInvalidItem = false;
    }

    private String checkReachLocation(GameContext context) {
        ModConfig config = context.getConfig();
        Identifier dimensionId = Identifier.tryParse(config.targetDimension);
        if (dimensionId == null) {
            warnOpsOnce(context, true, "胜利目标配置无效：目标维度 ID 不合法，已跳过坐标胜利检查。");
            return null;
        }

        RegistryKey<World> targetWorldKey = RegistryKey.of(RegistryKeys.WORLD, dimensionId);
        ServerWorld targetWorld = context.getServer().getWorld(targetWorldKey);
        if (targetWorld == null) {
            warnOpsOnce(context, true, "胜利目标配置无效：找不到目标维度 " + config.targetDimension + "。");
            return null;
        }

        double radiusSquared = (double) config.targetRadius * config.targetRadius;
        for (ServerPlayerEntity runner : context.getRunners()) {
            if (!runner.getEntityWorld().getRegistryKey().equals(targetWorld.getRegistryKey())) {
                continue;
            }

            double dx = runner.getX() - config.targetX;
            double dy = runner.getY() - config.targetY;
            double dz = runner.getZ() - config.targetZ;
            if (dx * dx + dy * dy + dz * dz <= radiusSquared) {
                return "逃亡者达成胜利目标：到达 " + config.targetDimension
                        + " (" + config.targetX + ", " + config.targetY + ", " + config.targetZ + ")。";
            }
        }

        return null;
    }

    private String checkCollectItem(GameContext context) {
        ModConfig config = context.getConfig();
        Identifier itemId = Identifier.tryParse(config.targetItemId);
        if (itemId == null) {
            warnOpsOnce(context, false, "胜利目标配置无效：目标物品 ID 不合法，已跳过收集胜利检查。");
            return null;
        }

        Item targetItem = Registries.ITEM.getOptionalValue(itemId).orElse(null);
        if (targetItem == null) {
            warnOpsOnce(context, false, "胜利目标配置无效：找不到目标物品 " + config.targetItemId + "。");
            return null;
        }

        int total = 0;
        for (ServerPlayerEntity runner : context.getRunners()) {
            for (int slot = 0; slot < runner.getInventory().size(); slot++) {
                ItemStack stack = runner.getInventory().getStack(slot);
                if (stack.isOf(targetItem)) {
                    total += stack.getCount();
                    if (total >= config.targetItemCount) {
                        return "逃亡者达成胜利目标：收集 " + config.targetItemCount + " 个 " + config.targetItemId + "。";
                    }
                }
            }
        }

        return null;
    }

    private boolean shouldRunSlowCheck() {
        checkTicks--;
        if (checkTicks > 0) {
            return false;
        }

        checkTicks = 20;
        return true;
    }

    private void warnOpsOnce(GameContext context, boolean dimensionWarning, String message) {
        if (dimensionWarning) {
            if (warnedInvalidDimension) {
                return;
            }
            warnedInvalidDimension = true;
        } else {
            if (warnedInvalidItem) {
                return;
            }
            warnedInvalidItem = true;
        }

        for (ServerPlayerEntity player : context.getServer().getPlayerManager().getPlayerList()) {
            if (HunterWildcardCommand.canManageGame(player.getCommandSource())) {
                player.sendMessage(Text.literal("[猎人外卡] " + message), false);
            }
        }
    }
}
