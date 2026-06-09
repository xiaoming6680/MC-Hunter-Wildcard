package com.xiaoming.hunterwildcard.mixin;

import com.xiaoming.hunterwildcard.game.GameManager;
import com.xiaoming.hunterwildcard.wildcard.rules.WindChargeBrawlRule;
import net.minecraft.entity.projectile.WindChargeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(WindChargeEntity.class)
public class WindChargeEntityMixin {
    @ModifyArg(
            method = "createExplosion",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;createExplosion(Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/damage/DamageSource;Lnet/minecraft/world/explosion/ExplosionBehavior;DDDFZLnet/minecraft/world/World$ExplosionSourceType;Lnet/minecraft/particle/ParticleEffect;Lnet/minecraft/particle/ParticleEffect;Lnet/minecraft/util/collection/WeightedPool;Lnet/minecraft/registry/entry/RegistryEntry;)V"
            ),
            index = 6
    )
    private float hunterwildcard$boostWindChargeExplosion(float power) {
        if (GameManager.getInstance().getWildcardManager().getActiveRule() instanceof WindChargeBrawlRule) {
            return power * WindChargeBrawlRule.EXPLOSION_POWER_MULTIPLIER;
        }

        return power;
    }
}
