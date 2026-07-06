package com.xiaoming.hunterwildcard.respawn;

import com.xiaoming.hunterwildcard.compass.CompassTracker;
import com.xiaoming.hunterwildcard.config.ModConfig;
import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.game.HunterVictoryType;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets;
import com.xiaoming.hunterwildcard.team.PlayerRole;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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

    public DeathOutcome onPlayerDeath(GameContext context, ServerPlayerEntity player, PlayerRole role, ServerPlayerEntity hunterKiller) {
        if (role == null) {
            return DeathOutcome.none();
        }

        ModConfig config = context.getConfig();
        boolean killedByHunter = hunterKiller != null;
        Text deathPrefix = deathPrefix(player, role, hunterKiller, config);
        if (role == PlayerRole.RUNNER && killedByHunter) {
            runnerKillCount++;
            if (config.getHunterVictoryType() == HunterVictoryType.RUNNER_KILL_COUNT) {
                int remainingKills = Math.max(0, config.hunterRunnerKillTarget - runnerKillCount);
                HunterWildcardPackets.sendHunterKillFeedback(
                        context,
                        hunterKiller.getName().getString(),
                        player.getName().getString(),
                        remainingKills,
                        runnerKillCount,
                        config.hunterRunnerKillTarget
                );
            }
            if (config.getHunterVictoryType() == HunterVictoryType.RUNNER_KILL_COUNT
                    && runnerKillCount >= config.hunterRunnerKillTarget) {
                return DeathOutcome.messageAndEnd(
                        HunterWildcardText.translatable("msg.death.final", deathPrefix),
                        HunterWildcardText.spec("msg.win.hunter.kill_target", config.hunterRunnerKillTarget)
                );
            }
            if (config.getHunterVictoryType() == HunterVictoryType.RUNNER_KILL_COUNT) {
                announceKillCountMilestone(context, config.hunterRunnerKillTarget - runnerKillCount);
            }
        }

        RespawnMode mode = modeFor(role, config);
        if (mode == RespawnMode.INFINITE) {
            scheduleRespawn(player, respawnTicksFor(role, config));
            return DeathOutcome.message(HunterWildcardText.translatable("msg.death.respawn_scheduled", deathPrefix, respawnSecondsFor(role, config)));
        }

        int livesAfterDeath = mode == RespawnMode.NO_RESPAWN ? 0 : decrementLife(player, role, config);
        if (livesAfterDeath > 0) {
            scheduleRespawn(player, respawnTicksFor(role, config));
            return DeathOutcome.message(HunterWildcardText.translatable("msg.death.limited_respawn_scheduled", deathPrefix, livesAfterDeath, respawnSecondsFor(role, config)));
        }

        markOut(player);
        Text outMessage = HunterWildcardText.translatable("msg.death.out", deathPrefix);
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
            player.sendMessage(HunterWildcardText.translatable("msg.respawn.out_spectating"), false);
            return;
        }

        Integer timer = respawnTimers.get(player.getUuid());
        if (timer != null && timer > 0) {
            player.changeGameMode(GameMode.SPECTATOR);
            player.sendMessage(HunterWildcardText.translatable("msg.respawn.spectator_wait"), false);
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
                    PlayerRole role = context.getTeamManager().getRole(player);
                    player.sendMessage(HunterWildcardText.translatable(
                            "msg.respawn.waiting_actionbar",
                            remaining / 20 + 1,
                            remainingLivesText(player, role, context.getConfig())
                    ), true);
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
                PlayerRole role = context.getTeamManager().getRole(player);
                player.sendMessage(HunterWildcardText.translatable("msg.respawn.rejoined"), false);
                HunterWildcardPackets.sendHudFeedback(
                        context,
                        HunterWildcardText.spec("hud.feedback.respawn.title"),
                        HunterWildcardText.spec("hud.feedback.respawn.player", player.getName().getString()),
                        HunterWildcardText.spec("hud.feedback.respawn.lives", remainingLivesArg(player, role, context.getConfig())),
                        feedbackStyle(role)
                );
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

    public boolean isWaitingForRespawn(ServerPlayerEntity player) {
        Integer timer = respawnTimers.get(player.getUuid());
        return timer != null && timer > 0 && !outPlayers.contains(player.getUuid());
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
            return HunterWildcardText.spec("msg.win.hunter.runner_out", outRunner.getName().getString());
        }

        for (ServerPlayerEntity runner : context.getRunners()) {
            if (!outPlayers.contains(runner.getUuid())) {
                return null;
            }
        }

        return HunterWildcardText.spec("msg.win.hunter.all_runners_out");
    }

    private Text deathPrefix(ServerPlayerEntity player, PlayerRole role, ServerPlayerEntity hunterKiller, ModConfig config) {
        String playerName = player.getName().getString();
        if (role != PlayerRole.RUNNER) {
            return HunterWildcardText.translatable("msg.death.player", role.getDisplayText(), playerName);
        }

        if (hunterKiller != null) {
            if (config.getHunterVictoryType() == HunterVictoryType.RUNNER_KILL_COUNT) {
                return HunterWildcardText.translatable("msg.death.runner_killed_counted", playerName, hunterKiller.getName().getString());
            }
            return HunterWildcardText.translatable("msg.death.runner_killed", playerName, hunterKiller.getName().getString());
        }

        if (config.getHunterVictoryType() == HunterVictoryType.RUNNER_KILL_COUNT) {
            return HunterWildcardText.translatable("msg.death.runner_died_not_counted", playerName);
        }

        return HunterWildcardText.translatable("msg.death.runner_died", playerName);
    }

    private String feedbackStyle(PlayerRole role) {
        if (role == PlayerRole.HUNTER) {
            return "hunter";
        }
        if (role == PlayerRole.RUNNER) {
            return "respawn";
        }
        return "neutral";
    }

    private void announceKillCountMilestone(GameContext context, int remainingKills) {
        if (remainingKills != 5 && remainingKills != 3 && remainingKills != 2 && remainingKills != 1) {
            return;
        }

        Text message = HunterWildcardText.prefixed(HunterWildcardText.translatable("msg.hunter.kill_target_remaining", remainingKills)).formatted(Formatting.GOLD);
        for (ServerPlayerEntity participant : context.getParticipants()) {
            participant.sendMessage(message);
        }
    }

    private Text remainingLivesText(ServerPlayerEntity player, PlayerRole role, ModConfig config) {
        if (role == null) {
            return HunterWildcardText.translatable("common.unknown");
        }

        RespawnMode mode = modeFor(role, config);
        if (mode == RespawnMode.INFINITE) {
            return HunterWildcardText.translatable("common.infinite");
        }

        if (mode == RespawnMode.NO_RESPAWN) {
            return Text.literal("0");
        }

        return Text.literal(Integer.toString(remainingLives.getOrDefault(player.getUuid(), initialLives(role, config))));
    }

    private String remainingLivesArg(ServerPlayerEntity player, PlayerRole role, ModConfig config) {
        if (role == null) {
            return HunterWildcardText.key("common.unknown");
        }

        RespawnMode mode = modeFor(role, config);
        if (mode == RespawnMode.INFINITE) {
            return HunterWildcardText.key("common.infinite");
        }

        if (mode == RespawnMode.NO_RESPAWN) {
            return "0";
        }

        return Integer.toString(remainingLives.getOrDefault(player.getUuid(), initialLives(role, config)));
    }

    public record DeathOutcome(Text message, String endingReason) {
        public static DeathOutcome none() {
            return new DeathOutcome(null, null);
        }

        public static DeathOutcome message(Text message) {
            return new DeathOutcome(message, null);
        }

        public static DeathOutcome end(String endingReason) {
            return new DeathOutcome(null, endingReason);
        }

        public static DeathOutcome messageAndEnd(Text message, String endingReason) {
            return new DeathOutcome(message, endingReason);
        }
    }
}
