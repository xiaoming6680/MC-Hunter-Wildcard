package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

public class CompassChaosRule implements WildcardRule {
    @Override
    public String getName() {
        return "指南针干扰";
    }

    @Override
    public BlockPos modifyCompassTarget(GameContext context, ServerPlayerEntity hunter, ServerPlayerEntity runner, BlockPos realTarget) {
        int distance = 30 + context.getRandom().nextInt(51);
        double angle = context.getRandom().nextDouble() * Math.PI * 2.0;
        int dx = (int) Math.round(Math.cos(angle) * distance);
        int dz = (int) Math.round(Math.sin(angle) * distance);
        BlockPos fakePos = realTarget.add(dx, 0, dz);
        return runner.getEntityWorld().getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, fakePos);
    }
}
