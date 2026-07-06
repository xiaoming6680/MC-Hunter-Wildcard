package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BlockDecayRule implements WildcardRule {
    private final List<DecayEntry> entries = new ArrayList<>();
    private int ticks;

    @Override
    public String getName() {
        return "方块腐化";
    }

    @Override
    public void onStart(GameContext context) {
        entries.clear();
        ticks = 0;
    }

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        ticks++;
        Iterator<DecayEntry> iterator = entries.iterator();
        while (iterator.hasNext()) {
            DecayEntry entry = iterator.next();
            if (ticks < entry.expireTick()) {
                continue;
            }

            ServerWorld world = context.getServer().getWorld(entry.worldKey());
            if (world != null && world.getBlockState(entry.pos()).equals(entry.state())) {
                world.removeBlock(entry.pos(), false);
            }
            iterator.remove();
        }
    }

    @Override
    public void onBlockPlaced(GameContext context, ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state) {
        if (isExcluded(state)) {
            return;
        }

        entries.add(new DecayEntry(world.getRegistryKey(), pos.toImmutable(), state, ticks + context.getConfig().getBlockDecayTicks()));
    }

    @Override
    public void onStop(GameContext context) {
        entries.clear();
    }

    private boolean isExcluded(BlockState state) {
        Block block = state.getBlock();
        return state.hasBlockEntity()
                || state.isOf(Blocks.BEDROCK)
                || state.isOf(Blocks.OBSIDIAN)
                || state.isOf(Blocks.CRYING_OBSIDIAN)
                || state.isOf(Blocks.CRAFTING_TABLE)
                || state.isOf(Blocks.FURNACE)
                || state.isOf(Blocks.BLAST_FURNACE)
                || block instanceof ChestBlock
                || block instanceof DoorBlock
                || block instanceof TrapdoorBlock
                || block instanceof BedBlock
                || block instanceof AbstractFurnaceBlock
                || block instanceof CraftingTableBlock;
    }

    private record DecayEntry(RegistryKey<World> worldKey, BlockPos pos, BlockState state, int expireTick) {
    }
}
