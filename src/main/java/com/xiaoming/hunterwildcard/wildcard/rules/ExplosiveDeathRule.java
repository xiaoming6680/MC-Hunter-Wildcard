package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

public class ExplosiveDeathRule implements WildcardRule {
    @Override
    public String getName() {
        return "死亡爆炸";
    }

    @Override
    public void onPlayerDeath(GameContext context, ServerPlayerEntity player) {
        player.getEntityWorld().createExplosion(player, player.getX(), player.getY(), player.getZ(), 2.5F, World.ExplosionSourceType.NONE);
    }
}
