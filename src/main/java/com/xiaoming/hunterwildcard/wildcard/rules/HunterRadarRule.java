package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.PlayerUtil;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class HunterRadarRule implements WildcardRule {
    @Override
    public String getName() {
        return "猎人雷达";
    }

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        if (remainingTicks <= 0 || remainingTicks % context.getConfig().getHunterRadarIntervalTicks() != 0) {
            return;
        }

        Set<UUID> alertedRunners = new HashSet<>();
        for (ServerPlayerEntity hunter : context.getHunters()) {
            ServerPlayerEntity runner = PlayerUtil.findNearestRunnerInSameWorld(hunter, context.getRunners());
            if (runner == null) {
                if (!context.getRunners().isEmpty()) {
                    hunter.sendMessage(Text.literal("雷达: 最近的逃亡者在其他维度。").formatted(Formatting.RED));
                }
                continue;
            }

            int distance = PlayerUtil.roundDistance(Math.sqrt(hunter.squaredDistanceTo(runner)));
            hunter.sendMessage(Text.literal("雷达: 最近逃亡者 " + runner.getName().getString() + " 约 " + distance + " 米。").formatted(Formatting.AQUA));
            if (alertedRunners.add(runner.getUuid())) {
                runner.sendMessage(Text.literal("雷达预警: 你被猎人雷达探测到了。").formatted(Formatting.YELLOW), true);
            }
        }
    }
}
