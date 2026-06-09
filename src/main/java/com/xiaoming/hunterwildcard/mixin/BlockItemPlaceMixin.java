package com.xiaoming.hunterwildcard.mixin;

import com.xiaoming.hunterwildcard.game.GameManager;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class BlockItemPlaceMixin {
    @Inject(method = "place", at = @At("RETURN"))
    private void hunterwildcard$afterPlace(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (!cir.getReturnValue().isAccepted()
                || !(context.getWorld() instanceof ServerWorld world)
                || !(context.getPlayer() instanceof ServerPlayerEntity player)) {
            return;
        }

        BlockPos pos = context.getBlockPos();
        BlockState state = world.getBlockState(pos);
        if (!state.isAir()) {
            GameManager.getInstance().handleBlockPlaced(player, world, pos, state);
        }
    }
}
