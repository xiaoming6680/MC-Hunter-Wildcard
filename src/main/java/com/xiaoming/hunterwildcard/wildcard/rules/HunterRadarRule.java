package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.util.PlayerUtil;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class HunterRadarRule implements WildcardRule {
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
                    hunter.sendMessage(HunterWildcardText.translatable("msg.wildcard.hunter_radar.other_dimension").formatted(Formatting.RED));
                }
                continue;
            }

            int distance = PlayerUtil.roundDistance(Math.sqrt(hunter.squaredDistanceTo(runner)));
            hunter.sendMessage(HunterWildcardText.translatable("msg.wildcard.hunter_radar.nearest", runner.getName().getString(), distance).formatted(Formatting.AQUA));
            if (alertedRunners.add(runner.getUuid())) {
                runner.sendMessage(HunterWildcardText.translatable("msg.wildcard.hunter_radar.warning").formatted(Formatting.YELLOW), true);
            }
        }
    }
}
