package com.xiaoming.hunterwildcard.prepare;

import com.xiaoming.hunterwildcard.game.GameContext;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class HunterBoundaryManager {
    private static final int PARTICLE_INTERVAL_TICKS = 10;
    private static final int BOUNDARY_PARTICLE_POINTS = 32;

    private final Map<UUID, BoundaryPosition> centers = new HashMap<>();
    private final Map<UUID, BoundaryPosition> lastLegalPositions = new HashMap<>();
    private int visualTicks;

    public void start(GameContext context) {
        clear();
        if (!context.getConfig().hunterPrepareBoundaryEnabled) {
            return;
        }

        for (ServerPlayerEntity hunter : context.getHunters()) {
            recordCenter(hunter);
            hunter.sendMessage(Text.literal("猎人准备区域已生成，边界会以粒子提示。"), false);
        }
    }

    public void tick(GameContext context) {
        if (!context.getConfig().hunterPrepareBoundaryEnabled) {
            return;
        }

        double radius = Math.max(1, context.getConfig().hunterPrepareBoundaryRadius);
        double radiusSquared = radius * radius;
        double warnDistance = Math.max(0, context.getConfig().hunterPrepareBoundaryWarnDistance);
        visualTicks--;
        boolean showBoundary = visualTicks <= 0;

        for (ServerPlayerEntity hunter : context.getHunters()) {
            UUID uuid = hunter.getUuid();
            BoundaryPosition center = centers.computeIfAbsent(uuid, ignored -> createPosition(hunter));
            if (showBoundary) {
                spawnBoundaryParticles(hunter, center, radius);
            }

            if (isInside(hunter, center, radiusSquared)) {
                lastLegalPositions.put(uuid, createPosition(hunter));
                maybeWarnNearEdge(hunter, center, radius, warnDistance);
                continue;
            }

            BoundaryPosition target = lastLegalPositions.getOrDefault(uuid, center);
            teleport(hunter, context, target);
            hunter.sendMessage(Text.literal("你不能离开猎人准备区域"), true);
        }

        if (showBoundary) {
            visualTicks = PARTICLE_INTERVAL_TICKS;
        }
    }

    public void remove(ServerPlayerEntity player) {
        centers.remove(player.getUuid());
        lastLegalPositions.remove(player.getUuid());
    }

    public void clear() {
        centers.clear();
        lastLegalPositions.clear();
        visualTicks = 0;
    }

    private void recordCenter(ServerPlayerEntity hunter) {
        BoundaryPosition position = createPosition(hunter);
        centers.put(hunter.getUuid(), position);
        lastLegalPositions.put(hunter.getUuid(), position);
    }

    private boolean isInside(ServerPlayerEntity hunter, BoundaryPosition center, double radiusSquared) {
        if (!hunter.getEntityWorld().getRegistryKey().equals(center.worldKey)) {
            return false;
        }

        double dx = hunter.getX() - center.x;
        double dz = hunter.getZ() - center.z;
        return dx * dx + dz * dz <= radiusSquared;
    }

    private void maybeWarnNearEdge(ServerPlayerEntity hunter, BoundaryPosition center, double radius, double warnDistance) {
        if (warnDistance <= 0) {
            return;
        }

        double dx = hunter.getX() - center.x;
        double dz = hunter.getZ() - center.z;
        double remaining = radius - Math.sqrt(dx * dx + dz * dz);
        if (remaining <= warnDistance) {
            hunter.sendMessage(Text.literal("准备区域边界剩余 " + Math.max(0, (int) Math.floor(remaining)) + " 格"), true);
        }
    }

    private void spawnBoundaryParticles(ServerPlayerEntity hunter, BoundaryPosition center, double radius) {
        if (!hunter.getEntityWorld().getRegistryKey().equals(center.worldKey)) {
            return;
        }

        ServerWorld world = hunter.getEntityWorld();
        double y = hunter.getY() + 0.15;
        int points = radius <= 12.0 ? 20 : BOUNDARY_PARTICLE_POINTS;
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0 * i) / points;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.0, 0.02, 0.0, 0.0);
        }
    }

    private void teleport(ServerPlayerEntity hunter, GameContext context, BoundaryPosition target) {
        ServerWorld world = context.getServer().getWorld(target.worldKey);
        if (world == null) {
            world = hunter.getEntityWorld();
        }

        hunter.teleport(world, target.x, target.y, target.z, Set.<PositionFlag>of(), target.yaw, target.pitch, true);
        lastLegalPositions.put(hunter.getUuid(), new BoundaryPosition(world.getRegistryKey(), target.x, target.y, target.z, target.yaw, target.pitch));
    }

    private BoundaryPosition createPosition(ServerPlayerEntity player) {
        return new BoundaryPosition(
                player.getEntityWorld().getRegistryKey(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYaw(),
                player.getPitch()
        );
    }

    private record BoundaryPosition(RegistryKey<World> worldKey, double x, double y, double z, float yaw, float pitch) {
    }
}
