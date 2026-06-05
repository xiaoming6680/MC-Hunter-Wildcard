package com.xiaoming.hunterwildcard.wildcard;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.ui.BossBarManager;
import com.xiaoming.hunterwildcard.ui.MessageManager;
import com.xiaoming.hunterwildcard.wildcard.rules.CompassChaosRule;
import com.xiaoming.hunterwildcard.wildcard.rules.ExplosiveDeathRule;
import com.xiaoming.hunterwildcard.wildcard.rules.FeatherweightRule;
import com.xiaoming.hunterwildcard.wildcard.rules.GlowingRule;
import com.xiaoming.hunterwildcard.wildcard.rules.HunterRadarRule;
import com.xiaoming.hunterwildcard.wildcard.rules.NightHuntRule;
import com.xiaoming.hunterwildcard.wildcard.rules.SpeedRushRule;
import com.xiaoming.hunterwildcard.wildcard.rules.SupplyDropRule;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

public class WildcardManager {
    private final List<WildcardRule> rules = List.of(
            new SpeedRushRule(),
            new FeatherweightRule(),
            new GlowingRule(),
            new NightHuntRule(),
            new ExplosiveDeathRule(),
            new SupplyDropRule(),
            new HunterRadarRule(),
            new CompassChaosRule()
    );
    private final BossBarManager bossBarManager;
    private final MessageManager messageManager;

    private WildcardRule activeRule;
    private Class<?> lastRuleClass;
    private int activeRemainingTicks;
    private int ticksUntilNextWildcard = -1;

    public WildcardManager(BossBarManager bossBarManager, MessageManager messageManager) {
        this.bossBarManager = bossBarManager;
        this.messageManager = messageManager;
    }

    public void reset() {
        activeRule = null;
        activeRemainingTicks = 0;
        ticksUntilNextWildcard = -1;
    }

    public void tick(GameContext context) {
        if (activeRule != null) {
            activeRemainingTicks--;
            activeRule.onTick(context, activeRemainingTicks);
            bossBarManager.updateWildcardBar(context, activeRule.getName(), activeRemainingTicks, context.getConfig().wildcardDurationTicks);

            if (activeRemainingTicks <= 0) {
                stopActiveRule(context);
            }
            return;
        }

        if (ticksUntilNextWildcard < 0) {
            ticksUntilNextWildcard = context.getConfig().wildcardIntervalTicks;
        }

        ticksUntilNextWildcard--;
        if (ticksUntilNextWildcard <= 0) {
            startRandomRule(context);
        }
    }

    public void onPlayerDeath(GameContext context, ServerPlayerEntity player) {
        if (activeRule != null) {
            activeRule.onPlayerDeath(context, player);
        }
    }

    public void clear(GameContext context) {
        if (activeRule != null) {
            activeRule.onStop(context);
        }

        activeRule = null;
        activeRemainingTicks = 0;
        ticksUntilNextWildcard = -1;
        bossBarManager.clear();
    }

    public WildcardRule getActiveRule() {
        return activeRule;
    }

    public String getActiveRuleName() {
        return activeRule == null ? null : activeRule.getName();
    }

    private void startRandomRule(GameContext context) {
        List<WildcardRule> candidates = new ArrayList<>(rules);
        if (lastRuleClass != null && candidates.size() > 1) {
            candidates.removeIf(rule -> rule.getClass() == lastRuleClass);
        }

        activeRule = candidates.get(context.getRandom().nextInt(candidates.size()));
        lastRuleClass = activeRule.getClass();
        activeRemainingTicks = context.getConfig().wildcardDurationTicks;
        ticksUntilNextWildcard = -1;

        activeRule.onStart(context);
        bossBarManager.updateWildcardBar(context, activeRule.getName(), activeRemainingTicks, context.getConfig().wildcardDurationTicks);
        messageManager.toParticipants(context, "外卡触发: " + activeRule.getName());
    }

    private void stopActiveRule(GameContext context) {
        if (activeRule != null) {
            messageManager.toParticipants(context, "外卡结束: " + activeRule.getName());
            activeRule.onStop(context);
        }

        activeRule = null;
        activeRemainingTicks = 0;
        ticksUntilNextWildcard = context.getConfig().wildcardIntervalTicks;
        bossBarManager.clear();
    }
}
