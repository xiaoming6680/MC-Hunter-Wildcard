package com.xiaoming.hunterwildcard.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xiaoming.hunterwildcard.HunterWildcardMod;
import net.fabricmc.loader.api.FabricLoader;

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
            ModConfig config = GSON.fromJson(reader, ModConfig.class);
            if (config == null) {
                config = new ModConfig();
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

    private static int secondsToTicks(int seconds) {
        long ticks = Math.max(1L, seconds) * 20L;
        return ticks > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) ticks;
    }
}
