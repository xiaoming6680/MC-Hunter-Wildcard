package com.xiaoming.hunterwildcard.ui;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.network.ServerPlayerEntity;

public class BossBarManager {
    private ServerBossBar prepareBar;
    private ServerBossBar wildcardBar;

    public void updatePrepareBar(GameContext context, int remainingTicks, int totalTicks) {
        if (prepareBar == null) {
            prepareBar = new ServerBossBar(HunterWildcardText.translatable("hud.bossbar.prepare.title"), BossBar.Color.YELLOW, BossBar.Style.PROGRESS);
        }

        prepareBar.clearPlayers();
        for (ServerPlayerEntity player : context.getParticipants()) {
            prepareBar.addPlayer(player);
        }

        int seconds = Math.max(0, (remainingTicks + 19) / 20);
        float percent = totalTicks <= 0 ? 0.0F : Math.max(0.0F, Math.min(1.0F, remainingTicks / (float) totalTicks));
        prepareBar.setName(HunterWildcardText.translatable("hud.bossbar.prepare.countdown", seconds));
        prepareBar.setPercent(percent);
        prepareBar.setVisible(true);
    }

    public void updateWildcardBar(GameContext context, String ruleId, int remainingTicks, int totalTicks) {
        if (wildcardBar == null) {
            wildcardBar = new ServerBossBar(HunterWildcardText.translatable("hud.bossbar.wildcard.title"), BossBar.Color.PURPLE, BossBar.Style.PROGRESS);
        }

        wildcardBar.clearPlayers();
        for (ServerPlayerEntity player : context.getParticipants()) {
            wildcardBar.addPlayer(player);
        }

        int seconds = Math.max(0, remainingTicks / 20);
        float percent = totalTicks <= 0 ? 0.0F : Math.max(0.0F, Math.min(1.0F, remainingTicks / (float) totalTicks));
        wildcardBar.setName(HunterWildcardText.translatable("hud.bossbar.wildcard.countdown", HunterWildcardText.wildcardName(ruleId), seconds));
        wildcardBar.setPercent(percent);
        wildcardBar.setVisible(true);
    }

    public void clearPrepareBar() {
        if (prepareBar != null) {
            prepareBar.clearPlayers();
            prepareBar.setVisible(false);
        }
    }

    public void clearWildcardBar() {
        if (wildcardBar != null) {
            wildcardBar.clearPlayers();
            wildcardBar.setVisible(false);
        }
    }

    public void clear() {
        clearPrepareBar();
        clearWildcardBar();
    }
}
