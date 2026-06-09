package com.xiaoming.hunterwildcard.mixin;

import com.xiaoming.hunterwildcard.game.GameManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ConsumableComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ConsumableComponent.class)
public class ItemFinishUsingMixin {
    @Inject(method = "finishConsumption", at = @At("HEAD"))
    private void hunterwildcard$beforeFinishConsumption(World world, LivingEntity user, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        if (!world.isClient() && user instanceof ServerPlayerEntity player && stack.contains(DataComponentTypes.FOOD)) {
            GameManager.getInstance().handlePlayerAteFood(player, stack.copy());
        }
    }
}
