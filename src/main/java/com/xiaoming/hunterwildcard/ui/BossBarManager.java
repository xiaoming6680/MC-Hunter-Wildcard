package com.xiaoming.hunterwildcard.ui;

import com.xiaoming.hunterwildcard.game.GameContext;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class BossBarManager {
    private ServerBossBar wildcardBar;

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

    public void clear() {
        if (wildcardBar != null) {
            wildcardBar.clearPlayers();
            wildcardBar.setVisible(false);
        }
    }
}
