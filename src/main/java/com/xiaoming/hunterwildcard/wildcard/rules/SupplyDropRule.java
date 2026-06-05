package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.List;
import java.util.Random;

public class SupplyDropRule implements WildcardRule {
    @Override
    public String getName() {
        return "补给空投";
    }

    @Override
    public void onStart(GameContext context) {
        spawnSupplyDrop(context);
    }

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        if (remainingTicks > 0 && remainingTicks % context.getConfig().getSupplyDropIntervalTicks() == 0) {
            spawnSupplyDrop(context);
        }
    }

    private void spawnSupplyDrop(GameContext context) {
        List<ServerPlayerEntity> players = context.getParticipants();
        if (players.isEmpty()) {
            return;
        }

        ServerPlayerEntity target = players.get(context.getRandom().nextInt(players.size()));
        ServerWorld world = target.getEntityWorld();
        BlockPos chestPos = findDropPosition(world, target.getBlockPos(), context.getRandom());
        if (chestPos == null) {
            return;
        }

        world.setBlockState(chestPos, Blocks.CHEST.getDefaultState(), Block.NOTIFY_ALL);
        if (world.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            chest.setStack(0, new ItemStack(Items.COOKED_BEEF, 8));
            chest.setStack(1, new ItemStack(Items.ARROW, 16));
            chest.setStack(2, new ItemStack(Items.IRON_INGOT, 4));
            chest.setStack(3, new ItemStack(Items.GOLDEN_APPLE, 1));
            chest.setStack(4, new ItemStack(Items.WATER_BUCKET, 1));
            chest.setStack(5, new ItemStack(Items.TORCH, 16));
        }
    }

    private BlockPos findDropPosition(ServerWorld world, BlockPos center, Random random) {
        for (int attempt = 0; attempt < 12; attempt++) {
            int distance = 16 + random.nextInt(33);
            int dx = random.nextBoolean() ? distance : -distance;
            int dz = random.nextBoolean() ? random.nextInt(distance + 1) : -random.nextInt(distance + 1);
            BlockPos searchPos = center.add(dx, 0, dz);
            BlockPos topPos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, searchPos);
            if (world.getBlockState(topPos).isAir()) {
                return topPos;
            }
        }

        return null;
    }
}
