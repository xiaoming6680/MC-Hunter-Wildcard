package com.xiaoming.hunterwildcard.network;

import com.xiaoming.hunterwildcard.HunterWildcardMod;
import com.xiaoming.hunterwildcard.command.HunterWildcardCommand;
import com.xiaoming.hunterwildcard.config.ModConfig;
import com.xiaoming.hunterwildcard.game.GameContext;
import com.xiaoming.hunterwildcard.game.GameManager;
import com.xiaoming.hunterwildcard.game.GameState;
import com.xiaoming.hunterwildcard.team.PlayerRole;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
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
    public static final CustomPayload.Id<CloseConfigScreenPayload> S2C_CLOSE_CONFIG_SCREEN =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "close_config_screen"));
    public static final CustomPayload.Id<ClearChatPayload> S2C_CLEAR_CHAT =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "clear_chat"));
    public static final CustomPayload.Id<WildcardDrawPayload> S2C_WILDCARD_DRAW =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "wildcard_draw"));
    public static final CustomPayload.Id<WildcardIntroPayload> S2C_WILDCARD_INTRO =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "wildcard_intro"));
    public static final CustomPayload.Id<HunterKillFeedbackPayload> S2C_HUNTER_KILL_FEEDBACK =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "hunter_kill_feedback"));
    public static final CustomPayload.Id<HudFeedbackPayload> S2C_HUD_FEEDBACK =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "hud_feedback"));
    public static final CustomPayload.Id<ObjectiveStatusPayload> S2C_OBJECTIVE_STATUS =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "objective_status"));
    public static final CustomPayload.Id<ObjectiveNoticePayload> S2C_OBJECTIVE_NOTICE =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "objective_notice"));
    public static final CustomPayload.Id<WeaponOverheatStatusPayload> S2C_WEAPON_OVERHEAT_STATUS =
            new CustomPayload.Id<>(Identifier.of(HunterWildcardMod.MOD_ID, "weapon_overheat_status"));
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
        PayloadTypeRegistry.playS2C().register(S2C_CLOSE_CONFIG_SCREEN, CloseConfigScreenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(S2C_CLEAR_CHAT, ClearChatPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(S2C_WILDCARD_DRAW, WildcardDrawPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(S2C_WILDCARD_INTRO, WildcardIntroPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(S2C_HUNTER_KILL_FEEDBACK, HunterKillFeedbackPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(S2C_HUD_FEEDBACK, HudFeedbackPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(S2C_OBJECTIVE_STATUS, ObjectiveStatusPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(S2C_OBJECTIVE_NOTICE, ObjectiveNoticePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(S2C_WEAPON_OVERHEAT_STATUS, WeaponOverheatStatusPayload.CODEC);
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

    public static void closeConfigScreens(MinecraftServer server) {
        if (server == null) {
            return;
        }

        CloseConfigScreenPayload payload = new CloseConfigScreenPayload();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (ServerPlayNetworking.canSend(player, S2C_CLOSE_CONFIG_SCREEN)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    public static void clearChat(MinecraftServer server) {
        if (server == null) {
            return;
        }

        ClearChatPayload payload = new ClearChatPayload();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (ServerPlayNetworking.canSend(player, S2C_CLEAR_CHAT)) {
                ServerPlayNetworking.send(player, payload);
            }
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

    public static void sendWildcardDraw(GameContext context, String wildcardName) {
        for (ServerPlayerEntity player : context.getParticipants()) {
            if (ServerPlayNetworking.canSend(player, S2C_WILDCARD_DRAW)) {
                ServerPlayNetworking.send(player, new WildcardDrawPayload(wildcardName));
            }
        }
    }

    public static void sendWildcardIntro(GameContext context, String wildcardId, String descriptionSpec) {
        WildcardIntroPayload payload = new WildcardIntroPayload(wildcardId, descriptionSpec, true);
        for (ServerPlayerEntity player : context.getParticipants()) {
            if (ServerPlayNetworking.canSend(player, S2C_WILDCARD_INTRO)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    public static void clearWildcardIntro(GameContext context) {
        WildcardIntroPayload payload = new WildcardIntroPayload("", "", false);
        for (ServerPlayerEntity player : context.getParticipants()) {
            if (ServerPlayNetworking.canSend(player, S2C_WILDCARD_INTRO)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    public static void sendHunterKillFeedback(GameContext context, String hunterName, String runnerName, int remainingKills, int currentKills, int targetKills) {
        HunterKillFeedbackPayload payload = new HunterKillFeedbackPayload(hunterName, runnerName, remainingKills, currentKills, targetKills);
        for (ServerPlayerEntity player : context.getServer().getPlayerManager().getPlayerList()) {
            if (ServerPlayNetworking.canSend(player, S2C_HUNTER_KILL_FEEDBACK)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    public static void sendHudFeedback(GameContext context, String title, String line1, String line2, String style) {
        sendHudFeedback(context.getServer(), title, line1, line2, style);
    }

    public static void sendHudFeedback(MinecraftServer server, String title, String line1, String line2, String style) {
        if (server == null) {
            return;
        }

        HudFeedbackPayload payload = new HudFeedbackPayload(title, line1, line2, style);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (ServerPlayNetworking.canSend(player, S2C_HUD_FEEDBACK)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    public static void sendObjectiveStatus(GameContext context, boolean visible, String text, String style) {
        sendObjectiveStatus(context.getServer(), visible, text, style);
    }

    public static void sendObjectiveStatus(MinecraftServer server, boolean visible, String text, String style) {
        if (server == null) {
            return;
        }

        ObjectiveStatusPayload payload = new ObjectiveStatusPayload(visible, text, style);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (ServerPlayNetworking.canSend(player, S2C_OBJECTIVE_STATUS)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    public static void sendObjectiveNotice(GameContext context, String message, String style) {
        sendObjectiveNotice(context.getServer(), message, style);
    }

    public static void sendObjectiveNotice(MinecraftServer server, String message, String style) {
        if (server == null) {
            return;
        }

        ObjectiveNoticePayload payload = new ObjectiveNoticePayload(message, style);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (ServerPlayNetworking.canSend(player, S2C_OBJECTIVE_NOTICE)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    public static void sendWeaponOverheatStatus(ServerPlayerEntity player, int heat, int maxHeat) {
        if (ServerPlayNetworking.canSend(player, S2C_WEAPON_OVERHEAT_STATUS)) {
            ServerPlayNetworking.send(player, new WeaponOverheatStatusPayload(Math.max(0, heat), Math.max(1, maxHeat), true));
        }
    }

    public static void clearWeaponOverheatStatus(ServerPlayerEntity player) {
        if (ServerPlayNetworking.canSend(player, S2C_WEAPON_OVERHEAT_STATUS)) {
            ServerPlayNetworking.send(player, new WeaponOverheatStatusPayload(0, 1, false));
        }
    }

    private static SyncConfigPayload createSyncPayload(ServerPlayerEntity player) {
        GameManager manager = GameManager.getInstance();
        String activeWildcard = manager.getWildcardManager().getActiveRuleName();
        if (activeWildcard == null) {
            activeWildcard = "";
        }

        PlayerRole playerRole = manager.getTeamManager().getRole(player);
        boolean canManage = HunterWildcardCommand.canManageGame(player.getCommandSource());

        return new SyncConfigPayload(
                manager.getState(),
                manager.getTeamManager().count(PlayerRole.HUNTER),
                manager.getTeamManager().count(PlayerRole.RUNNER),
                activeWildcard,
                playerRole == null ? HunterWildcardText.key("role.not_joined") : playerRole.getDisplayName(),
                playerRole != null,
                manager.getWildcardManager().hasRuleInProgress(),
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
        if (manager.getState() != GameState.WAITING) {
            fail(player, HunterWildcardText.spec("msg.config.cannot_edit_started"));
            return;
        }

        manager.applyConfig(snapshot.toConfig());
        if (manager.saveConfig()) {
            String message = HunterWildcardText.spec("msg.config.saved");
            manager.getMessageManager().directSpec(player, message);
            syncAllAndResult(player, true, message);
        } else {
            String message = HunterWildcardText.spec("msg.config.save_failed");
            player.sendMessage(HunterWildcardText.fromSpec(message), false);
            syncAllAndResult(player, false, message);
        }
    }

    private static void handleReloadConfig(ServerPlayerEntity player) {
        if (!HunterWildcardCommand.canManageGame(player.getCommandSource())) {
            reject(player);
            return;
        }

        GameManager manager = GameManager.getInstance();
        if (!manager.reloadConfig()) {
            fail(player, HunterWildcardText.spec("msg.config.cannot_reload_started"));
            return;
        }

        String message = HunterWildcardText.spec("msg.config.reloaded");
        manager.getMessageManager().directSpec(player, message);
        syncAllAndResult(player, true, message);
    }

    private static void handleDebugAction(ServerPlayerEntity player, DebugAction action) {
        if (!HunterWildcardCommand.canManageGame(player.getCommandSource())) {
            reject(player);
            return;
        }

        GameManager manager = GameManager.getInstance();
        if (!manager.isDebugMenuEnabled(player)) {
            fail(player, HunterWildcardText.spec("msg.debug_menu.required"));
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
                closeConfigScreens(player.getEntityWorld().getServer());
                syncAllAndResult(player, true, HunterWildcardText.spec("msg.game.preparing_started_short"));
            }
            case STOP_GAME -> {
                boolean hadGame = canStopGame(manager);
                manager.stop(player.getCommandSource());
                syncAllAndResult(player, hadGame, hadGame ? HunterWildcardText.spec("msg.game.stopped") : HunterWildcardText.spec("msg.game.none_running"));
            }
            case ROLL_WILDCARD -> {
                if (manager.getState() != GameState.RUNNING) {
                    fail(player, HunterWildcardText.spec("msg.wildcard.roll_requires_running"));
                    return;
                }
                manager.rollWildcard(player.getCommandSource());
                String activeRule = manager.getWildcardManager().getActiveRuleName();
                syncAllAndResult(player, activeRule != null, activeRule == null
                        ? HunterWildcardText.spec("msg.wildcard.none_available")
                        : HunterWildcardText.spec("msg.wildcard.random_triggered", HunterWildcardText.wildcardNameKey(activeRule)));
            }
            case STOP_WILDCARD -> {
                boolean hadWildcard = manager.getWildcardManager().hasRuleInProgress();
                manager.debugStopWildcard(player.getCommandSource());
                syncAllAndResult(player, hadWildcard, hadWildcard ? HunterWildcardText.spec("msg.wildcard.stopped") : HunterWildcardText.spec("msg.wildcard.none_running"));
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
            fail(player, HunterWildcardText.spec("msg.debug_menu.required"));
            return;
        }

        manager.testWildcard(player.getCommandSource(), wildcardName, player);
        String activeRule = manager.getWildcardManager().getActiveRuleName();
        boolean success = activeRule != null && activeRule.equals(wildcardName);
        syncAllAndResult(player, success, success
                ? HunterWildcardText.spec("msg.wildcard.test_triggered", HunterWildcardText.wildcardNameKey(wildcardName))
                : HunterWildcardText.spec("msg.wildcard.unavailable_or_disabled", wildcardName));
    }

    private static void handleTeamAction(ServerPlayerEntity player, TeamAction action) {
        GameManager manager = GameManager.getInstance();
        PlayerRole previousRole = manager.getTeamManager().getRole(player);
        boolean canChangeTeam = manager.getState() == GameState.WAITING;
        if (!canChangeTeam) {
            String message = action == TeamAction.LEAVE
                    ? HunterWildcardText.spec("msg.team.cannot_leave_started")
                    : HunterWildcardText.spec("msg.team.cannot_switch_started");
            fail(player, message);
            return;
        }

        switch (action) {
            case JOIN_HUNTER -> manager.join(player, PlayerRole.HUNTER);
            case JOIN_RUNNER -> manager.join(player, PlayerRole.RUNNER);
            case LEAVE -> manager.leave(player);
        }
        switch (action) {
            case JOIN_HUNTER -> syncAllAndResult(player, true, HunterWildcardText.spec("msg.team.joined", PlayerRole.HUNTER.getTranslationKey()));
            case JOIN_RUNNER -> syncAllAndResult(player, true, HunterWildcardText.spec("msg.team.joined", PlayerRole.RUNNER.getTranslationKey()));
            case LEAVE -> syncAllAndResult(player, previousRole != null, previousRole == null
                    ? HunterWildcardText.spec("msg.team.not_in_team")
                    : HunterWildcardText.spec("msg.team.left", previousRole.getTranslationKey()));
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
                closeConfigScreens(player.getEntityWorld().getServer());
                syncAllAndResult(player, true, HunterWildcardText.spec("msg.game.preparing_started_short"));
            }
            case STOP_GAME -> {
                boolean hadGame = canStopGame(manager);
                manager.stop(player.getCommandSource());
                syncAllAndResult(player, hadGame, hadGame ? HunterWildcardText.spec("msg.game.stopped") : HunterWildcardText.spec("msg.game.none_running"));
            }
            case ROLL_WILDCARD -> {
                if (manager.getState() != GameState.RUNNING) {
                    fail(player, HunterWildcardText.spec("msg.wildcard.roll_requires_running"));
                    return;
                }
                manager.rollWildcard(player.getCommandSource());
                String activeRule = manager.getWildcardManager().getActiveRuleName();
                syncAllAndResult(player, activeRule != null, activeRule == null
                        ? HunterWildcardText.spec("msg.wildcard.none_available")
                        : HunterWildcardText.spec("msg.wildcard.random_triggered", HunterWildcardText.wildcardNameKey(activeRule)));
            }
        }
    }

    private static void reject(ServerPlayerEntity player) {
        fail(player, HunterWildcardText.spec("msg.permission.denied"));
    }

    private static void fail(ServerPlayerEntity player, String messageSpec) {
        GameManager.getInstance().getMessageManager().directSpec(player, messageSpec);
        sendSyncAndResult(player, false, messageSpec);
    }

    private static String validateStartGame(GameManager manager) {
        if (manager.getState() != GameState.WAITING) {
            return HunterWildcardText.spec("msg.game.already_started_or_ending");
        }

        if (manager.getTeamManager().count(PlayerRole.HUNTER) == 0 || manager.getTeamManager().count(PlayerRole.RUNNER) == 0) {
            return HunterWildcardText.spec("msg.game.need_teams");
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
            int blockDecaySeconds,
            int pearlFrenzyMaxPearls,
            int pearlFrenzyIntervalSeconds,
            int windChargeBrawlIntervalSeconds,
            int windChargeExplosionMultiplierPercent,
            boolean hunterPrepareBoundaryEnabled,
            int hunterPrepareBoundaryRadius,
            int hunterPrepareBoundaryWarnDistance,
            boolean runnerDeathNoDrops,
            boolean hunterDeathNoDrops,
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
            boolean enableCompassChaos,
            boolean enableHungerChase,
            boolean enableWeaponOverheat,
            boolean enableLightLoad,
            boolean enableBlockDecay,
            boolean enablePearlFrenzy,
            boolean enableWindChargeBrawl,
            boolean enableBloodRage,
            boolean enableDisabledWildcard
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
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readBoolean(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readBoolean(),
                    buf.readBoolean(),
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
                    buf.readBoolean(),
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
            buf.writeInt(blockDecaySeconds);
            buf.writeInt(pearlFrenzyMaxPearls);
            buf.writeInt(pearlFrenzyIntervalSeconds);
            buf.writeInt(windChargeBrawlIntervalSeconds);
            buf.writeInt(windChargeExplosionMultiplierPercent);
            buf.writeBoolean(hunterPrepareBoundaryEnabled);
            buf.writeInt(hunterPrepareBoundaryRadius);
            buf.writeInt(hunterPrepareBoundaryWarnDistance);
            buf.writeBoolean(runnerDeathNoDrops);
            buf.writeBoolean(hunterDeathNoDrops);
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
            buf.writeBoolean(enableHungerChase);
            buf.writeBoolean(enableWeaponOverheat);
            buf.writeBoolean(enableLightLoad);
            buf.writeBoolean(enableBlockDecay);
            buf.writeBoolean(enablePearlFrenzy);
            buf.writeBoolean(enableWindChargeBrawl);
            buf.writeBoolean(enableBloodRage);
            buf.writeBoolean(enableDisabledWildcard);
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
                    config.blockDecaySeconds,
                    config.pearlFrenzyMaxPearls,
                    config.pearlFrenzyIntervalSeconds,
                    config.windChargeBrawlIntervalSeconds,
                    config.windChargeExplosionMultiplierPercent,
                    config.hunterPrepareBoundaryEnabled,
                    config.hunterPrepareBoundaryRadius,
                    config.hunterPrepareBoundaryWarnDistance,
                    config.runnerDeathNoDrops,
                    config.hunterDeathNoDrops,
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
                    config.enableCompassChaos,
                    config.enableHungerChase,
                    config.enableWeaponOverheat,
                    config.enableLightLoad,
                    config.enableBlockDecay,
                    config.enablePearlFrenzy,
                    config.enableWindChargeBrawl,
                    config.enableBloodRage,
                    config.enableDisabledWildcard
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
            config.blockDecaySeconds = blockDecaySeconds;
            config.pearlFrenzyMaxPearls = pearlFrenzyMaxPearls;
            config.pearlFrenzyIntervalSeconds = pearlFrenzyIntervalSeconds;
            config.windChargeBrawlIntervalSeconds = windChargeBrawlIntervalSeconds;
            config.windChargeExplosionMultiplierPercent = windChargeExplosionMultiplierPercent;
            config.hunterPrepareBoundaryEnabled = hunterPrepareBoundaryEnabled;
            config.hunterPrepareBoundaryRadius = hunterPrepareBoundaryRadius;
            config.hunterPrepareBoundaryWarnDistance = hunterPrepareBoundaryWarnDistance;
            config.runnerDeathNoDrops = runnerDeathNoDrops;
            config.hunterDeathNoDrops = hunterDeathNoDrops;
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
            config.enableHungerChase = enableHungerChase;
            config.enableWeaponOverheat = enableWeaponOverheat;
            config.enableLightLoad = enableLightLoad;
            config.enableBlockDecay = enableBlockDecay;
            config.enablePearlFrenzy = enablePearlFrenzy;
            config.enableWindChargeBrawl = enableWindChargeBrawl;
            config.enableBloodRage = enableBloodRage;
            config.enableDisabledWildcard = enableDisabledWildcard;
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

    public record CloseConfigScreenPayload() implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, CloseConfigScreenPayload> CODEC =
                PacketCodec.of(CloseConfigScreenPayload::write, CloseConfigScreenPayload::read);

        private void write(RegistryByteBuf buf) {
        }

        private static CloseConfigScreenPayload read(RegistryByteBuf buf) {
            return new CloseConfigScreenPayload();
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return S2C_CLOSE_CONFIG_SCREEN;
        }
    }

    public record ClearChatPayload() implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, ClearChatPayload> CODEC =
                PacketCodec.of(ClearChatPayload::write, ClearChatPayload::read);

        private void write(RegistryByteBuf buf) {
        }

        private static ClearChatPayload read(RegistryByteBuf buf) {
            return new ClearChatPayload();
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return S2C_CLEAR_CHAT;
        }
    }

    public record WildcardDrawPayload(String wildcardName) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, WildcardDrawPayload> CODEC =
                PacketCodec.of(WildcardDrawPayload::write, WildcardDrawPayload::read);

        private void write(RegistryByteBuf buf) {
            buf.writeString(wildcardName);
        }

        private static WildcardDrawPayload read(RegistryByteBuf buf) {
            return new WildcardDrawPayload(buf.readString(64));
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return S2C_WILDCARD_DRAW;
        }
    }

    public record WildcardIntroPayload(String wildcardName, String description, boolean visible) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, WildcardIntroPayload> CODEC =
                PacketCodec.of(WildcardIntroPayload::write, WildcardIntroPayload::read);

        private void write(RegistryByteBuf buf) {
            buf.writeString(wildcardName == null ? "" : wildcardName);
            buf.writeString(description == null ? "" : description);
            buf.writeBoolean(visible);
        }

        private static WildcardIntroPayload read(RegistryByteBuf buf) {
            return new WildcardIntroPayload(
                    buf.readString(64),
                    buf.readString(160),
                    buf.readBoolean()
            );
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return S2C_WILDCARD_INTRO;
        }
    }

    public record HunterKillFeedbackPayload(String hunterName, String runnerName, int remainingKills, int currentKills, int targetKills) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, HunterKillFeedbackPayload> CODEC =
                PacketCodec.of(HunterKillFeedbackPayload::write, HunterKillFeedbackPayload::read);

        private void write(RegistryByteBuf buf) {
            buf.writeString(hunterName);
            buf.writeString(runnerName);
            buf.writeInt(remainingKills);
            buf.writeInt(currentKills);
            buf.writeInt(targetKills);
        }

        private static HunterKillFeedbackPayload read(RegistryByteBuf buf) {
            return new HunterKillFeedbackPayload(
                    buf.readString(64),
                    buf.readString(64),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt()
            );
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return S2C_HUNTER_KILL_FEEDBACK;
        }
    }

    public record HudFeedbackPayload(String title, String line1, String line2, String style) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, HudFeedbackPayload> CODEC =
                PacketCodec.of(HudFeedbackPayload::write, HudFeedbackPayload::read);

        private void write(RegistryByteBuf buf) {
            buf.writeString(title == null ? "" : title);
            buf.writeString(line1 == null ? "" : line1);
            buf.writeString(line2 == null ? "" : line2);
            buf.writeString(style == null ? "" : style);
        }

        private static HudFeedbackPayload read(RegistryByteBuf buf) {
            return new HudFeedbackPayload(
                    buf.readString(64),
                    buf.readString(128),
                    buf.readString(128),
                    buf.readString(32)
            );
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return S2C_HUD_FEEDBACK;
        }
    }

    public record ObjectiveStatusPayload(boolean visible, String text, String style) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, ObjectiveStatusPayload> CODEC =
                PacketCodec.of(ObjectiveStatusPayload::write, ObjectiveStatusPayload::read);

        private void write(RegistryByteBuf buf) {
            buf.writeBoolean(visible);
            buf.writeString(text == null ? "" : text);
            buf.writeString(style == null ? "" : style);
        }

        private static ObjectiveStatusPayload read(RegistryByteBuf buf) {
            return new ObjectiveStatusPayload(
                    buf.readBoolean(),
                    buf.readString(192),
                    buf.readString(32)
            );
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return S2C_OBJECTIVE_STATUS;
        }
    }

    public record ObjectiveNoticePayload(String message, String style) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, ObjectiveNoticePayload> CODEC =
                PacketCodec.of(ObjectiveNoticePayload::write, ObjectiveNoticePayload::read);

        private void write(RegistryByteBuf buf) {
            buf.writeString(message == null ? "" : message);
            buf.writeString(style == null ? "" : style);
        }

        private static ObjectiveNoticePayload read(RegistryByteBuf buf) {
            return new ObjectiveNoticePayload(
                    buf.readString(192),
                    buf.readString(32)
            );
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return S2C_OBJECTIVE_NOTICE;
        }
    }

    public record WeaponOverheatStatusPayload(int heat, int maxHeat, boolean visible) implements CustomPayload {
        public static final PacketCodec<RegistryByteBuf, WeaponOverheatStatusPayload> CODEC =
                PacketCodec.of(WeaponOverheatStatusPayload::write, WeaponOverheatStatusPayload::read);

        private void write(RegistryByteBuf buf) {
            buf.writeInt(heat);
            buf.writeInt(maxHeat);
            buf.writeBoolean(visible);
        }

        private static WeaponOverheatStatusPayload read(RegistryByteBuf buf) {
            return new WeaponOverheatStatusPayload(buf.readInt(), buf.readInt(), buf.readBoolean());
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return S2C_WEAPON_OVERHEAT_STATUS;
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
