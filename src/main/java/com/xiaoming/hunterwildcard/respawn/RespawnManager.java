package com.xiaoming.hunterwildcard.respawn;

import com.xiaoming.hunterwildcard.compass.CompassTracker;
import com.xiaoming.hunterwildcard.game.GameContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class RespawnManager {
    private final Map<UUID, Integer> respawnTimers = new HashMap<>();

    public void onHunterDeath(ServerPlayerEntity hunter, int respawnTicks) {
        respawnTimers.put(hunter.getUuid(), respawnTicks);
    }

    public void onAfterHunterRespawn(ServerPlayerEntity hunter) {
        Integer remaining = respawnTimers.get(hunter.getUuid());
        if (remaining != null && remaining > 0) {
            hunter.changeGameMode(GameMode.SPECTATOR);
        }
    }

    public void tick(GameContext context, CompassTracker compassTracker) {
        Iterator<Map.Entry<UUID, Integer>> iterator = respawnTimers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            int remaining = entry.getValue() - 1;

            ServerPlayerEntity player = context.getServer().getPlayerManager().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }

            if (remaining > 0) {
                entry.setValue(remaining);
                if (!player.isDead()) {
                    player.sendMessage(net.minecraft.text.Text.literal("猎人复活倒计时: " + (remaining / 20 + 1) + " 秒"), true);
                }
                continue;
            }

            iterator.remove();
            if (!player.isDead()) {
                player.changeGameMode(GameMode.SURVIVAL);
                player.setHealth(player.getMaxHealth());
                compassTracker.giveCompass(player);
                player.sendMessage(net.minecraft.text.Text.literal("你已重新加入追杀。"), false);
            }
        }
    }

    public void remove(ServerPlayerEntity player) {
        respawnTimers.remove(player.getUuid());
    }

    public void clear() {
        respawnTimers.clear();
    }

    public void clear(GameContext context) {
        for (UUID uuid : respawnTimers.keySet()) {
            ServerPlayerEntity player = context.getServer().getPlayerManager().getPlayer(uuid);
            if (player != null && !player.isDead()) {
                player.changeGameMode(GameMode.SURVIVAL);
            }
        }
        respawnTimers.clear();
    }
}
