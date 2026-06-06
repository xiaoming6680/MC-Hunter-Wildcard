package com.xiaoming.hunterwildcard.network;

import com.xiaoming.hunterwildcard.HunterWildcardMod;
import com.xiaoming.hunterwildcard.command.HunterWildcardCommand;
import com.xiaoming.hunterwildcard.config.ModConfig;
import com.xiaoming.hunterwildcard.game.GameManager;
import com.xiaoming.hunterwildcard.game.GameState;
import com.xiaoming.hunterwildcard.team.PlayerRole;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class HunterWildcardPackets {
    public static final CustomPayload.Id<RequestConfigPayload> C2S_REQUEST_CONFIG =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "request_config"));
    public static final CustomPayload.Id<SyncConfigPayload> S2C_SYNC_CONFIG =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "sync_config"));
    public static final CustomPayload.Id<OperationResultPayload> S2C_OPERATION_RESULT =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "operation_result"));
    public static final CustomPayload.Id<UpdateConfigPayload> C2S_UPDATE_CONFIG =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "update_config"));
    public static final CustomPayload.Id<ReloadConfigPayload> C2S_RELOAD_CONFIG =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "reload_config"));
    public static final CustomPayload.Id<DebugActionPayload> C2S_DEBUG_ACTION =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "debug_action"));
    public static final CustomPayload.Id<TestWildcardPayload> C2S_TEST_WILDCARD =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "test_wildcard"));
    public static final CustomPayload.Id<TeamActionPayload> C2S_TEAM_ACTION =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "team_action"));
    public static final CustomPayload.Id<GameActionPayload> C2S_GAME_ACTION =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "game_action"));

    private static boolean payloadTypesRegistered;
    private static boolean serverReceiversRegistered;

    private HunterWildcardPackets() {
    }

    public static void registerPayloadTypes() {
        if (payloadTypesRegistered) {
            return;
        }
        payloadTypesRegistered = true;

        PayloadTypeRegistry.playC2S().register(C2S_REQUEST_CONFIG, RequestConfigPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(S2C_SYNC_CONFIG, SyncConfigPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(S2C_OPERATION_RESULT, OperationResultPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(C2S_UPDATE_CONFIG, UpdateConfigPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(C2S_RELOAD_CONFIG, ReloadConfigPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(C2S_DEBUG_ACTION, DebugActionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(C2S_TEST_WILDCARD, TestWildcardPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(C2S_TEAM_ACTION, TeamActionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(C2S_GAME_ACTION, GameActionPayload.CODEC);
    }

    public static void registerServerReceivers() {
        if (serverReceiversRegistered) {
            return;
        }
        serverReceiversRegistered = true;

        ServerPlayNetworking.registerGlobalReceiver(C2S_REQUEST_CONFIG, (payload, context) -> sendSync(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(C2S_UPDATE_CONFIG, (payload, context) -> handleUpdateConfig(context.player(), payload.config()));
        ServerPlayNetworking.registerGlobalReceiver(C2S_RELOAD_CONFIG, (payload, context) -> handleReloadConfig(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(C2S_DEBUG_ACTION, (payload, context) -> handleDebugAction(context.player(), payload.action()));
        ServerPlayNetworking.registerGlobalReceiver(C2S_TEST_WILDCARD, (payload, context) -> handleTestWildcard(context.player(), payload.wildcardName()));
        ServerPlayNetworking.registerGlobalReceiver(C2S_TEAM_ACTION, (payload, context) -> handleTeamAction(context.player(), payload.action()));
        ServerPlayNetworking.registerGlobalReceiver(C2S_GAME_ACTION, (payload, context) -> handleGameAction(context.player(), payload.action()));
    }

    public static void sendSync(ServerPlayerEntity player) {
        if (ServerPlayNetworking.canSend(player, S2C_SYNC_CONFIG)) {
            ServerPlayNetworking.send(player, createSyncPayload(player));
        }
    }

    public static void syncAll(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            sendSync(player);
        }
    }

    private static void sendOperationResult(ServerPlayerEntity player, boolean success, String message) {
        if (ServerPlayNetworking.canSend(player, S2C_OPERATION_RESULT)) {
            ServerPlayNetworking.send(player, new OperationResultPayload(success, message));
        }
    }

    private static void sendSyncAndResult(ServerPlayerEntity player, boolean success, String message) {
        sendSync(player);
        sendOperationResult(player, success, message);
    }

    private static void syncAllAndResult(ServerPlayerEntity player, boolean success, String message) {
        syncAll(player.getEntityWorld().getServer());
        sendOperationResult(player, success, message);
    }

    private static SyncConfigPayload createSyncPayload(ServerPlayerEntity player) {
        GameManager manager = GameManager.getInstance();
        String activeWildcard = manager.getWildcardManager().getActiveRuleName();
        if (activeWildcard == null) {
            activeWildcard = "无";
        }

        PlayerRole playerRole = manager.getTeamManager().getRole(player);
        boolean canManage = HunterWildcardCommand.canManageGame(player.getCommandSource());

        return new SyncConfigPayload(
                manager.getState(),
                manager.getTeamManager().count(PlayerRole.HUNTER),
                manager.getTeamManager().count(PlayerRole.RUNNER),
                activeWildcard,
                playerRole == null ? "未加入" : playerRole.getDisplayName(),
                playerRole != null,
                manager.getWildcardManager().getActiveRule() != null,
                ticksToSeconds(manager.getPhaseRemainingTicks()),
                ticksToSeconds(manager.getActiveWildcardRemainingTicks()),
                ticksToSeconds(manager.getTicksUntilNextWildcard()),
                canManage,
                canManage && manager.isDebugMenuEnabled(player),
                ConfigSnapshot.from(manager.getConfig())
        );
    }

    private static void handleUpdateConfig(ServerPlayerEntity player, ConfigSnapshot snapshot) {
        if (!HunterWildcardCommand.canManageGame(player.getCommandSource())) {
            reject(player);
            return;
        }

        GameManager manager = GameManager.getInstance();
        manager.applyConfig(snapshot.toConfig());
        if (manager.saveConfig()) {
            String message = "配置已保存。";
            manager.getMessageManager().direct(player, message);
            syncAllAndResult(player, true, message);
        } else {
            String message = "保存配置失败，请检查服务器日志。";
            player.sendMessage(Text.literal(message), false);
            syncAllAndResult(player, false, message);
        }
    }

    private static void handleReloadConfig(ServerPlayerEntity player) {
        if (!HunterWildcardCommand.canManageGame(player.getCommandSource())) {
            reject(player);
            return;
        }

        GameManager manager = GameManager.getInstance();
        manager.reloadConfig();
        String message = "配置已重新加载。";
        manager.getMessageManager().direct(player, message);
        syncAllAndResult(player, true, message);
    }

    private static void handleDebugAction(ServerPlayerEntity player, DebugAction action) {
        if (!HunterWildcardCommand.canManageGame(player.getCommandSource())) {
            reject(player);
            return;
        }

        GameManager manager = GameManager.getInstance();
        if (!manager.isDebugMenuEnabled(player)) {
            fail(player, "请先使用 /hw ts true 打开调试页。");
            return;
        }

        switch (action) {
            case START_GAME -> {
                String error = validateStartGame(manager);
                if (error != null) {
                    fail(player, error);
                    return;
                }
                manager.start(player.getCommandSource());
                syncAllAndResult(player, true, "游戏已开始准备。");
            }
            case STOP_GAME -> {
                boolean hadGame = canStopGame(manager);
                manager.stop(player.getCommandSource());
                syncAllAndResult(player, hadGame, hadGame ? "游戏已停止。" : "当前没有正在进行的猎人外卡游戏。");
            }
            case ROLL_WILDCARD -> {
                if (manager.getState() != GameState.RUNNING) {
                    fail(player, "只有 RUNNING 阶段可以手动触发外卡。");
                    return;
                }
                manager.rollWildcard(player.getCommandSource());
                String activeRule = manager.getWildcardManager().getActiveRuleName();
                syncAllAndResult(player, activeRule != null, activeRule == null ? "没有可用外卡。" : "已随机触发外卡: " + activeRule);
            }
            case STOP_WILDCARD -> {
                boolean hadWildcard = manager.getWildcardManager().getActiveRule() != null;
                manager.debugStopWildcard(player.getCommandSource());
                syncAllAndResult(player, hadWildcard, hadWildcard ? "已停止当前外卡。" : "当前没有正在运行的外卡。");
            }
        }
    }

    private static void handleTestWildcard(ServerPlayerEntity player, String wildcardName) {
        if (!HunterWildcardCommand.canManageGame(player.getCommandSource())) {
            reject(player);
            return;
        }

        GameManager manager = GameManager.getInstance();
        if (!manager.isDebugMenuEnabled(player)) {
            fail(player, "请先使用 /hw ts true 打开调试页。");
            return;
        }

        manager.testWildcard(player.getCommandSource(), wildcardName, player);
        String activeRule = manager.getWildcardManager().getActiveRuleName();
        boolean success = activeRule != null && activeRule.equals(wildcardName);
        syncAllAndResult(player, success, success ? "已测试触发外卡: " + wildcardName : "该外卡不可用或已关闭: " + wildcardName);
    }

    private static void handleTeamAction(ServerPlayerEntity player, TeamAction action) {
        GameManager manager = GameManager.getInstance();
        PlayerRole previousRole = manager.getTeamManager().getRole(player);
        boolean canJoin = manager.getState() != GameState.RUNNING && manager.getState() != GameState.ENDING;
        switch (action) {
            case JOIN_HUNTER -> manager.join(player, PlayerRole.HUNTER);
            case JOIN_RUNNER -> manager.join(player, PlayerRole.RUNNER);
            case LEAVE -> manager.leave(player);
        }
        switch (action) {
            case JOIN_HUNTER -> syncAllAndResult(player, canJoin, canJoin ? "已加入猎人队伍。" : "游戏已经开始，当前不能加入队伍。");
            case JOIN_RUNNER -> syncAllAndResult(player, canJoin, canJoin ? "已加入逃亡者队伍。" : "游戏已经开始，当前不能加入队伍。");
            case LEAVE -> syncAllAndResult(player, previousRole != null, previousRole == null ? "你当前不在游戏队伍中。" : "已离开 " + previousRole.getDisplayName() + " 队伍。");
        }
    }

    private static void handleGameAction(ServerPlayerEntity player, GameAction action) {
        if (!HunterWildcardCommand.canManageGame(player.getCommandSource())) {
            reject(player);
            return;
        }

        GameManager manager = GameManager.getInstance();
        switch (action) {
            case START_GAME -> {
                String error = validateStartGame(manager);
                if (error != null) {
                    fail(player, error);
                    return;
                }
                manager.start(player.getCommandSource());
                syncAllAndResult(player, true, "游戏已开始准备。");
            }
            case STOP_GAME -> {
                boolean hadGame = canStopGame(manager);
                manager.stop(player.getCommandSource());
                syncAllAndResult(player, hadGame, hadGame ? "游戏已停止。" : "当前没有正在进行的猎人外卡游戏。");
            }
            case ROLL_WILDCARD -> {
                if (manager.getState() != GameState.RUNNING) {
                    fail(player, "只有 RUNNING 阶段可以手动触发外卡。");
                    return;
                }
                manager.rollWildcard(player.getCommandSource());
                String activeRule = manager.getWildcardManager().getActiveRuleName();
                syncAllAndResult(player, activeRule != null, activeRule == null ? "没有可用外卡。" : "已随机触发外卡: " + activeRule);
            }
        }
    }

    private static void reject(ServerPlayerEntity player) {
        fail(player, "你没有权限执行该操作。");
    }

    private static void fail(ServerPlayerEntity player, String message) {
        GameManager.getInstance().getMessageManager().direct(player, message);
        sendSyncAndResult(player, false, message);
    }

    private static String validateStartGame(GameManager manager) {
        if (manager.getState() != GameState.WAITING) {
            return "游戏已经开始或正在结束。";
        }

        if (manager.getTeamManager().count(PlayerRole.HUNTER) == 0 || manager.getTeamManager().count(PlayerRole.RUNNER) == 0) {
            return "至少需要 1 名猎人和 1 名逃亡者。";
        }

        return null;
    }

    private static boolean canStopGame(GameManager manager) {
        return manager.getState() != GameState.WAITING
                || manager.getTeamManager().count(PlayerRole.HUNTER) > 0
                || manager.getTeamManager().count(PlayerRole.RUNNER) > 0;
    }

    private static int ticksToSeconds(int ticks) {
        return ticks < 0 ? -1 : Math.max(0, (ticks + 19) / 20);
    }

    public enum DebugAction {
        START_GAME,
        STOP_GAME,
        ROLL_WILDCARD,
        STOP_WILDCARD
    }

    public enum TeamAction {
        JOIN_HUNTER,
        JOIN_RUNNER,
        LEAVE
    }

    public enum GameAction {
        START_GAME,
        STOP_GAME,
        ROLL_WILDCARD
    }

    public record ConfigSnapshot(
            int preparingSeconds,
            int endingSeconds,
            int compassUpdateSeconds,
            int hunterRespawnSeconds,
            int wildcardIntervalSeconds,
            int wildcardDurationSeconds,
            int actionBarIntervalSeconds,
            int hunterRadarIntervalSeconds,
            int supplyDropIntervalSeconds,
            boolean hunterPrepareBoundaryEnabled,
            int hunterPrepareBoundaryRadius,
            int hunterPrepareBoundaryWarnDistance,
            String runnerVictoryType,
            String runnerWinMode,
            boolean enableDragonWin,
            boolean enableSurviveTimeWin,
            int surviveTimeSeconds,
            boolean enableReachLocationWin,
            String targetDimension,
            int targetX,
            int targetY,
            int targetZ,
            int targetRadius,
            boolean enableCollectItemWin,
            String targetItemId,
            int targetItemCount,
            String hunterRespawnMode,
            int hunterLives,
            String runnerRespawnMode,
            int runnerLives,
            int runnerRespawnSeconds,
            String runnerTeamLossMode,
            String hunterVictoryType,
            boolean hunterWinByRunnerKillsEnabled,
            int hunterRunnerKillTarget,
            boolean enableSpeedRush,
            boolean enableFeatherweight,
            boolean enableGlowing,
            boolean enableNightHunt,
            boolean enableExplosiveDeath,
            boolean enableSupplyDrop,
            boolean enableHunterRadar,
            boolean enableCompassChaos
    ) {
        private static ConfigSnapshot fromBuf(RegistryByteBuf buf) {
            return new ConfigSnapshot(
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readBoolean(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readString(32),
                    buf.readString(32),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readInt(),
                    buf.readBoolean(),
                    buf.readString(128),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readBoolean(),
                    buf.readString(128),
                    buf.readInt(),
                    buf.readString(32),
                    buf.readInt(),
                    buf.readString(32),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readString(32),
                    buf.readString(32),
                    buf.readBoolean(),
                    buf.readInt(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean()
            );
        }

        private void write(RegistryByteBuf buf) {
            buf.writeInt(preparingSeconds);
            buf.writeInt(endingSeconds);
            buf.writeInt(compassUpdateSeconds);
            buf.writeInt(hunterRespawnSeconds);
            buf.writeInt(wildcardIntervalSeconds);
            buf.writeInt(wildcardDurationSeconds);
            buf.writeInt(actionBarIntervalSeconds);
            buf.writeInt(hunterRadarIntervalSeconds);
            buf.writeInt(supplyDropIntervalSeconds);
            buf.writeBoolean(hunterPrepareBoundaryEnabled);
            buf.writeInt(hunterPrepareBoundaryRadius);
            buf.writeInt(hunterPrepareBoundaryWarnDistance);
            buf.writeString(runnerVictoryType);
            buf.writeString(runnerWinMode);
            buf.writeBoolean(enableDragonWin);
            buf.writeBoolean(enableSurviveTimeWin);
            buf.writeInt(surviveTimeSeconds);
            buf.writeBoolean(enableReachLocationWin);
            buf.writeString(targetDimension);
            buf.writeInt(targetX);
            buf.writeInt(targetY);
            buf.writeInt(targetZ);
            buf.writeInt(targetRadius);
            buf.writeBoolean(enableCollectItemWin);
            buf.writeString(targetItemId);
            buf.writeInt(targetItemCount);
            buf.writeString(hunterRespawnMode);
            buf.writeInt(hunterLives);
            buf.writeString(runnerRespawnMode);
            buf.writeInt(runnerLives);
            buf.writeInt(runnerRespawnSeconds);
            buf.writeString(runnerTeamLossMode);
            buf.writeString(hunterVictoryType);
            buf.writeBoolean(hunterWinByRunnerKillsEnabled);
            buf.writeInt(hunterRunnerKillTarget);
            buf.writeBoolean(enableSpeedRush);
            buf.writeBoolean(enableFeatherweight);
            buf.writeBoolean(enableGlowing);
            buf.writeBoolean(enableNightHunt);
            buf.writeBoolean(enableExplosiveDeath);
            buf.writeBoolean(enableSupplyDrop);
            buf.writeBoolean(enableHunterRadar);
            buf.writeBoolean(enableCompassChaos);
        }

        public static ConfigSnapshot from(ModConfig config) {
            return new ConfigSnapshot(
                    config.preparingSeconds,
                    config.endingSeconds,
                    config.compassUpdateSeconds,
                    config.hunterRespawnSeconds,
                    config.wildcardIntervalSeconds,
                    config.wildcardDurationSeconds,
                    config.actionBarIntervalSeconds,
                    config.hunterRadarIntervalSeconds,
                    config.supplyDropIntervalSeconds,
                    config.hunterPrepareBoundaryEnabled,
                    config.hunterPrepareBoundaryRadius,
                    config.hunterPrepareBoundaryWarnDistance,
                    config.runnerVictoryType,
                    config.runnerWinMode,
                    config.enableDragonWin,
                    config.enableSurviveTimeWin,
                    config.surviveTimeSeconds,
                    config.enableReachLocationWin,
                    config.targetDimension,
                    config.targetX,
                    config.targetY,
                    config.targetZ,
                    config.targetRadius,
                    config.enableCollectItemWin,
                    config.targetItemId,
                    config.targetItemCount,
                    config.hunterRespawnMode,
                    config.hunterLives,
                    config.runnerRespawnMode,
                    config.runnerLives,
                    config.runnerRespawnSeconds,
                    config.runnerTeamLossMode,
                    config.hunterVictoryType,
                    config.hunterWinByRunnerKillsEnabled,
                    config.hunterRunnerKillTarget,
                    config.enableSpeedRush,
                    config.enableFeatherweight,
                    config.enableGlowing,
                    config.enableNightHunt,
                    config.enableExplosiveDeath,
                    config.enableSupplyDrop,
                    config.enableHunterRadar,
                    config.enableCompassChaos
            );
        }

        public ModConfig toConfig() {
            ModConfig config = new ModConfig();
            config.preparingSeconds = preparingSeconds;
            config.endingSeconds = endingSeconds;
            config.compassUpdateSeconds = compassUpdateSeconds;
            config.hunterRespawnSeconds = hunterRespawnSeconds;
            config.wildcardIntervalSeconds = wildcardIntervalSeconds;
            config.wildcardDurationSeconds = wildcardDurationSeconds;
            config.actionBarIntervalSeconds = actionBarIntervalSeconds;
            config.hunterRadarIntervalSeconds = hunterRadarIntervalSeconds;
            config.supplyDropIntervalSeconds = supplyDropIntervalSeconds;
            config.hunterPrepareBoundaryEnabled = hunterPrepareBoundaryEnabled;
            config.hunterPrepareBoundaryRadius = hunterPrepareBoundaryRadius;
            config.hunterPrepareBoundaryWarnDistance = hunterPrepareBoundaryWarnDistance;
            config.runnerVictoryType = runnerVictoryType;
            config.runnerWinMode = runnerWinMode;
            config.enableDragonWin = enableDragonWin;
            config.enableSurviveTimeWin = enableSurviveTimeWin;
            config.surviveTimeSeconds = surviveTimeSeconds;
            config.enableReachLocationWin = enableReachLocationWin;
            config.targetDimension = targetDimension;
            config.targetX = targetX;
            config.targetY = targetY;
            config.targetZ = targetZ;
            config.targetRadius = targetRadius;
            config.enableCollectItemWin = enableCollectItemWin;
            config.targetItemId = targetItemId;
            config.targetItemCount = targetItemCount;
            config.hunterRespawnMode = hunterRespawnMode;
            config.hunterLives = hunterLives;
            config.runnerRespawnMode = runnerRespawnMode;
            config.runnerLives = runnerLives;
            config.runnerRespawnSeconds = runnerRespawnSeconds;
            config.runnerTeamLossMode = runnerTeamLossMode;
            config.hunterVictoryType = hunterVictoryType;
            config.hunterWinByRunnerKillsEnabled = hunterWinByRunnerKillsEnabled;
            config.hunterRunnerKillTarget = hunterRunnerKillTarget;
            config.enableSpeedRush = enableSpeedRush;
            config.enableFeatherweight = enableFeatherweight;
            config.enableGlowing = enableGlowing;
            config.enableNightHunt = enableNightHunt;
            config.enableExplosiveDeath = enableExplosiveDeath;
            config.enableSupplyDrop = enableSupplyDrop;
            config.enableHunterRadar = enableHunterRadar;
            config.enableCompassChaos = enableCompassChaos;
            config.validate();
            return config;
        }
    }

    public record RequestConfigPayload() implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, RequestConfigPayload> CODEC =
                PacketCodec.of(RequestConfigPayload::write, RequestConfigPayload::read);

        private void write(RegistryByteBuf buf) {
        }

        private static RequestConfigPayload read(RegistryByteBuf buf) {
            return new RequestConfigPayload();
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return C2S_REQUEST_CONFIG;
        }
    }

    public record SyncConfigPayload(
            GameState gameState,
            int hunterCount,
            int runnerCount,
            String activeWildcard,
            String playerRole,
            boolean playerInTeam,
            boolean activeWildcardRunning,
            int phaseRemainingSeconds,
            int activeWildcardRemainingSeconds,
            int nextWildcardSeconds,
            boolean canManage,
            boolean debugPageEnabled,
            ConfigSnapshot config
    ) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, SyncConfigPayload> CODEC =
                PacketCodec.of(SyncConfigPayload::write, SyncConfigPayload::read);

        private void write(RegistryByteBuf buf) {
            buf.writeEnumConstant(gameState);
            buf.writeInt(hunterCount);
            buf.writeInt(runnerCount);
            buf.writeString(activeWildcard);
            buf.writeString(playerRole);
            buf.writeBoolean(playerInTeam);
            buf.writeBoolean(activeWildcardRunning);
            buf.writeInt(phaseRemainingSeconds);
            buf.writeInt(activeWildcardRemainingSeconds);
            buf.writeInt(nextWildcardSeconds);
            buf.writeBoolean(canManage);
            buf.writeBoolean(debugPageEnabled);
            config.write(buf);
        }

        private static SyncConfigPayload read(RegistryByteBuf buf) {
            return new SyncConfigPayload(
                    buf.readEnumConstant(GameState.class),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readString(128),
                    buf.readString(64),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    ConfigSnapshot.fromBuf(buf)
            );
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return S2C_SYNC_CONFIG;
        }
    }

    public record OperationResultPayload(boolean success, String message) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, OperationResultPayload> CODEC =
                PacketCodec.of(OperationResultPayload::write, OperationResultPayload::read);

        private void write(RegistryByteBuf buf) {
            buf.writeBoolean(success);
            buf.writeString(message);
        }

        private static OperationResultPayload read(RegistryByteBuf buf) {
            return new OperationResultPayload(buf.readBoolean(), buf.readString(256));
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return S2C_OPERATION_RESULT;
        }
    }

    public record UpdateConfigPayload(ConfigSnapshot config) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, UpdateConfigPayload> CODEC =
                PacketCodec.of(UpdateConfigPayload::write, UpdateConfigPayload::read);

        private void write(RegistryByteBuf buf) {
            config.write(buf);
        }

        private static UpdateConfigPayload read(RegistryByteBuf buf) {
            return new UpdateConfigPayload(ConfigSnapshot.fromBuf(buf));
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return C2S_UPDATE_CONFIG;
        }
    }

    public record ReloadConfigPayload() implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, ReloadConfigPayload> CODEC =
                PacketCodec.of(ReloadConfigPayload::write, ReloadConfigPayload::read);

        private void write(RegistryByteBuf buf) {
        }

        private static ReloadConfigPayload read(RegistryByteBuf buf) {
            return new ReloadConfigPayload();
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return C2S_RELOAD_CONFIG;
        }
    }

    public record DebugActionPayload(DebugAction action) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, DebugActionPayload> CODEC =
                PacketCodec.of(DebugActionPayload::write, DebugActionPayload::read);

        private void write(RegistryByteBuf buf) {
            buf.writeEnumConstant(action);
        }

        private static DebugActionPayload read(RegistryByteBuf buf) {
            return new DebugActionPayload(buf.readEnumConstant(DebugAction.class));
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return C2S_DEBUG_ACTION;
        }
    }

    public record TestWildcardPayload(String wildcardName) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, TestWildcardPayload> CODEC =
                PacketCodec.of(TestWildcardPayload::write, TestWildcardPayload::read);

        private void write(RegistryByteBuf buf) {
            buf.writeString(wildcardName);
        }

        private static TestWildcardPayload read(RegistryByteBuf buf) {
            return new TestWildcardPayload(buf.readString(64));
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return C2S_TEST_WILDCARD;
        }
    }

    public record TeamActionPayload(TeamAction action) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, TeamActionPayload> CODEC =
                PacketCodec.of(TeamActionPayload::write, TeamActionPayload::read);

        private void write(RegistryByteBuf buf) {
            buf.writeEnumConstant(action);
        }

        private static TeamActionPayload read(RegistryByteBuf buf) {
            return new TeamActionPayload(buf.readEnumConstant(TeamAction.class));
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return C2S_TEAM_ACTION;
        }
    }

    public record GameActionPayload(GameAction action) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, GameActionPayload> CODEC =
                PacketCodec.of(GameActionPayload::write, GameActionPayload::read);

        private void write(RegistryByteBuf buf) {
            buf.writeEnumConstant(action);
        }

        private static GameActionPayload read(RegistryByteBuf buf) {
            return new GameActionPayload(buf.readEnumConstant(GameAction.class));
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return C2S_GAME_ACTION;
        }
    }
}
