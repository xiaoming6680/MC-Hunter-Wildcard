package com.xiaoming.hunterwildcard.wildcard;

import com.xiaoming.hunterwildcard.config.ModConfig;
import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets;
import com.xiaoming.hunterwildcard.ui.BossBarManager;
import com.xiaoming.hunterwildcard.ui.MessageManager;
import com.xiaoming.hunterwildcard.wildcard.rules.BlockDecayRule;
import com.xiaoming.hunterwildcard.wildcard.rules.BloodRageRule;
import com.xiaoming.hunterwildcard.wildcard.rules.CompassChaosRule;
import com.xiaoming.hunterwildcard.wildcard.rules.DisabledWildcardRule;
import com.xiaoming.hunterwildcard.wildcard.rules.ExplosiveDeathRule;
import com.xiaoming.hunterwildcard.wildcard.rules.FeatherweightRule;
import com.xiaoming.hunterwildcard.wildcard.rules.GlowingRule;
import com.xiaoming.hunterwildcard.wildcard.rules.HungerChaseRule;
import com.xiaoming.hunterwildcard.wildcard.rules.HunterRadarRule;
import com.xiaoming.hunterwildcard.wildcard.rules.LightLoadRule;
import com.xiaoming.hunterwildcard.wildcard.rules.NightHuntRule;
import com.xiaoming.hunterwildcard.wildcard.rules.PearlFrenzyRule;
import com.xiaoming.hunterwildcard.wildcard.rules.SpeedRushRule;
import com.xiaoming.hunterwildcard.wildcard.rules.SupplyDropRule;
import com.xiaoming.hunterwildcard.wildcard.rules.WeaponOverheatRule;
import com.xiaoming.hunterwildcard.wildcard.rules.WindChargeBrawlRule;
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

    private final List<WildcardRule> rules = List.of(
            new SpeedRushRule(),
            new FeatherweightRule(),
            new GlowingRule(),
            new NightHuntRule(),
            new ExplosiveDeathRule(),
            new SupplyDropRule(),
            new HunterRadarRule(),
            new CompassChaosRule(),
            new HungerChaseRule(),
            new WeaponOverheatRule(),
            new LightLoadRule(),
            new BlockDecayRule(),
            new PearlFrenzyRule(),
            new WindChargeBrawlRule(),
            new BloodRageRule(),
            new DisabledWildcardRule()
    );
    private final BossBarManager bossBarManager;
    private final MessageManager messageManager;

    private WildcardRule activeRule;
    private WildcardRule pendingRule;
    private Class<?> lastRuleClass;
    private int activeRemainingTicks;
    private int pendingDrawTicks;
    private int ticksUntilNextWildcard = -1;

    public WildcardManager(BossBarManager bossBarManager, MessageManager messageManager) {
        this.bossBarManager = bossBarManager;
        this.messageManager = messageManager;
    }

    public void reset() {
        activeRule = null;
        pendingRule = null;
        activeRemainingTicks = 0;
        pendingDrawTicks = 0;
        ticksUntilNextWildcard = -1;
    }

    public void tick(GameContext context) {
        if (pendingRule != null) {
            pendingDrawTicks--;
            if (pendingDrawTicks <= 0) {
                activatePendingRule(context);
            }
            return;
        }

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
        if (activeRule != null && isParticipant(context, player)) {
            activeRule.onPlayerDeath(context, player);
        }
    }

    public void onEntityKilled(GameContext context, ServerPlayerEntity killer, LivingEntity killed) {
        if (activeRule != null && isParticipant(context, killer)) {
            activeRule.onEntityKilled(context, killer, killed);
        }
    }

    public void onPlayerAttack(GameContext context, ServerPlayerEntity player, Entity target) {
        if (activeRule != null && isParticipant(context, player)) {
            activeRule.onPlayerAttack(context, player, target);
        }
    }

    public void onPlayerAteFood(GameContext context, ServerPlayerEntity player, ItemStack eatenStack) {
        if (activeRule != null && isParticipant(context, player)) {
            activeRule.onPlayerAteFood(context, player, eatenStack);
        }
    }

    public void onItemUse(GameContext context, ServerPlayerEntity player, Hand hand, ItemStack stack) {
        if (activeRule != null && isParticipant(context, player)) {
            activeRule.onItemUse(context, player, hand, stack);
        }
    }

    public void onBlockPlaced(GameContext context, ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state) {
        if (activeRule != null && isParticipant(context, player)) {
            activeRule.onBlockPlaced(context, player, world, pos, state);
        }
    }

    public void clear(GameContext context) {
        if (activeRule != null) {
            activeRule.onStop(context);
        }

        activeRule = null;
        pendingRule = null;
        activeRemainingTicks = 0;
        pendingDrawTicks = 0;
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

        for (WildcardRule rule : rules) {
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
        return activeRule == null ? -1 : Math.max(0, activeRemainingTicks);
    }

    public int getTicksUntilNextWildcard() {
        return activeRule == null && pendingRule == null ? ticksUntilNextWildcard : -1;
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
        pendingRule = rule;
        lastRuleClass = pendingRule.getClass();
        pendingDrawTicks = WILDCARD_DRAW_DELAY_TICKS;
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
        pendingDrawTicks = 0;
        activeRemainingTicks = context.getConfig().getWildcardDurationTicks();

        activeRule.onStart(context);
        bossBarManager.updateWildcardBar(context, activeRule.getName(), activeRemainingTicks, context.getConfig().getWildcardDurationTicks());
        HunterWildcardPackets.sendWildcardIntro(context, activeRule.getName(), activeRule.getDescription());
        messageManager.toParticipants(context, "外卡触发: " + activeRule.getName());
    }

    private void cancelPendingRule(GameContext context, boolean resetInterval) {
        if (pendingRule != null) {
            messageManager.toParticipants(context, "外卡抽取已取消: " + pendingRule.getName());
        }

        pendingRule = null;
        pendingDrawTicks = 0;
        ticksUntilNextWildcard = resetInterval ? context.getConfig().getWildcardIntervalTicks() : -1;
        bossBarManager.clearWildcardBar();
    }

    private void stopActiveRuleInternal(GameContext context, boolean resetInterval) {
        if (activeRule != null) {
            messageManager.toParticipants(context, "外卡结束: " + activeRule.getName());
            activeRule.onStop(context);
        }

        activeRule = null;
        activeRemainingTicks = 0;
        ticksUntilNextWildcard = resetInterval ? context.getConfig().getWildcardIntervalTicks() : -1;
        bossBarManager.clearWildcardBar();
        HunterWildcardPackets.clearWildcardIntro(context);
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
