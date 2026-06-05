package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.PlayerUtil;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class HunterRadarRule implements WildcardRule {
    @Override
    public String getName() {
        return "HunterRadar";
    }

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        if (remainingTicks <= 0 || remainingTicks % context.getConfig().hunterRadarIntervalTicks != 0) {
            return;
        }

        for (ServerPlayerEntity hunter : context.getHunters()) {
            ServerPlayerEntity runner = PlayerUtil.findNearestRunner(hunter, context.getRunners());
            if (runner == null) {
                continue;
            }

            if (hunter.getEntityWorld() != runner.getEntityWorld()) {
                hunter.sendMessage(Text.literal("雷达: 最近的逃亡者在其他维度。").formatted(Formatting.RED));
                continue;
            }

            int distance = PlayerUtil.roundDistance(Math.sqrt(hunter.squaredDistanceTo(runner)));
            hunter.sendMessage(Text.literal("雷达: 最近逃亡者约 " + distance + " 米。").formatted(Formatting.AQUA));
        }
    }
}
