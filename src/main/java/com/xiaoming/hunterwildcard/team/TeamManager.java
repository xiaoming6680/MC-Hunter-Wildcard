package com.xiaoming.hunterwildcard.team;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TeamManager {
    private final Map<UUID, PlayerRole> roles = new HashMap<>();

    public void join(ServerPlayerEntity player, PlayerRole role) {
        roles.put(player.getUuid(), role);
    }

    public PlayerRole leave(ServerPlayerEntity player) {
        return roles.remove(player.getUuid());
    }

    public void remove(UUID uuid) {
        roles.remove(uuid);
    }

    public void clear() {
        roles.clear();
    }

    public PlayerRole getRole(ServerPlayerEntity player) {
        return roles.get(player.getUuid());
    }

    public boolean isHunter(ServerPlayerEntity player) {
        return getRole(player) == PlayerRole.HUNTER;
    }

    public boolean isRunner(ServerPlayerEntity player) {
        return getRole(player) == PlayerRole.RUNNER;
    }

    public int count(PlayerRole role) {
        int count = 0;
        for (PlayerRole playerRole : roles.values()) {
            if (playerRole == role) {
                count++;
            }
        }
        return count;
    }

    public List<ServerPlayerEntity> getHunters(MinecraftServer server) {
        return getPlayers(server, PlayerRole.HUNTER);
    }

    public List<ServerPlayerEntity> getRunners(MinecraftServer server) {
        return getPlayers(server, PlayerRole.RUNNER);
    }

    public List<ServerPlayerEntity> getParticipants(MinecraftServer server) {
        List<ServerPlayerEntity> players = new ArrayList<>();
        for (UUID uuid : roles.keySet()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player != null) {
                players.add(player);
            }
        }
        return players;
    }

    private List<ServerPlayerEntity> getPlayers(MinecraftServer server, PlayerRole role) {
        List<ServerPlayerEntity> players = new ArrayList<>();
        for (Map.Entry<UUID, PlayerRole> entry : roles.entrySet()) {
            if (entry.getValue() != role) {
                continue;
            }

            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player != null) {
                players.add(player);
            }
        }
        return players;
    }
}
