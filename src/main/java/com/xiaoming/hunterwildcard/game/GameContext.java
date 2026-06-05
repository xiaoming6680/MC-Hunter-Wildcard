package com.xiaoming.hunterwildcard.game;

import com.xiaoming.hunterwildcard.team.TeamManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.Random;

public class GameContext {
    private final MinecraftServer server;
    private final GameConfig config;
    private final TeamManager teamManager;
    private final Random random;

    public GameContext(MinecraftServer server, GameConfig config, TeamManager teamManager, Random random) {
        this.server = server;
        this.config = config;
        this.teamManager = teamManager;
        this.random = random;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public GameConfig getConfig() {
        return config;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public Random getRandom() {
        return random;
    }

    public List<ServerPlayerEntity> getParticipants() {
        return teamManager.getParticipants(server);
    }

    public List<ServerPlayerEntity> getHunters() {
        return teamManager.getHunters(server);
    }

    public List<ServerPlayerEntity> getRunners() {
        return teamManager.getRunners(server);
    }
}
