package com.xiaoming.hunterwildcard.util;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public final class PlayerUtil {
    private PlayerUtil() {
    }

    public static ServerPlayerEntity findNearestRunner(ServerPlayerEntity hunter, List<ServerPlayerEntity> runners) {
        ServerPlayerEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (ServerPlayerEntity runner : runners) {
            double distance = distanceSquared(hunter, runner);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = runner;
            }
        }

        return nearest;
    }

    public static double distanceSquared(ServerPlayerEntity a, ServerPlayerEntity b) {
        if (a.getEntityWorld() != b.getEntityWorld()) {
            return Double.MAX_VALUE / 4.0;
        }

        return a.squaredDistanceTo(b);
    }

    public static int roundDistance(double distance) {
        if (Double.isInfinite(distance) || distance > 1_000_000) {
            return -1;
        }

        return Math.max(0, (int) Math.round(distance / 50.0) * 50);
    }
}
