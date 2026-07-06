package com.xiaoming.hunterwildcard.wildcard;

import com.xiaoming.hunterwildcard.config.ModConfig;
import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets;
import com.xiaoming.hunterwildcard.ui.BossBarManager;
import com.xiaoming.hunterwildcard.ui.MessageManager;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WildcardManager {
    private static final int WILDCARD_DRAW_DELAY_TICKS = 100;

    private final List<WildcardRule> registeredRules = WildcardRuleRegistry.createRegisteredRules();
    private final BossBarManager bossBarManager;
    private final MessageManager messageManager;

    private WildcardRule activeRule;
    private WildcardRule pendingRule;
    private Class<?> lastDrawnRuleClass;
    private int activeRuleRemainingTicks;
    private int pendingRuleDrawTicks;
    private int ticksUntilNextWildcard = -1;

    public WildcardManager(BossBarManager bossBarManager, MessageManager messageManager) {
        this.bossBarManager = bossBarManager;
        this.messageManager = messageManager;
    }

    public void reset() {
        activeRule = null;
        pendingRule = null;
        activeRuleRemainingTicks = 0;
        pendingRuleDrawTicks = 0;
        ticksUntilNextWildcard = -1;
    }

    public void tick(GameContext context) {
        if (pendingRule != null) {
            pendingRuleDrawTicks--;
            if (pendingRuleDrawTicks <= 0) {
                activatePendingRule(context);
            }
            return;
        }

        if (activeRule != null) {
            activeRuleRemainingTicks--;
            activeRule.onTick(context, activeRuleRemainingTicks);
            bossBarManager.updateWildcardBar(context, activeRule.getName(), activeRuleRemainingTicks, context.getConfig().getWildcardDurationTicks());

            if (activeRuleRemainingTicks <= 0) {
                stopActiveRuleInternal(context, true);
            }
            return;
        }

        if (ticksUntilNextWildcard < 0) {
            ticksUntilNextWildcard = context.getConfig().getWildcardIntervalTicks();
        }

        ticksUntilNextWildcard--;
        if (ticksUntilNextWildcard <= 0) {
            startRandomRule(context);
        }
    }

    public void onPlayerDeath(GameContext context, ServerPlayerEntity player) {
        if (shouldForwardRuleEvent(context, player)) {
            activeRule.onPlayerDeath(context, player);
        }
    }

    public void onEntityKilled(GameContext context, ServerPlayerEntity killer, LivingEntity killed) {
        if (shouldForwardRuleEvent(context, killer)) {
            activeRule.onEntityKilled(context, killer, killed);
        }
    }

    public void onPlayerAttack(GameContext context, ServerPlayerEntity player, Entity target) {
        if (shouldForwardRuleEvent(context, player)) {
            activeRule.onPlayerAttack(context, player, target);
        }
    }

    public void onPlayerAteFood(GameContext context, ServerPlayerEntity player, ItemStack eatenStack) {
        if (shouldForwardRuleEvent(context, player)) {
            activeRule.onPlayerAteFood(context, player, eatenStack);
        }
    }

    public void onItemUse(GameContext context, ServerPlayerEntity player, Hand hand, ItemStack stack) {
        if (shouldForwardRuleEvent(context, player)) {
            activeRule.onItemUse(context, player, hand, stack);
        }
    }

    public void onBlockPlaced(GameContext context, ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state) {
        if (shouldForwardRuleEvent(context, player)) {
            activeRule.onBlockPlaced(context, player, world, pos, state);
        }
    }

    public void clear(GameContext context) {
        if (activeRule != null) {
            activeRule.onStop(context);
        }

        activeRule = null;
        pendingRule = null;
        activeRuleRemainingTicks = 0;
        pendingRuleDrawTicks = 0;
        ticksUntilNextWildcard = -1;
        bossBarManager.clearWildcardBar();
        HunterWildcardPackets.clearWildcardIntro(context);
    }

    public void onConfigChanged(ModConfig config) {
        if (ticksUntilNextWildcard > config.getWildcardIntervalTicks()) {
            ticksUntilNextWildcard = config.getWildcardIntervalTicks();
        }
    }

    public boolean rollNow(GameContext context) {
        if (activeRule != null) {
            stopActiveRule(context);
        }
        if (pendingRule != null) {
            cancelPendingRule(context, false);
        }

        return startRandomRule(context);
    }

    public boolean startRuleByName(GameContext context, String ruleName) {
        if (activeRule != null) {
            stopActiveRule(context);
        }
        if (pendingRule != null) {
            cancelPendingRule(context, false);
        }

        for (WildcardRule rule : registeredRules) {
            if (rule.getName().equals(ruleName) && context.getConfig().isWildcardEnabled(rule.getName())) {
                return startRule(context, rule);
            }
        }

        messageManager.toParticipants(context, "没有可用外卡: " + ruleName);
        return false;
    }

    public boolean stopActiveRule(GameContext context) {
        if (pendingRule != null) {
            cancelPendingRule(context, true);
            return true;
        }

        if (activeRule == null) {
            return false;
        }

        stopActiveRuleInternal(context, true);
        return true;
    }

    public WildcardRule getActiveRule() {
        return activeRule;
    }

    public boolean hasRuleInProgress() {
        return activeRule != null || pendingRule != null;
    }

    public String getActiveRuleName() {
        if (activeRule != null) {
            return activeRule.getName();
        }
        return pendingRule == null ? null : pendingRule.getName();
    }

    public int getActiveRemainingTicks() {
        return activeRule == null ? -1 : Math.max(0, activeRuleRemainingTicks);
    }

    public int getTicksUntilNextWildcard() {
        return activeRule == null && pendingRule == null ? ticksUntilNextWildcard : -1;
    }

    public List<WildcardStatus> getRuleStatuses(ModConfig config) {
        List<WildcardStatus> statuses = new ArrayList<>();
        for (WildcardRule rule : registeredRules) {
            statuses.add(new WildcardStatus(rule.getName(), config.isWildcardEnabled(rule.getName())));
        }
        return statuses;
    }

    private boolean startRandomRule(GameContext context) {
        List<WildcardRule> candidates = getEnabledRules(context.getConfig());
        if (lastDrawnRuleClass != null && candidates.size() > 1) {
            candidates.removeIf(rule -> rule.getClass() == lastDrawnRuleClass);
        }

        if (candidates.isEmpty()) {
            ticksUntilNextWildcard = context.getConfig().getWildcardIntervalTicks();
            messageManager.toParticipants(context, "没有可用外卡。");
            return false;
        }

        return startRule(context, candidates.get(context.getRandom().nextInt(candidates.size())));
    }

    private boolean startRule(GameContext context, WildcardRule rule) {
        pendingRule = rule;
        lastDrawnRuleClass = pendingRule.getClass();
        pendingRuleDrawTicks = WILDCARD_DRAW_DELAY_TICKS;
        ticksUntilNextWildcard = -1;

        HunterWildcardPackets.sendWildcardDraw(context, pendingRule.getName());
        return true;
    }

    private void activatePendingRule(GameContext context) {
        if (pendingRule == null) {
            return;
        }

        activeRule = pendingRule;
        pendingRule = null;
        pendingRuleDrawTicks = 0;
        activeRuleRemainingTicks = context.getConfig().getWildcardDurationTicks();

        activeRule.onStart(context);
        bossBarManager.updateWildcardBar(context, activeRule.getName(), activeRuleRemainingTicks, context.getConfig().getWildcardDurationTicks());
        HunterWildcardPackets.sendWildcardIntro(context, activeRule.getName(), activeRule.getDescription());
        messageManager.toParticipants(context, "外卡触发: " + activeRule.getName());
    }

    private void cancelPendingRule(GameContext context, boolean resetInterval) {
        if (pendingRule != null) {
            messageManager.toParticipants(context, "外卡抽取已取消: " + pendingRule.getName());
        }

        pendingRule = null;
        pendingRuleDrawTicks = 0;
        ticksUntilNextWildcard = resetInterval ? context.getConfig().getWildcardIntervalTicks() : -1;
        bossBarManager.clearWildcardBar();
    }

    private void stopActiveRuleInternal(GameContext context, boolean resetInterval) {
        if (activeRule != null) {
            messageManager.toParticipants(context, "外卡结束: " + activeRule.getName());
            activeRule.onStop(context);
        }

        activeRule = null;
        activeRuleRemainingTicks = 0;
        ticksUntilNextWildcard = resetInterval ? context.getConfig().getWildcardIntervalTicks() : -1;
        bossBarManager.clearWildcardBar();
        HunterWildcardPackets.clearWildcardIntro(context);
    }

    private List<WildcardRule> getEnabledRules(ModConfig config) {
        List<WildcardRule> enabledRules = new ArrayList<>();
        for (WildcardRule rule : registeredRules) {
            if (config.isWildcardEnabled(rule.getName())) {
                enabledRules.add(rule);
            }
        }
        return enabledRules;
    }

    private boolean shouldForwardRuleEvent(GameContext context, ServerPlayerEntity player) {
        return activeRule != null && isParticipant(context, player);
    }

    private boolean isParticipant(GameContext context, ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        for (ServerPlayerEntity participant : context.getParticipants()) {
            if (participant.getUuid().equals(playerId)) {
                return true;
            }
        }
        return false;
    }

    public record WildcardStatus(String name, boolean enabled) {
    }
}
