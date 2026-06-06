package com.xiaoming.hunterwildcard.respawn;

import com.xiaoming.hunterwildcard.compass.CompassTracker;
import com.xiaoming.hunterwildcard.config.ModConfig;
import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.game.HunterVictoryType;
import com.xiaoming.hunterwildcard.team.PlayerRole;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class RespawnManager {
    private final Map<UUID, Integer> respawnTimers = new HashMap<>();
    private final Map<UUID, Integer> remainingLives = new HashMap<>();
    private final Set<UUID> outPlayers = new HashSet<>();
    private int runnerKillCount;

    public void start(GameContext context) {
        clear();
        for (ServerPlayerEntity hunter : context.getHunters()) {
            remainingLives.put(hunter.getUuid(), initialLives(PlayerRole.HUNTER, context.getConfig()));
        }
        for (ServerPlayerEntity runner : context.getRunners()) {
            remainingLives.put(runner.getUuid(), initialLives(PlayerRole.RUNNER, context.getConfig()));
        }
    }

    public DeathOutcome onPlayerDeath(GameContext context, ServerPlayerEntity player, PlayerRole role, boolean killedByHunter) {
        if (role == null) {
            return DeathOutcome.none();
        }

        ModConfig config = context.getConfig();
        if (role == PlayerRole.RUNNER && killedByHunter) {
            runnerKillCount++;
            if (config.getHunterVictoryType() == HunterVictoryType.RUNNER_KILL_COUNT
                    && runnerKillCount >= config.hunterRunnerKillTarget) {
                return DeathOutcome.end("猎人累计击杀逃亡者达到 " + config.hunterRunnerKillTarget + " 次，猎人阵营获胜。");
            }
        }

        RespawnMode mode = modeFor(role, config);
        if (mode == RespawnMode.INFINITE) {
            scheduleRespawn(player, respawnTicksFor(role, config));
            return DeathOutcome.message(role.getDisplayName() + " " + player.getName().getString()
                    + " 死亡，将在 " + respawnSecondsFor(role, config) + " 秒后复活。");
        }

        int livesAfterDeath = mode == RespawnMode.NO_RESPAWN ? 0 : decrementLife(player, role, config);
        if (livesAfterDeath > 0) {
            scheduleRespawn(player, respawnTicksFor(role, config));
            return DeathOutcome.message(role.getDisplayName() + " " + player.getName().getString()
                    + " 死亡，剩余生命 " + livesAfterDeath
                    + "，将在 " + respawnSecondsFor(role, config) + " 秒后复活。");
        }

        markOut(player);
        String outMessage = role.getDisplayName() + " " + player.getName().getString() + " 已出局。";
        if (role == PlayerRole.RUNNER) {
            String endingReason = runnerLossReason(context, player);
            if (endingReason != null) {
                return DeathOutcome.messageAndEnd(outMessage, endingReason);
            }
        }

        return DeathOutcome.message(outMessage);
    }

    public void onAfterRespawn(GameContext context, ServerPlayerEntity player, CompassTracker compassTracker) {
        if (outPlayers.contains(player.getUuid())) {
            player.changeGameMode(GameMode.SPECTATOR);
            player.sendMessage(Text.literal("你已出局，正在旁观本局游戏。"), false);
            return;
        }

        Integer timer = respawnTimers.get(player.getUuid());
        if (timer != null && timer > 0) {
            player.changeGameMode(GameMode.SPECTATOR);
            return;
        }

        if (context.getTeamManager().isHunter(player)) {
            compassTracker.giveCompass(player);
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

            if (outPlayers.contains(player.getUuid())) {
                iterator.remove();
                continue;
            }

            if (remaining > 0) {
                entry.setValue(remaining);
                if (!player.isDead()) {
                    player.sendMessage(Text.literal("复活倒计时: " + (remaining / 20 + 1) + " 秒"), true);
                }
                continue;
            }

            iterator.remove();
            if (!player.isDead()) {
                player.changeGameMode(GameMode.SURVIVAL);
                player.setHealth(player.getMaxHealth());
                if (context.getTeamManager().isHunter(player)) {
                    compassTracker.giveCompass(player);
                }
                player.sendMessage(Text.literal("你已重新加入游戏。"), false);
            }
        }
    }

    public void remove(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        respawnTimers.remove(uuid);
        remainingLives.remove(uuid);
        outPlayers.remove(uuid);
    }

    public boolean isOut(ServerPlayerEntity player) {
        return outPlayers.contains(player.getUuid());
    }

    public int getRunnerKillCount() {
        return runnerKillCount;
    }

    public void clear() {
        respawnTimers.clear();
        remainingLives.clear();
        outPlayers.clear();
        runnerKillCount = 0;
    }

    public void clear(GameContext context) {
        Set<UUID> affectedPlayers = new HashSet<>();
        affectedPlayers.addAll(respawnTimers.keySet());
        affectedPlayers.addAll(outPlayers);

        for (UUID uuid : affectedPlayers) {
            ServerPlayerEntity player = context.getServer().getPlayerManager().getPlayer(uuid);
            if (player != null && !player.isDead()) {
                player.changeGameMode(GameMode.SURVIVAL);
            }
        }

        clear();
    }

    private void scheduleRespawn(ServerPlayerEntity player, int respawnTicks) {
        respawnTimers.put(player.getUuid(), Math.max(1, respawnTicks));
    }

    private void markOut(ServerPlayerEntity player) {
        respawnTimers.remove(player.getUuid());
        remainingLives.put(player.getUuid(), 0);
        outPlayers.add(player.getUuid());
    }

    private int decrementLife(ServerPlayerEntity player, PlayerRole role, ModConfig config) {
        UUID uuid = player.getUuid();
        int current = remainingLives.computeIfAbsent(uuid, ignored -> initialLives(role, config));
        int remaining = Math.max(0, current - 1);
        remainingLives.put(uuid, remaining);
        return remaining;
    }

    private int initialLives(PlayerRole role, ModConfig config) {
        RespawnMode mode = modeFor(role, config);
        if (mode == RespawnMode.INFINITE) {
            return Integer.MAX_VALUE;
        }
        if (mode == RespawnMode.NO_RESPAWN) {
            return 1;
        }

        int configured = role == PlayerRole.HUNTER ? config.hunterLives : config.runnerLives;
        return Math.max(1, configured);
    }

    private RespawnMode modeFor(PlayerRole role, ModConfig config) {
        return role == PlayerRole.HUNTER ? config.getHunterRespawnMode() : config.getRunnerRespawnMode();
    }

    private int respawnTicksFor(PlayerRole role, ModConfig config) {
        return role == PlayerRole.HUNTER ? config.getHunterRespawnTicks() : config.getRunnerRespawnTicks();
    }

    private int respawnSecondsFor(PlayerRole role, ModConfig config) {
        return role == PlayerRole.HUNTER ? config.hunterRespawnSeconds : config.runnerRespawnSeconds;
    }

    private String runnerLossReason(GameContext context, ServerPlayerEntity outRunner) {
        RunnerTeamLossMode lossMode = context.getConfig().getRunnerTeamLossMode();
        if (lossMode == RunnerTeamLossMode.ANY_RUNNER_OUT) {
            return "逃亡者 " + outRunner.getName().getString() + " 出局，猎人阵营获胜。";
        }

        for (ServerPlayerEntity runner : context.getRunners()) {
            if (!outPlayers.contains(runner.getUuid())) {
                return null;
            }
        }

        return "所有逃亡者已出局，猎人阵营获胜。";
    }

    public record DeathOutcome(String message, String endingReason) {
        public static DeathOutcome none() {
            return new DeathOutcome(null, null);
        }

        public static DeathOutcome message(String message) {
            return new DeathOutcome(message, null);
        }

        public static DeathOutcome end(String endingReason) {
            return new DeathOutcome(null, endingReason);
        }

        public static DeathOutcome messageAndEnd(String message, String endingReason) {
            return new DeathOutcome(message, endingReason);
        }
    }
}
