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
                canManage,
                canManage && manager.isDebugMenuEnabled(player),
                ConfigSnapshot.from(manager.getConfig())
        );
    }

    private static void handleUpdateConfig(ServerPlayerEntity player, ConfigSnapshot snapshot) {
        if (!HunterWildcardCommand.canManageGame(player.getCommandSource())) {
            reject(player);
            sendSync(player);
            return;
        }

        GameManager manager = GameManager.getInstance();
        manager.applyConfig(snapshot.toConfig());
        if (manager.saveConfig()) {
            manager.getMessageManager().direct(player, "配置已保存。");
        } else {
            player.sendMessage(Text.literal("保存配置失败，请检查服务器日志。"), false);
        }
        syncAll(player.getEntityWorld().getServer());
    }

    private static void handleReloadConfig(ServerPlayerEntity player) {
        if (!HunterWildcardCommand.canManageGame(player.getCommandSource())) {
            reject(player);
            sendSync(player);
            return;
        }

        GameManager manager = GameManager.getInstance();
        manager.reloadConfig();
        manager.getMessageManager().direct(player, "配置已重新加载。");
        syncAll(player.getEntityWorld().getServer());
    }

    private static void handleDebugAction(ServerPlayerEntity player, DebugAction action) {
        if (!HunterWildcardCommand.canManageGame(player.getCommandSource())) {
            reject(player);
            sendSync(player);
            return;
        }

        GameManager manager = GameManager.getInstance();
        if (!manager.isDebugMenuEnabled(player)) {
            manager.getMessageManager().direct(player, "请先使用 /hw ts true 打开调试页。");
            sendSync(player);
            return;
        }

        switch (action) {
            case START_GAME -> manager.start(player.getCommandSource());
            case STOP_GAME -> manager.stop(player.getCommandSource());
            case ROLL_WILDCARD -> manager.rollWildcard(player.getCommandSource());
            case STOP_WILDCARD -> manager.debugStopWildcard(player.getCommandSource());
        }
        syncAll(player.getEntityWorld().getServer());
    }

    private static void handleTestWildcard(ServerPlayerEntity player, String wildcardName) {
        if (!HunterWildcardCommand.canManageGame(player.getCommandSource())) {
            reject(player);
            sendSync(player);
            return;
        }

        GameManager manager = GameManager.getInstance();
        if (!manager.isDebugMenuEnabled(player)) {
            manager.getMessageManager().direct(player, "请先使用 /hw ts true 打开调试页。");
            sendSync(player);
            return;
        }

        manager.testWildcard(player.getCommandSource(), wildcardName, player);
        syncAll(player.getEntityWorld().getServer());
    }

    private static void handleTeamAction(ServerPlayerEntity player, TeamAction action) {
        GameManager manager = GameManager.getInstance();
        switch (action) {
            case JOIN_HUNTER -> manager.join(player, PlayerRole.HUNTER);
            case JOIN_RUNNER -> manager.join(player, PlayerRole.RUNNER);
            case LEAVE -> manager.leave(player);
        }
        syncAll(player.getEntityWorld().getServer());
    }

    private static void handleGameAction(ServerPlayerEntity player, GameAction action) {
        if (!HunterWildcardCommand.canManageGame(player.getCommandSource())) {
            reject(player);
            sendSync(player);
            return;
        }

        GameManager manager = GameManager.getInstance();
        if (action == GameAction.START_GAME) {
            manager.start(player.getCommandSource());
        }
        syncAll(player.getEntityWorld().getServer());
    }

    private static void reject(ServerPlayerEntity player) {
        GameManager.getInstance().getMessageManager().direct(player, "你没有权限执行该操作。");
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
        START_GAME
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
