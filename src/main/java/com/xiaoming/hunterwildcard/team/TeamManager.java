package com.xiaoming.hunterwildcard.team;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TeamManager {
    private final Map<UUID, PlayerRole> playerRolesById = new HashMap<>();

    public void join(ServerPlayerEntity player, PlayerRole role) {
        playerRolesById.put(player.getUuid(), role);
    }

    public PlayerRole leave(ServerPlayerEntity player) {
        return playerRolesById.remove(player.getUuid());
    }

    public void remove(UUID playerId) {
        playerRolesById.remove(playerId);
    }

    public void clear() {
        playerRolesById.clear();
    }

    public PlayerRole getRole(ServerPlayerEntity player) {
        return playerRolesById.get(player.getUuid());
    }

    public boolean isHunter(ServerPlayerEntity player) {
        return getRole(player) == PlayerRole.HUNTER;
    }

    public boolean isRunner(ServerPlayerEntity player) {
        return getRole(player) == PlayerRole.RUNNER;
    }

    public int count(PlayerRole role) {
        int count = 0;
        for (PlayerRole assignedRole : playerRolesById.values()) {
            if (assignedRole == role) {
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
        List<ServerPlayerEntity> onlinePlayers = new ArrayList<>();
        for (UUID playerId : playerRolesById.keySet()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
            if (player != null) {
                onlinePlayers.add(player);
            }
        }
        return onlinePlayers;
    }

    private List<ServerPlayerEntity> getPlayers(MinecraftServer server, PlayerRole role) {
        List<ServerPlayerEntity> onlinePlayers = new ArrayList<>();
        for (Map.Entry<UUID, PlayerRole> entry : playerRolesById.entrySet()) {
            if (entry.getValue() != role) {
                continue;
            }

            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player != null) {
                onlinePlayers.add(player);
            }
        }
        return onlinePlayers;
    }
}
