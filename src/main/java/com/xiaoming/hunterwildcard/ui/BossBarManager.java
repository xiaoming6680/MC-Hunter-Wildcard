package com.xiaoming.hunterwildcard.ui;

import com.xiaoming.hunterwildcard.game.GameContext;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class BossBarManager {
    private ServerBossBar prepareBar;
    private ServerBossBar wildcardBar;

    public void updatePrepareBar(GameContext context, int remainingTicks, int totalTicks) {
        if (prepareBar == null) {
            prepareBar = new ServerBossBar(Text.literal("准备阶段"), BossBar.Color.YELLOW, BossBar.Style.PROGRESS);
        }

        prepareBar.clearPlayers();
        for (ServerPlayerEntity player : context.getParticipants()) {
            prepareBar.addPlayer(player);
        }

        int seconds = Math.max(0, (remainingTicks + 19) / 20);
        float percent = totalTicks <= 0 ? 0.0F : Math.max(0.0F, Math.min(1.0F, remainingTicks / (float) totalTicks));
        prepareBar.setName(Text.literal("准备阶段 | " + seconds + " 秒后开始追杀"));
        prepareBar.setPercent(percent);
        prepareBar.setVisible(true);
    }

    public void updateWildcardBar(GameContext context, String ruleName, int remainingTicks, int totalTicks) {
        if (wildcardBar == null) {
            wildcardBar = new ServerBossBar(Text.literal("外卡"), BossBar.Color.PURPLE, BossBar.Style.PROGRESS);
        }

        wildcardBar.clearPlayers();
        for (ServerPlayerEntity player : context.getParticipants()) {
            wildcardBar.addPlayer(player);
        }

        int seconds = Math.max(0, remainingTicks / 20);
        float percent = totalTicks <= 0 ? 0.0F : Math.max(0.0F, Math.min(1.0F, remainingTicks / (float) totalTicks));
        wildcardBar.setName(Text.literal("外卡: " + ruleName + " | 剩余 " + seconds + " 秒"));
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
