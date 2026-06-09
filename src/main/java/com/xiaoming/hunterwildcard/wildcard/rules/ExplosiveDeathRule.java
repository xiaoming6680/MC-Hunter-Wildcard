package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

public class ExplosiveDeathRule implements WildcardRule {
    private static final float EXPLOSION_POWER = 2.5F;

    @Override
    public String getName() {
        return "死亡爆炸";
    }

    @Override
    public void onPlayerDeath(GameContext context, ServerPlayerEntity player) {
        explodeAt(player);
    }

    @Override
    public void onEntityKilled(GameContext context, ServerPlayerEntity killer, LivingEntity killed) {
        explodeAt(killed);
    }

    private void explodeAt(LivingEntity entity) {
        entity.getEntityWorld().createExplosion(entity, entity.getX(), entity.getY(), entity.getZ(), EXPLOSION_POWER, World.ExplosionSourceType.NONE);
    }
}
