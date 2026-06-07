package com.xiaoming.hunterwildcard.mixin;

import com.xiaoming.hunterwildcard.game.GameManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class PlayerEntityDropInventoryMixin {
    @Inject(method = "dropInventory", at = @At("HEAD"), cancellable = true)
    private void hunterwildcard$dropInventory(ServerWorld world, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player
                && GameManager.getInstance().handleDeathInventoryDrop(player)) {
            ci.cancel();
        }
    }
}
