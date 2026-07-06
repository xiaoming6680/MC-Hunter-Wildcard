package com.xiaoming.hunterwildcard.game;

import com.xiaoming.hunterwildcard.command.HunterWildcardCommand;
import com.xiaoming.hunterwildcard.config.ModConfig;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;

public class WinConditionManager {
    private static final int[] SURVIVE_NOTICE_SECONDS = {
            180, 120, 60, 30, 15, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1
    };
    private static final int[] DISTANCE_NOTICE_THRESHOLDS = {
            5000, 2000, 1000, 700, 500, 300, 150, 100, 50
    };

    private int runningTicks;
    private int checkTicks;
    private boolean warnedInvalidDimension;
    private boolean warnedInvalidItem;
    private final Set<Integer> announcedSurviveSeconds = new HashSet<>();
    private int lastSurviveRemainingSeconds = -1;
    private int lastCollectedCount = -1;
    private boolean targetDimensionNoticeSent;
    private int nextDistanceThresholdIndex;
    private int pendingDistanceNoticeTicks;
    private int pendingDistanceNoticeDistance = -1;

    public void start() {
        resetTracking();
    }

    public void start(GameContext context) {
        resetTracking();
        showInitialObjectiveStatus(context);
    }

    public void clear(GameContext context) {
        if (context != null) {
            HunterWildcardPackets.sendObjectiveStatus(context, false, "", "");
        }
        clear();
    }

    public void clear() {
        resetTracking();
    }

    private void resetTracking() {
        runningTicks = 0;
        checkTicks = 0;
        warnedInvalidDimension = false;
        warnedInvalidItem = false;
        announcedSurviveSeconds.clear();
        lastSurviveRemainingSeconds = -1;
        lastCollectedCount = -1;
        targetDimensionNoticeSent = false;
        nextDistanceThresholdIndex = 0;
        pendingDistanceNoticeTicks = 0;
        pendingDistanceNoticeDistance = -1;
    }

    public String tick(GameContext context) {
        ModConfig config = context.getConfig();
        runningTicks++;
        return switch (config.getRunnerVictoryType()) {
            case DRAGON -> null;
            case SURVIVE_TIME -> checkSurviveTime(context);
            case REACH_LOCATION -> checkReachLocation(context);
            case COLLECT_ITEM -> shouldRunSlowCheck() ? checkCollectItem(context) : null;
        };
    }

    public String onDragonKilled(GameContext context) {
        if (context.getConfig().getRunnerVictoryType() == RunnerVictoryType.DRAGON) {
            return HunterWildcardText.spec("msg.win.runner.dragon");
        }

        return null;
    }

    private String checkSurviveTime(GameContext context) {
        ModConfig config = context.getConfig();
        if (runningTicks >= config.getSurviveTimeTicks()) {
            return HunterWildcardText.spec("msg.win.runner.survive_time", config.surviveTimeSeconds);
        }

        updateSurviveObjective(context);
        return null;
    }

    private String checkReachLocation(GameContext context) {
        tickPendingDistanceNotice(context);

        ModConfig config = context.getConfig();
        Identifier dimensionId = Identifier.tryParse(config.targetDimension);
        if (dimensionId == null) {
            warnOpsOnce(context, true, HunterWildcardText.translatable("msg.win_config.invalid_dimension_id"));
            return null;
        }

        RegistryKey<World> targetWorldKey = RegistryKey.of(RegistryKeys.WORLD, dimensionId);
        ServerWorld targetWorld = context.getServer().getWorld(targetWorldKey);
        if (targetWorld == null) {
            warnOpsOnce(context, true, HunterWildcardText.translatable("msg.win_config.dimension_not_found", config.targetDimension));
            return null;
        }

        double radiusSquared = (double) config.targetRadius * config.targetRadius;
        double closestDistanceSquared = Double.MAX_VALUE;
        for (ServerPlayerEntity runner : context.getRunners()) {
            if (!runner.getEntityWorld().getRegistryKey().equals(targetWorld.getRegistryKey())) {
                continue;
            }

            double dx = runner.getX() - config.targetX;
            double dy = runner.getY() - config.targetY;
            double dz = runner.getZ() - config.targetZ;
            if (dx * dx + dy * dy + dz * dz <= radiusSquared) {
                return HunterWildcardText.spec("msg.win.runner.reach_location", config.targetDimension, config.targetX, config.targetY, config.targetZ);
            }
            double distanceSquared = dx * dx + dy * dy + dz * dz;
            if (distanceSquared < closestDistanceSquared) {
                closestDistanceSquared = distanceSquared;
            }
        }

        if (closestDistanceSquared < Double.MAX_VALUE) {
            int distance = Math.max(0, (int) Math.ceil(Math.sqrt(closestDistanceSquared)));
            handleReachLocationNotices(context, targetWorld.getRegistryKey(), distance);
        }

        return null;
    }

    private String checkCollectItem(GameContext context) {
        ModConfig config = context.getConfig();
        int total = countCollectedTargetItems(context);
        updateCollectObjective(context, total);
        if (total >= config.targetItemCount) {
            return HunterWildcardText.spec("msg.win.runner.collect_item", config.targetItemCount, config.targetItemId);
        }

        return null;
    }

    private void showInitialObjectiveStatus(GameContext context) {
        switch (context.getConfig().getRunnerVictoryType()) {
            case SURVIVE_TIME -> updateSurviveObjective(context);
            case COLLECT_ITEM -> updateCollectObjective(context, countCollectedTargetItems(context));
            default -> HunterWildcardPackets.sendObjectiveStatus(context, false, "", "");
        }
    }

    private void updateSurviveObjective(GameContext context) {
        ModConfig config = context.getConfig();
        int remainingTicks = Math.max(0, config.getSurviveTimeTicks() - runningTicks);
        int remainingSeconds = ticksToSeconds(remainingTicks);
        if (remainingSeconds != lastSurviveRemainingSeconds) {
            lastSurviveRemainingSeconds = remainingSeconds;
            HunterWildcardPackets.sendObjectiveStatus(
                    context,
                    true,
                    HunterWildcardText.spec("hud.objective.survive_remaining", remainingSeconds),
                    "time"
            );
        }

        if (isSurviveNoticeSecond(remainingSeconds) && announcedSurviveSeconds.add(remainingSeconds)) {
            broadcastToAll(context, HunterWildcardText.translatable("msg.objective.survive_notice", remainingSeconds));
        }
    }

    private void updateCollectObjective(GameContext context, int collectedCount) {
        ModConfig config = context.getConfig();
        int displayCount = Math.max(0, Math.min(collectedCount, config.targetItemCount));
        if (displayCount == lastCollectedCount) {
            return;
        }

        lastCollectedCount = displayCount;
        HunterWildcardPackets.sendObjectiveStatus(
                context,
                true,
                HunterWildcardText.spec("hud.objective.collect_progress", displayCount, config.targetItemCount),
                "item"
        );
    }

    private int countCollectedTargetItems(GameContext context) {
        ModConfig config = context.getConfig();
        Identifier itemId = Identifier.tryParse(config.targetItemId);
        if (itemId == null) {
            warnOpsOnce(context, false, HunterWildcardText.translatable("msg.win_config.invalid_item_id"));
            return 0;
        }

        Item targetItem = Registries.ITEM.getOptionalValue(itemId).orElse(null);
        if (targetItem == null) {
            warnOpsOnce(context, false, HunterWildcardText.translatable("msg.win_config.item_not_found", config.targetItemId));
            return 0;
        }

        int total = 0;
        for (ServerPlayerEntity runner : context.getRunners()) {
            for (int slot = 0; slot < runner.getInventory().size(); slot++) {
                ItemStack stack = runner.getInventory().getStack(slot);
                if (stack.isOf(targetItem)) {
                    total += stack.getCount();
                }
            }
        }

        return total;
    }

    private void handleReachLocationNotices(GameContext context, RegistryKey<World> targetWorldKey, int distance) {
        boolean sentDimensionNotice = false;
        if (!targetWorldKey.equals(World.OVERWORLD) && !targetDimensionNoticeSent) {
            targetDimensionNoticeSent = true;
            sentDimensionNotice = true;
            pendingDistanceNoticeTicks = 10;
            HunterWildcardPackets.sendObjectiveNotice(context, HunterWildcardText.spec("hud.objective.dimension_reached"), "coordinate");
        }

        int crossedIndex = crossedDistanceThresholdIndex(distance);
        if (crossedIndex < nextDistanceThresholdIndex) {
            return;
        }

        nextDistanceThresholdIndex = crossedIndex + 1;
        if (sentDimensionNotice || pendingDistanceNoticeTicks > 0) {
            scheduleDistanceNotice(distance);
        } else {
            sendDistanceNotice(context, distance);
        }
    }

    private int crossedDistanceThresholdIndex(int distance) {
        int crossedIndex = -1;
        for (int i = nextDistanceThresholdIndex; i < DISTANCE_NOTICE_THRESHOLDS.length; i++) {
            if (distance > DISTANCE_NOTICE_THRESHOLDS[i]) {
                break;
            }
            crossedIndex = i;
        }
        return crossedIndex;
    }

    private void scheduleDistanceNotice(int distance) {
        pendingDistanceNoticeTicks = pendingDistanceNoticeTicks <= 0 ? 10 : pendingDistanceNoticeTicks;
        pendingDistanceNoticeDistance = distance;
    }

    private void tickPendingDistanceNotice(GameContext context) {
        if (pendingDistanceNoticeTicks <= 0) {
            return;
        }

        pendingDistanceNoticeTicks--;
        if (pendingDistanceNoticeTicks <= 0 && pendingDistanceNoticeDistance >= 0) {
            sendDistanceNotice(context, pendingDistanceNoticeDistance);
            pendingDistanceNoticeDistance = -1;
        }
    }

    private void sendDistanceNotice(GameContext context, int distance) {
        HunterWildcardPackets.sendObjectiveNotice(context, HunterWildcardText.spec("hud.objective.distance_notice", distance), "coordinate");
    }

    private boolean isSurviveNoticeSecond(int seconds) {
        for (int noticeSecond : SURVIVE_NOTICE_SECONDS) {
            if (noticeSecond == seconds) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldRunSlowCheck() {
        checkTicks--;
        if (checkTicks > 0) {
            return false;
        }

        checkTicks = 20;
        return true;
    }

    private int ticksToSeconds(int ticks) {
        return Math.max(0, (ticks + 19) / 20);
    }

    private void broadcastToAll(GameContext context, Text message) {
        context.getServer().getPlayerManager().broadcast(
                HunterWildcardText.prefixed(message).formatted(Formatting.GOLD),
                false
        );
    }

    private void warnOpsOnce(GameContext context, boolean dimensionWarning, Text message) {
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
                player.sendMessage(HunterWildcardText.prefixed(message).formatted(Formatting.GOLD), false);
            }
        }
    }
}
