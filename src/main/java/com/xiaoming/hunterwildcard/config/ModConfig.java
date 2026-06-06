package com.xiaoming.hunterwildcard.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.xiaoming.hunterwildcard.HunterWildcardMod;
import com.xiaoming.hunterwildcard.game.HunterVictoryType;
import com.xiaoming.hunterwildcard.game.RunnerVictoryType;
import com.xiaoming.hunterwildcard.respawn.RespawnMode;
import com.xiaoming.hunterwildcard.respawn.RunnerTeamLossMode;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "hunterwildcard.json";

    public int preparingSeconds = 60;
    public int endingSeconds = 5;
    public int compassUpdateSeconds = 5;
    public int hunterRespawnSeconds = 10;
    public int wildcardIntervalSeconds = 240;
    public int wildcardDurationSeconds = 180;
    public int actionBarIntervalSeconds = 1;
    public int hunterRadarIntervalSeconds = 20;
    public int supplyDropIntervalSeconds = 60;
    public boolean hunterPrepareBoundaryEnabled = true;
    public int hunterPrepareBoundaryRadius = 20;
    public int hunterPrepareBoundaryWarnDistance = 3;

    public String runnerVictoryType = "DRAGON";
    public String runnerWinMode = "ANY_ENABLED";
    public boolean enableDragonWin = true;
    public boolean enableSurviveTimeWin = false;
    public int surviveTimeSeconds = 2700;
    public boolean enableReachLocationWin = false;
    public String targetDimension = "minecraft:overworld";
    public int targetX = 0;
    public int targetY = 80;
    public int targetZ = 0;
    public int targetRadius = 10;
    public boolean enableCollectItemWin = false;
    public String targetItemId = "minecraft:diamond";
    public int targetItemCount = 16;

    public String hunterRespawnMode = "INFINITE";
    public int hunterLives = 0;
    public String runnerRespawnMode = "LIMITED_LIVES";
    public int runnerLives = 1;
    public int runnerRespawnSeconds = 10;
    public String runnerTeamLossMode = "ALL_RUNNERS_OUT";
    public String hunterVictoryType = "RUNNERS_OUT";
    public boolean hunterWinByRunnerKillsEnabled = false;
    public int hunterRunnerKillTarget = 10;

    public boolean enableSpeedRush = true;
    public boolean enableFeatherweight = true;
    public boolean enableGlowing = true;
    public boolean enableNightHunt = true;
    public boolean enableExplosiveDeath = true;
    public boolean enableSupplyDrop = true;
    public boolean enableHunterRadar = true;
    public boolean enableCompassChaos = true;

    public static ModConfig load() {
        Path path = getConfigPath();
        if (!Files.exists(path)) {
            ModConfig config = new ModConfig();
            config.save();
            return config;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            ModConfig config = json == null ? new ModConfig() : GSON.fromJson(json, ModConfig.class);
            if (config == null) {
                config = new ModConfig();
            }
            if (json == null || !json.has("runnerVictoryType")) {
                config.runnerVictoryType = inferRunnerVictoryType(config).name();
            }
            if (json == null || !json.has("hunterVictoryType")) {
                config.hunterVictoryType = inferHunterVictoryType(config).name();
            }
            config.validate();
            config.save();
            return config;
        } catch (IOException | RuntimeException exception) {
            HunterWildcardMod.LOGGER.warn("Failed to load hunterwildcard config, using defaults.", exception);
            ModConfig config = new ModConfig();
            config.save();
            return config;
        }
    }

    public boolean save() {
        validate();
        Path path = getConfigPath();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
            return true;
        } catch (IOException exception) {
            HunterWildcardMod.LOGGER.warn("Failed to save hunterwildcard config.", exception);
            return false;
        }
    }

    public void validate() {
        preparingSeconds = clampSeconds(preparingSeconds);
        endingSeconds = clampSeconds(endingSeconds);
        compassUpdateSeconds = clampSeconds(compassUpdateSeconds);
        hunterRespawnSeconds = clampSeconds(hunterRespawnSeconds);
        wildcardIntervalSeconds = clampSeconds(wildcardIntervalSeconds);
        wildcardDurationSeconds = clampSeconds(wildcardDurationSeconds);
        actionBarIntervalSeconds = clampSeconds(actionBarIntervalSeconds);
        hunterRadarIntervalSeconds = clampSeconds(hunterRadarIntervalSeconds);
        supplyDropIntervalSeconds = clampSeconds(supplyDropIntervalSeconds);
        hunterPrepareBoundaryRadius = clampPositive(hunterPrepareBoundaryRadius);
        hunterPrepareBoundaryWarnDistance = Math.max(0, hunterPrepareBoundaryWarnDistance);
        runnerVictoryType = getRunnerVictoryType().name();
        syncLegacyRunnerWinFields();
        runnerWinMode = sanitizeRunnerWinMode(runnerWinMode);
        surviveTimeSeconds = clampSeconds(surviveTimeSeconds);
        targetDimension = sanitizeIdentifier(targetDimension, "minecraft:overworld");
        targetRadius = clampPositive(targetRadius);
        targetItemId = sanitizeIdentifier(targetItemId, "minecraft:diamond");
        targetItemCount = clampPositive(targetItemCount);
        hunterRespawnMode = getHunterRespawnMode().name();
        hunterLives = Math.max(0, hunterLives);
        runnerRespawnMode = getRunnerRespawnMode().name();
        runnerLives = Math.max(0, runnerLives);
        runnerRespawnSeconds = clampSeconds(runnerRespawnSeconds);
        runnerTeamLossMode = getRunnerTeamLossMode().name();
        hunterVictoryType = getHunterVictoryType().name();
        syncLegacyHunterWinFields();
        if (getHunterVictoryType() == HunterVictoryType.RUNNER_KILL_COUNT) {
            runnerRespawnMode = RespawnMode.INFINITE.name();
        }
        hunterRunnerKillTarget = clampPositive(hunterRunnerKillTarget);
    }

    public void copyFrom(ModConfig other) {
        preparingSeconds = other.preparingSeconds;
        endingSeconds = other.endingSeconds;
        compassUpdateSeconds = other.compassUpdateSeconds;
        hunterRespawnSeconds = other.hunterRespawnSeconds;
        wildcardIntervalSeconds = other.wildcardIntervalSeconds;
        wildcardDurationSeconds = other.wildcardDurationSeconds;
        actionBarIntervalSeconds = other.actionBarIntervalSeconds;
        hunterRadarIntervalSeconds = other.hunterRadarIntervalSeconds;
        supplyDropIntervalSeconds = other.supplyDropIntervalSeconds;
        hunterPrepareBoundaryEnabled = other.hunterPrepareBoundaryEnabled;
        hunterPrepareBoundaryRadius = other.hunterPrepareBoundaryRadius;
        hunterPrepareBoundaryWarnDistance = other.hunterPrepareBoundaryWarnDistance;
        runnerVictoryType = other.runnerVictoryType;
        runnerWinMode = other.runnerWinMode;
        enableDragonWin = other.enableDragonWin;
        enableSurviveTimeWin = other.enableSurviveTimeWin;
        surviveTimeSeconds = other.surviveTimeSeconds;
        enableReachLocationWin = other.enableReachLocationWin;
        targetDimension = other.targetDimension;
        targetX = other.targetX;
        targetY = other.targetY;
        targetZ = other.targetZ;
        targetRadius = other.targetRadius;
        enableCollectItemWin = other.enableCollectItemWin;
        targetItemId = other.targetItemId;
        targetItemCount = other.targetItemCount;
        hunterRespawnMode = other.hunterRespawnMode;
        hunterLives = other.hunterLives;
        runnerRespawnMode = other.runnerRespawnMode;
        runnerLives = other.runnerLives;
        runnerRespawnSeconds = other.runnerRespawnSeconds;
        runnerTeamLossMode = other.runnerTeamLossMode;
        hunterVictoryType = other.hunterVictoryType;
        hunterWinByRunnerKillsEnabled = other.hunterWinByRunnerKillsEnabled;
        hunterRunnerKillTarget = other.hunterRunnerKillTarget;
        enableSpeedRush = other.enableSpeedRush;
        enableFeatherweight = other.enableFeatherweight;
        enableGlowing = other.enableGlowing;
        enableNightHunt = other.enableNightHunt;
        enableExplosiveDeath = other.enableExplosiveDeath;
        enableSupplyDrop = other.enableSupplyDrop;
        enableHunterRadar = other.enableHunterRadar;
        enableCompassChaos = other.enableCompassChaos;
        validate();
    }

    public int getPreparingTicks() {
        return secondsToTicks(preparingSeconds);
    }

    public int getEndingTicks() {
        return secondsToTicks(endingSeconds);
    }

    public int getCompassUpdateTicks() {
        return secondsToTicks(compassUpdateSeconds);
    }

    public int getHunterRespawnTicks() {
        return secondsToTicks(hunterRespawnSeconds);
    }

    public int getWildcardIntervalTicks() {
        return secondsToTicks(wildcardIntervalSeconds);
    }

    public int getWildcardDurationTicks() {
        return secondsToTicks(wildcardDurationSeconds);
    }

    public int getActionBarIntervalTicks() {
        return secondsToTicks(actionBarIntervalSeconds);
    }

    public int getHunterRadarIntervalTicks() {
        return secondsToTicks(hunterRadarIntervalSeconds);
    }

    public int getSupplyDropIntervalTicks() {
        return secondsToTicks(supplyDropIntervalSeconds);
    }

    public int getSurviveTimeTicks() {
        return secondsToTicks(surviveTimeSeconds);
    }

    public int getRunnerRespawnTicks() {
        return secondsToTicks(runnerRespawnSeconds);
    }

    public RunnerVictoryType getRunnerVictoryType() {
        return RunnerVictoryType.fromConfig(runnerVictoryType, RunnerVictoryType.DRAGON);
    }

    public RespawnMode getHunterRespawnMode() {
        return RespawnMode.fromConfig(hunterRespawnMode, RespawnMode.INFINITE);
    }

    public RespawnMode getRunnerRespawnMode() {
        return RespawnMode.fromConfig(runnerRespawnMode, RespawnMode.LIMITED_LIVES);
    }

    public RunnerTeamLossMode getRunnerTeamLossMode() {
        return RunnerTeamLossMode.fromConfig(runnerTeamLossMode, RunnerTeamLossMode.ALL_RUNNERS_OUT);
    }

    public HunterVictoryType getHunterVictoryType() {
        return HunterVictoryType.fromConfig(hunterVictoryType, HunterVictoryType.RUNNERS_OUT);
    }

    public boolean isWildcardEnabled(String ruleName) {
        return switch (ruleName) {
            case "疾速追猎", "SpeedRush" -> enableSpeedRush;
            case "轻盈之身", "Featherweight" -> enableFeatherweight;
            case "全员发光", "Glowing" -> enableGlowing;
            case "暗夜追猎", "NightHunt" -> enableNightHunt;
            case "死亡爆炸", "ExplosiveDeath" -> enableExplosiveDeath;
            case "补给空投", "SupplyDrop" -> enableSupplyDrop;
            case "猎人雷达", "HunterRadar" -> enableHunterRadar;
            case "指南针干扰", "CompassChaos" -> enableCompassChaos;
            default -> false;
        };
    }

    public static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    private static int clampSeconds(int value) {
        return Math.max(1, value);
    }

    private static int clampPositive(int value) {
        return Math.max(1, value);
    }

    private static String sanitizeRunnerWinMode(String value) {
        if (value == null || value.isBlank()) {
            return "ANY_ENABLED";
        }

        return "ANY_ENABLED".equalsIgnoreCase(value.trim()) ? "ANY_ENABLED" : "ANY_ENABLED";
    }

    private static RunnerVictoryType inferRunnerVictoryType(ModConfig config) {
        if (config.enableDragonWin) {
            return RunnerVictoryType.DRAGON;
        }
        if (config.enableSurviveTimeWin) {
            return RunnerVictoryType.SURVIVE_TIME;
        }
        if (config.enableReachLocationWin) {
            return RunnerVictoryType.REACH_LOCATION;
        }
        if (config.enableCollectItemWin) {
            return RunnerVictoryType.COLLECT_ITEM;
        }
        return RunnerVictoryType.DRAGON;
    }

    private static HunterVictoryType inferHunterVictoryType(ModConfig config) {
        return config.hunterWinByRunnerKillsEnabled ? HunterVictoryType.RUNNER_KILL_COUNT : HunterVictoryType.RUNNERS_OUT;
    }

    private void syncLegacyRunnerWinFields() {
        RunnerVictoryType victoryType = getRunnerVictoryType();
        enableDragonWin = victoryType == RunnerVictoryType.DRAGON;
        enableSurviveTimeWin = victoryType == RunnerVictoryType.SURVIVE_TIME;
        enableReachLocationWin = victoryType == RunnerVictoryType.REACH_LOCATION;
        enableCollectItemWin = victoryType == RunnerVictoryType.COLLECT_ITEM;
    }

    private void syncLegacyHunterWinFields() {
        hunterWinByRunnerKillsEnabled = getHunterVictoryType() == HunterVictoryType.RUNNER_KILL_COUNT;
    }

    private static String sanitizeIdentifier(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        String trimmed = value.trim();
        return Identifier.tryParse(trimmed) == null ? fallback : trimmed;
    }

    private static int secondsToTicks(int seconds) {
        long ticks = Math.max(1L, seconds) * 20L;
        return ticks > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) ticks;
    }
}
