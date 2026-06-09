package com.xiaoming.hunterwildcard.wildcard.rules;

import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.wildcard.WildcardRule;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class SupplyDropRule implements WildcardRule {
    private static final int MIN_DISTANCE = 18;
    private static final int MAX_DISTANCE = 54;
    private static final int DROP_HEIGHT = 44;
    private static final int MAX_FALL_TICKS = 220;
    private static final int LANDING_BEAM_TICKS = 20 * 20;
    private static final int BEAM_HEIGHT = 42;
    private static final int MIN_USEFUL_STACKS = 4;
    private static final int EXTRA_USEFUL_STACKS = 3;
    private static final int MIN_JUNK_STACKS = 1;
    private static final int EXTRA_JUNK_STACKS = 3;
    private static final List<ActiveDrop> ACTIVE_DROPS = new ArrayList<>();
    private static final Random LOOT_RANDOM = new Random();
    private static final List<LootEntry> RARE_GEAR_POOL = List.of(
            new LootEntry(Items.DIAMOND_SWORD, 1, 1),
            new LootEntry(Items.DIAMOND_AXE, 1, 1),
            new LootEntry(Items.DIAMOND_PICKAXE, 1, 1),
            new LootEntry(Items.DIAMOND_SPEAR, 1, 1),
            new LootEntry(Items.MACE, 1, 1),
            new LootEntry(Items.DIAMOND_HELMET, 1, 1),
            new LootEntry(Items.DIAMOND_CHESTPLATE, 1, 1),
            new LootEntry(Items.DIAMOND_LEGGINGS, 1, 1),
            new LootEntry(Items.DIAMOND_BOOTS, 1, 1)
    );
    private static final List<LootEntry> USEFUL_LOOT_POOL = List.of(
            new LootEntry(Items.COOKED_BEEF, 5, 10),
            new LootEntry(Items.COOKED_PORKCHOP, 5, 10),
            new LootEntry(Items.COOKED_CHICKEN, 5, 10),
            new LootEntry(Items.GOLDEN_CARROT, 3, 6),
            new LootEntry(Items.GOLDEN_APPLE, 1, 2),
            new LootEntry(Items.ENCHANTED_GOLDEN_APPLE, 1, 1),
            new LootEntry(Items.ARROW, 16, 40),
            new LootEntry(Items.SPECTRAL_ARROW, 6, 14),
            new LootEntry(Items.IRON_INGOT, 4, 10),
            new LootEntry(Items.DIAMOND, 1, 2),
            new LootEntry(Items.IRON_SWORD, 1, 1),
            new LootEntry(Items.IRON_AXE, 1, 1),
            new LootEntry(Items.IRON_CHESTPLATE, 1, 1),
            new LootEntry(Items.SHIELD, 1, 1),
            new LootEntry(Items.BOW, 1, 1),
            new LootEntry(Items.CROSSBOW, 1, 1),
            new LootEntry(Items.WATER_BUCKET, 1, 1),
            new LootEntry(Items.LAVA_BUCKET, 1, 1),
            new LootEntry(Items.MILK_BUCKET, 1, 1),
            new LootEntry(Items.HONEY_BOTTLE, 2, 5),
            new LootEntry(Items.TORCH, 12, 32),
            new LootEntry(Items.OAK_PLANKS, 12, 32),
            new LootEntry(Items.COBBLESTONE, 12, 32),
            new LootEntry(Items.WIND_CHARGE, 2, 5),
            new LootEntry(Items.ENDER_PEARL, 1, 3),
            new LootEntry(Items.EXPERIENCE_BOTTLE, 4, 10),
            new LootEntry(Items.FIREWORK_ROCKET, 4, 10)
    );
    private static final List<LootEntry> JUNK_LOOT_POOL = List.of(
            new LootEntry(Items.ROTTEN_FLESH, 3, 8),
            new LootEntry(Items.POISONOUS_POTATO, 2, 6),
            new LootEntry(Items.SPIDER_EYE, 2, 5),
            new LootEntry(Items.STICK, 8, 20),
            new LootEntry(Items.BOWL, 2, 6),
            new LootEntry(Items.GRAVEL, 12, 32),
            new LootEntry(Items.DIRT, 12, 32),
            new LootEntry(Items.STRING, 4, 10),
            new LootEntry(Items.BONE, 4, 10),
            new LootEntry(Items.COBWEB, 2, 5)
    );

    private int ticks;

    public static void tickTrackedDrops(MinecraftServer server) {
        if (ACTIVE_DROPS.isEmpty()) {
            return;
        }

        Iterator<ActiveDrop> iterator = ACTIVE_DROPS.iterator();
        while (iterator.hasNext()) {
            ActiveDrop drop = iterator.next();
            drop.age++;
            ServerWorld world = server.getWorld(drop.worldKey);
            if (world == null) {
                iterator.remove();
                continue;
            }

            if (drop.chestPos != null) {
                if (tickLandedDrop(world, drop)) {
                    iterator.remove();
                }
                continue;
            }

            Entity entity = world.getEntityAnyDimension(drop.fallingEntityUuid);
            if (entity != null && !entity.isRemoved() && drop.age <= MAX_FALL_TICKS) {
                spawnFallingTrail(world, entity, drop);
                continue;
            }

            BlockPos chestPos = findLandedChest(world, drop.landingPos);
            if (chestPos == null && drop.age > MAX_FALL_TICKS) {
                if (entity != null && !entity.isRemoved()) {
                    entity.discard();
                }
                chestPos = forcePlaceChest(world, drop.landingPos);
            }
            if (chestPos != null && world.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
                fillChest(chest, LOOT_RANDOM);
                drop.chestPos = chestPos;
                drop.beamActive = true;
                drop.beamExpireAge = drop.age + LANDING_BEAM_TICKS;
                playLandingEffects(world, chestPos);
            }
        }
    }

    @Override
    public String getName() {
        return "补给空投";
    }

    @Override
    public void onStart(GameContext context) {
        ticks = 0;
        spawnSupplyDrop(context);
    }

    @Override
    public void onTick(GameContext context, int remainingTicks) {
        ticks++;
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
        DropPoint point = findDropPosition(world, target.getBlockPos(), context.getRandom());
        if (point == null) {
            return;
        }

        BlockState chestState = Blocks.CHEST.getDefaultState();
        if (!world.setBlockState(point.spawnPos, chestState, Block.NOTIFY_ALL)) {
            return;
        }
        FallingBlockEntity fallingChest = FallingBlockEntity.spawnFromBlock(world, point.spawnPos, chestState);
        fallingChest.dropItem = false;
        fallingChest.setGlowing(true);
        fallingChest.setVelocity(0.0, -0.18, 0.0);
        ACTIVE_DROPS.add(new ActiveDrop(world.getRegistryKey(), point.landingPos, point.spawnPos, fallingChest.getUuid(), createBeaconMarker(world, point.landingPos)));

        playDropStartEffects(world, point.landingPos);
    }

    private static boolean tickLandedDrop(ServerWorld world, ActiveDrop drop) {
        if (!(world.getBlockEntity(drop.chestPos) instanceof ChestBlockEntity chest)) {
            restoreBeaconMarker(world, drop);
            return true;
        }

        boolean openedNow = !chest.getViewingUsers().isEmpty();
        if (openedNow && !drop.opened) {
            drop.opened = true;
            drop.beamActive = false;
            playBeamStopEffects(world, drop.chestPos, true);
            restoreBeaconMarker(world, drop);
        }
        if (!drop.opened && drop.age >= drop.beamExpireAge) {
            removeUnopenedChest(world, drop);
            return true;
        }

        if (drop.beamActive) {
            spawnBeaconBeam(world, drop.chestPos);
        }

        if (!chest.isEmpty()) {
            if (drop.age % 10 == 0) {
                spawnGroundMarker(world, drop.chestPos);
            }
            return false;
        }

        world.spawnParticles(ParticleTypes.CLOUD, drop.chestPos.getX() + 0.5, drop.chestPos.getY() + 0.8, drop.chestPos.getZ() + 0.5, 16, 0.25, 0.2, 0.25, 0.02);
        world.spawnParticles(ParticleTypes.END_ROD, drop.chestPos.getX() + 0.5, drop.chestPos.getY() + 0.9, drop.chestPos.getZ() + 0.5, 8, 0.2, 0.25, 0.2, 0.01);
        world.playSound(null, drop.chestPos, SoundEvents.BLOCK_CHEST_CLOSE, SoundCategory.BLOCKS, 0.8F, 1.3F);
        world.removeBlock(drop.chestPos, false);
        restoreBeaconMarker(world, drop);
        return true;
    }

    private DropPoint findDropPosition(ServerWorld world, BlockPos center, Random random) {
        int maxY = world.getDimension().minY() + world.getDimension().height() - 2;
        for (int attempt = 0; attempt < 24; attempt++) {
            int distance = MIN_DISTANCE + random.nextInt(MAX_DISTANCE - MIN_DISTANCE + 1);
            int dx = random.nextInt(distance * 2 + 1) - distance;
            int dz = random.nextInt(distance * 2 + 1) - distance;
            if (Math.abs(dx) < MIN_DISTANCE / 2 && Math.abs(dz) < MIN_DISTANCE / 2) {
                continue;
            }
            BlockPos searchPos = center.add(dx, 0, dz);
            BlockPos landingPos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, searchPos);
            int spawnY = Math.min(landingPos.getY() + DROP_HEIGHT, maxY);
            if (spawnY <= landingPos.getY() + 8) {
                continue;
            }
            BlockPos spawnPos = new BlockPos(landingPos.getX(), spawnY, landingPos.getZ());
            if (world.getBlockState(landingPos).isAir() && world.getBlockState(landingPos.up()).isAir() && world.getBlockState(spawnPos).isAir()) {
                return new DropPoint(landingPos.toImmutable(), spawnPos.toImmutable());
            }
        }

        return null;
    }

    private static BlockPos findLandedChest(ServerWorld world, BlockPos center) {
        for (BlockPos pos : BlockPos.iterate(center.add(-1, -2, -1), center.add(1, 3, 1))) {
            if (world.getBlockEntity(pos) instanceof ChestBlockEntity) {
                return pos.toImmutable();
            }
        }
        return null;
    }

    private static BlockPos forcePlaceChest(ServerWorld world, BlockPos landingPos) {
        BlockPos placePos = landingPos;
        if (!world.getBlockState(placePos).isAir()) {
            placePos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, landingPos);
        }
        if (!world.getBlockState(placePos).isAir()) {
            return null;
        }
        world.setBlockState(placePos, Blocks.CHEST.getDefaultState(), Block.NOTIFY_ALL);
        return world.getBlockEntity(placePos) instanceof ChestBlockEntity ? placePos.toImmutable() : null;
    }

    private static void fillChest(ChestBlockEntity chest, Random random) {
        for (int slot = 0; slot < chest.size(); slot++) {
            chest.setStack(slot, ItemStack.EMPTY);
        }

        List<LootEntry> gear = new ArrayList<>(RARE_GEAR_POOL);
        Collections.shuffle(gear, random);
        int gearCount = Math.min(gear.size(), randomRareGearCount(random));
        for (int i = 0; i < gearCount; i++) {
            if (!placeRandomStack(chest, gear.get(i), random)) {
                return;
            }
        }

        List<LootEntry> loot = new ArrayList<>(USEFUL_LOOT_POOL);
        Collections.shuffle(loot, random);
        int stackCount = Math.min(loot.size(), MIN_USEFUL_STACKS + random.nextInt(EXTRA_USEFUL_STACKS + 1));
        for (int i = 0; i < stackCount; i++) {
            if (!placeRandomStack(chest, loot.get(i), random)) {
                return;
            }
        }

        List<LootEntry> junk = new ArrayList<>(JUNK_LOOT_POOL);
        Collections.shuffle(junk, random);
        int junkCount = Math.min(junk.size(), MIN_JUNK_STACKS + random.nextInt(EXTRA_JUNK_STACKS + 1));
        for (int i = 0; i < junkCount; i++) {
            if (!placeRandomStack(chest, junk.get(i), random)) {
                return;
            }
        }
        chest.markDirty();
    }

    private static int randomRareGearCount(Random random) {
        int roll = random.nextInt(100);
        if (roll < 45) {
            return 0;
        }
        if (roll < 85) {
            return 1;
        }
        return 2;
    }

    private static boolean placeRandomStack(ChestBlockEntity chest, LootEntry entry, Random random) {
        int slot = randomEmptySlot(chest, random);
        if (slot < 0) {
            return false;
        }
        chest.setStack(slot, entry.createStack(random));
        return true;
    }

    private static int randomEmptySlot(ChestBlockEntity chest, Random random) {
        int start = random.nextInt(chest.size());
        for (int offset = 0; offset < chest.size(); offset++) {
            int slot = (start + offset) % chest.size();
            if (chest.getStack(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    private static void playDropStartEffects(ServerWorld world, BlockPos pos) {
        world.playSound(null, pos, SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.BLOCKS, 1.7F, 0.7F);
        world.playSound(null, pos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.2F, 1.45F);
        world.spawnParticles(ParticleTypes.FIREWORK, pos.getX() + 0.5, pos.getY() + 1.6, pos.getZ() + 0.5, 48, 0.75, 1.0, 0.75, 0.08);
        world.spawnParticles(ParticleTypes.GLOW, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 30, 0.65, 0.7, 0.65, 0.05);
        spawnGroundMarker(world, pos);
    }

    private static void playLandingEffects(ServerWorld world, BlockPos pos) {
        world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 0.75F, 1.8F);
        world.playSound(null, pos, SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 0.9F, 1.2F);
        world.playSound(null, pos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.0F, 1.7F);
        world.spawnParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 35, 0.45, 0.15, 0.45, 0.04);
        world.spawnParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 38, 0.45, 0.65, 0.45, 0.025);
        spawnBeaconBeam(world, pos);
    }

    private static void spawnFallingTrail(ServerWorld world, Entity entity, ActiveDrop drop) {
        world.spawnParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.35, entity.getZ(), 8, 0.22, 0.16, 0.22, 0.02);
        world.spawnParticles(ParticleTypes.END_ROD, entity.getX(), entity.getY() + 0.35, entity.getZ(), 5, 0.18, 0.14, 0.18, 0.015);
        world.spawnParticles(ParticleTypes.GLOW, entity.getX(), entity.getY() + 0.35, entity.getZ(), 6, 0.22, 0.18, 0.22, 0.02);
        if (drop.age % 4 == 0) {
            world.spawnParticles(ParticleTypes.FIREWORK, entity.getX(), entity.getY() + 0.4, entity.getZ(), 3, 0.16, 0.16, 0.16, 0.04);
            spawnDescentColumn(world, drop.landingPos, entity.getY());
        }
        if (drop.age % 6 == 0) {
            spawnGroundMarker(world, drop.landingPos);
        }
    }

    private static void spawnGroundMarker(ServerWorld world, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        world.spawnParticles(ParticleTypes.END_ROD, x, pos.getY() + 0.15, z, 14, 0.65, 0.05, 0.65, 0.0);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, x, pos.getY() + 1.0, z, 16, 0.75, 0.65, 0.75, 0.04);
        world.spawnParticles(ParticleTypes.GLOW, x, pos.getY() + 1.0, z, 10, 0.8, 0.45, 0.8, 0.02);
    }

    private static void spawnDescentColumn(ServerWorld world, BlockPos pos, double topY) {
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        int bottomY = pos.getY() + 2;
        int maxY = Math.min((int) Math.ceil(topY), bottomY + BEAM_HEIGHT);
        for (int y = bottomY; y <= maxY; y += 5) {
            world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.04, 0.04, 0.04, 0.0);
            world.spawnParticles(ParticleTypes.GLOW, x, y + 0.5, z, 1, 0.06, 0.06, 0.06, 0.0);
        }
    }

    private static void spawnBeaconBeam(ServerWorld world, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        for (int offset = 1; offset <= BEAM_HEIGHT; offset += 3) {
            double y = pos.getY() + offset;
            world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.035, 0.08, 0.035, 0.0);
            if (offset % 6 == 1) {
                world.spawnParticles(ParticleTypes.GLOW, x, y + 0.5, z, 2, 0.08, 0.12, 0.08, 0.0);
            }
        }
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, x, pos.getY() + 1.0, z, 6, 0.28, 0.35, 0.28, 0.025);
    }

    private static void playBeamStopEffects(ServerWorld world, BlockPos pos, boolean opened) {
        world.spawnParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, opened ? 10 : 16, 0.25, 0.25, 0.25, 0.02);
        world.playSound(null, pos, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 0.55F, opened ? 1.65F : 1.25F);
    }

    private static void removeUnopenedChest(ServerWorld world, ActiveDrop drop) {
        world.spawnParticles(ParticleTypes.CLOUD, drop.chestPos.getX() + 0.5, drop.chestPos.getY() + 0.9, drop.chestPos.getZ() + 0.5, 28, 0.35, 0.3, 0.35, 0.03);
        world.spawnParticles(ParticleTypes.END_ROD, drop.chestPos.getX() + 0.5, drop.chestPos.getY() + 1.1, drop.chestPos.getZ() + 0.5, 16, 0.25, 0.4, 0.25, 0.02);
        world.playSound(null, drop.chestPos, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 0.8F, 0.9F);
        world.removeBlock(drop.chestPos, false);
        restoreBeaconMarker(world, drop);
    }

    private static BeaconMarker createBeaconMarker(ServerWorld world, BlockPos chestPos) {
        BlockPos beaconPos = chestPos.down();
        BlockPos baseCenter = chestPos.down(2);
        Map<BlockPos, BlockState> originals = new LinkedHashMap<>();
        List<BlockPos> positions = new ArrayList<>();
        positions.add(beaconPos);
        for (BlockPos pos : BlockPos.iterate(baseCenter.add(-1, 0, -1), baseCenter.add(1, 0, 1))) {
            positions.add(pos.toImmutable());
        }

        for (BlockPos pos : positions) {
            if (world.getBlockEntity(pos) != null) {
                return null;
            }
            originals.put(pos, world.getBlockState(pos));
        }

        world.setBlockState(beaconPos, Blocks.BEACON.getDefaultState(), Block.NOTIFY_ALL);
        for (BlockPos pos : BlockPos.iterate(baseCenter.add(-1, 0, -1), baseCenter.add(1, 0, 1))) {
            world.setBlockState(pos, Blocks.IRON_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
        }
        return new BeaconMarker(originals);
    }

    private static void restoreBeaconMarker(ServerWorld world, ActiveDrop drop) {
        if (drop.beaconMarker == null) {
            return;
        }

        for (Map.Entry<BlockPos, BlockState> entry : drop.beaconMarker.originalStates().entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState current = world.getBlockState(pos);
            if (current.isOf(Blocks.BEACON) || current.isOf(Blocks.IRON_BLOCK)) {
                world.setBlockState(pos, entry.getValue(), Block.NOTIFY_ALL);
            }
        }
        drop.beaconMarker = null;
    }

    private record DropPoint(BlockPos landingPos, BlockPos spawnPos) {
    }

    private record LootEntry(net.minecraft.item.Item item, int minCount, int maxCount) {
        private ItemStack createStack(Random random) {
            int count = minCount == maxCount ? minCount : minCount + random.nextInt(maxCount - minCount + 1);
            return new ItemStack(item, count);
        }
    }

    private record BeaconMarker(Map<BlockPos, BlockState> originalStates) {
    }

    private static class ActiveDrop {
        private final RegistryKey<World> worldKey;
        private final BlockPos landingPos;
        private final BlockPos spawnPos;
        private final UUID fallingEntityUuid;
        private BeaconMarker beaconMarker;
        private BlockPos chestPos;
        private int age;
        private boolean beamActive;
        private int beamExpireAge;
        private boolean opened;

        private ActiveDrop(RegistryKey<World> worldKey, BlockPos landingPos, BlockPos spawnPos, UUID fallingEntityUuid, BeaconMarker beaconMarker) {
            this.worldKey = worldKey;
            this.landingPos = landingPos;
            this.spawnPos = spawnPos;
            this.fallingEntityUuid = fallingEntityUuid;
            this.beaconMarker = beaconMarker;
        }
    }
}
