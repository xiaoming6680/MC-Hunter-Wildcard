package com.xiaoming.hunterwildcard.wildcard;

import com.xiaoming.hunterwildcard.config.ModConfig;
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
            bossBarManager.updateWildcardBar(context, activeRule.getName(), activeRemainingTicks, context.getConfig().getWildcardDurationTicks());

            if (activeRemainingTicks <= 0) {
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

    public void onConfigChanged(ModConfig config) {
        if (ticksUntilNextWildcard > config.getWildcardIntervalTicks()) {
            ticksUntilNextWildcard = config.getWildcardIntervalTicks();
        }
    }

    public boolean rollNow(GameContext context) {
        if (activeRule != null) {
            stopActiveRule(context);
        }

        return startRandomRule(context);
    }

    public boolean startRuleByName(GameContext context, String ruleName) {
        if (activeRule != null) {
            stopActiveRule(context);
        }

        for (WildcardRule rule : rules) {
            if (rule.getName().equals(ruleName) && context.getConfig().isWildcardEnabled(rule.getName())) {
                return startRule(context, rule);
            }
        }

        messageManager.toParticipants(context, "没有可用外卡: " + ruleName);
        return false;
    }

    public boolean stopActiveRule(GameContext context) {
        if (activeRule == null) {
            return false;
        }

        stopActiveRuleInternal(context, true);
        return true;
    }

    public WildcardRule getActiveRule() {
        return activeRule;
    }

    public String getActiveRuleName() {
        return activeRule == null ? null : activeRule.getName();
    }

    public List<WildcardStatus> getRuleStatuses(ModConfig config) {
        List<WildcardStatus> statuses = new ArrayList<>();
        for (WildcardRule rule : rules) {
            statuses.add(new WildcardStatus(rule.getName(), config.isWildcardEnabled(rule.getName())));
        }
        return statuses;
    }

    private boolean startRandomRule(GameContext context) {
        List<WildcardRule> candidates = getEnabledRules(context.getConfig());
        if (lastRuleClass != null && candidates.size() > 1) {
            candidates.removeIf(rule -> rule.getClass() == lastRuleClass);
        }

        if (candidates.isEmpty()) {
            ticksUntilNextWildcard = context.getConfig().getWildcardIntervalTicks();
            messageManager.toParticipants(context, "没有可用外卡。");
            return false;
        }

        return startRule(context, candidates.get(context.getRandom().nextInt(candidates.size())));
    }

    private boolean startRule(GameContext context, WildcardRule rule) {
        activeRule = rule;
        lastRuleClass = activeRule.getClass();
        activeRemainingTicks = context.getConfig().getWildcardDurationTicks();
        ticksUntilNextWildcard = -1;

        activeRule.onStart(context);
        bossBarManager.updateWildcardBar(context, activeRule.getName(), activeRemainingTicks, context.getConfig().getWildcardDurationTicks());
        messageManager.toParticipants(context, "外卡触发: " + activeRule.getName());
        return true;
    }

    private void stopActiveRuleInternal(GameContext context, boolean resetInterval) {
        if (activeRule != null) {
            messageManager.toParticipants(context, "外卡结束: " + activeRule.getName());
            activeRule.onStop(context);
        }

        activeRule = null;
        activeRemainingTicks = 0;
        ticksUntilNextWildcard = resetInterval ? context.getConfig().getWildcardIntervalTicks() : -1;
        bossBarManager.clear();
    }

    private List<WildcardRule> getEnabledRules(ModConfig config) {
        List<WildcardRule> enabledRules = new ArrayList<>();
        for (WildcardRule rule : rules) {
            if (config.isWildcardEnabled(rule.getName())) {
                enabledRules.add(rule);
            }
        }
        return enabledRules;
    }

    public record WildcardStatus(String name, boolean enabled) {
    }
}
