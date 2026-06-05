package com.xiaoming.hunterwildcard.game;

import com.xiaoming.hunterwildcard.compass.CompassTracker;
import com.xiaoming.hunterwildcard.config.ModConfig;
import com.xiaoming.hunterwildcard.respawn.RespawnManager;
import com.xiaoming.hunterwildcard.team.PlayerRole;
import com.xiaoming.hunterwildcard.team.TeamManager;
import com.xiaoming.hunterwildcard.ui.BossBarManager;
import com.xiaoming.hunterwildcard.ui.MessageManager;
import com.xiaoming.hunterwildcard.wildcard.WildcardManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Random;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class GameManager {
    private static final GameManager INSTANCE = new GameManager();

    private final ModConfig config = ModConfig.load();
    private final TeamManager teamManager = new TeamManager();
    private final BossBarManager bossBarManager = new BossBarManager();
    private final MessageManager messageManager = new MessageManager();
    private final CompassTracker compassTracker = new CompassTracker();
    private final RespawnManager respawnManager = new RespawnManager();
    private final WildcardManager wildcardManager = new WildcardManager(bossBarManager, messageManager);
    private final Random random = new Random();
    private final Set<UUID> debugMenuPlayers = new HashSet<>();

    private GameState state = GameState.WAITING;
    private MinecraftServer server;
    private int preparingTicks;
    private int endingTicks;
    private int actionBarTicks;
    private boolean eventsRegistered;
    private UUID wildcardTestPlayerUuid;

    private GameManager() {
    }

    public static GameManager getInstance() {
        return INSTANCE;
    }

    public void registerEvents() {
        if (eventsRegistered) {
            return;
        }
        eventsRegistered = true;

        ServerTickEvents.END_SERVER_TICK.register(this::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> handleDisconnect(handler.player));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayerEntity player) {
                handlePlayerDeath(player);
            }
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> handleAfterRespawn(newPlayer));
        ServerLifecycleEvents.SERVER_STOPPING.register(this::handleServerStopping);
    }

    public void start(ServerCommandSource source) {
        if (state != GameState.WAITING) {
            source.sendError(Text.literal("游戏已经开始或正在结束。"));
            return;
        }

        if (teamManager.count(PlayerRole.HUNTER) == 0 || teamManager.count(PlayerRole.RUNNER) == 0) {
            source.sendError(Text.literal("至少需要 1 名猎人和 1 名逃亡者。"));
            return;
        }

        server = source.getServer();
        if (wildcardManager.getActiveRule() != null) {
            wildcardManager.clear(debugContext(server));
            wildcardTestPlayerUuid = null;
        }
        state = GameState.PREPARING;
        preparingTicks = config.getPreparingTicks();
        endingTicks = 0;
        actionBarTicks = 0;
        wildcardManager.reset();
        respawnManager.clear();
        compassTracker.reset();

        messageManager.broadcast(server, "游戏开始准备，" + config.preparingSeconds + " 秒后进入追杀阶段。");
        source.sendFeedback(() -> Text.literal("猎人外卡已进入 PREPARING。"), true);
    }

    public void stop(ServerCommandSource source) {
        MinecraftServer currentServer = source.getServer();
        if (state == GameState.WAITING && teamManager.count(PlayerRole.HUNTER) == 0 && teamManager.count(PlayerRole.RUNNER) == 0) {
            source.sendFeedback(() -> Text.literal("当前没有正在进行的猎人外卡游戏。"), false);
            return;
        }

        cleanupAndReset(currentServer);
        messageManager.broadcast(currentServer, "游戏已由管理员停止，状态已清理。");
        source.sendFeedback(() -> Text.literal("猎人外卡已停止。"), true);
    }

    public void join(ServerPlayerEntity player, PlayerRole role) {
        if (state == GameState.RUNNING || state == GameState.ENDING) {
            messageManager.direct(player, "游戏已经开始，当前不能加入队伍。");
            return;
        }

        teamManager.join(player, role);
        messageManager.direct(player, "你已加入 " + role.getDisplayName() + " 队伍。");
    }

    public void leave(ServerPlayerEntity player) {
        PlayerRole oldRole = teamManager.leave(player);
        respawnManager.remove(player);
        compassTracker.removeCompass(player);
        if (oldRole == null) {
            messageManager.direct(player, "你当前不在游戏队伍中。");
            return;
        }

        messageManager.direct(player, "你已离开 " + oldRole.getDisplayName() + " 队伍。");
        checkWinConditions();
    }

    public String getStatusText() {
        String wildcardName = wildcardManager.getActiveRuleName();
        if (wildcardName == null) {
            wildcardName = "无";
        }

        return "猎人外卡状态: " + state
                + " | 猎人 " + teamManager.count(PlayerRole.HUNTER)
                + " | 逃亡者 " + teamManager.count(PlayerRole.RUNNER)
                + " | 当前外卡 " + wildcardName;
    }

    public GameState getState() {
        return state;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public WildcardManager getWildcardManager() {
        return wildcardManager;
    }

    public ModConfig getConfig() {
        return config;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public void setDebugMenuEnabled(ServerPlayerEntity player, boolean enabled) {
        if (enabled) {
            debugMenuPlayers.add(player.getUuid());
        } else {
            debugMenuPlayers.remove(player.getUuid());
        }
    }

    public boolean isDebugMenuEnabled(ServerPlayerEntity player) {
        return debugMenuPlayers.contains(player.getUuid());
    }

    public void applyConfig(ModConfig newConfig) {
        config.copyFrom(newConfig);
        notifyConfigChanged();
    }

    public boolean saveConfig() {
        return config.save();
    }

    public boolean reloadConfig() {
        config.copyFrom(ModConfig.load());
        notifyConfigChanged();
        return true;
    }

    public void reloadConfig(ServerCommandSource source) {
        reloadConfig();
        source.sendFeedback(() -> Text.literal("已重新读取 config/hunterwildcard.json。"), true);
    }

    public void saveConfig(ServerCommandSource source) {
        if (saveConfig()) {
            source.sendFeedback(() -> Text.literal("已保存 config/hunterwildcard.json。"), true);
        } else {
            source.sendError(Text.literal("保存配置失败，请检查服务器日志。"));
        }
    }

    public void rollWildcard(ServerCommandSource source) {
        if (state != GameState.RUNNING) {
            source.sendError(Text.literal("只有 RUNNING 阶段可以手动触发外卡。"));
            return;
        }

        boolean started = wildcardManager.rollNow(context());
        if (started) {
            source.sendFeedback(() -> Text.literal("已手动随机触发外卡。"), true);
        } else {
            source.sendError(Text.literal("没有可用外卡。"));
        }
    }

    public void testWildcard(ServerCommandSource source, String wildcardName, ServerPlayerEntity tester) {
        MinecraftServer targetServer = source.getServer();
        GameContext testContext = new GameContext(targetServer, config, teamManager, random, List.of(tester));
        boolean started = wildcardManager.startRuleByName(testContext, wildcardName);
        if (started) {
            server = targetServer;
            wildcardTestPlayerUuid = tester.getUuid();
            source.sendFeedback(() -> Text.literal("已测试触发外卡: " + wildcardName), true);
        } else {
            source.sendError(Text.literal("该外卡不可用或已关闭: " + wildcardName));
        }
    }

    public void stopWildcard(ServerCommandSource source) {
        if (state != GameState.RUNNING) {
            source.sendError(Text.literal("只有 RUNNING 阶段可以停止外卡。"));
            return;
        }

        boolean stopped = wildcardManager.stopActiveRule(context());
        if (stopped) {
            source.sendFeedback(() -> Text.literal("已停止当前外卡。"), true);
        } else {
            source.sendError(Text.literal("当前没有正在运行的外卡。"));
        }
    }

    public void debugStopWildcard(ServerCommandSource source) {
        GameContext testContext = state == GameState.RUNNING && server != null ? context() : debugContext(source.getServer());
        boolean stopped = wildcardManager.stopActiveRule(testContext);
        if (stopped) {
            wildcardTestPlayerUuid = null;
            source.sendFeedback(() -> Text.literal("已停止当前外卡。"), true);
        } else {
            source.sendError(Text.literal("当前没有正在运行的外卡。"));
        }
    }

    private void tick(MinecraftServer tickServer) {
        if (state == GameState.WAITING && wildcardManager.getActiveRule() != null) {
            server = tickServer;
            wildcardManager.tick(debugContext(tickServer));
            return;
        }

        if (state == GameState.WAITING) {
            return;
        }

        server = tickServer;
        if (state == GameState.PREPARING) {
            tickPreparing();
            return;
        }

        if (state == GameState.RUNNING) {
            tickRunning();
            return;
        }

        if (state == GameState.ENDING) {
            tickEnding();
        }
    }

    private void tickPreparing() {
        preparingTicks--;
        actionBarTicks--;

        if (actionBarTicks <= 0) {
            actionBarTicks = config.getActionBarIntervalTicks();
            int seconds = Math.max(0, preparingTicks / 20);
            messageManager.actionBar(context(), "准备阶段 | " + seconds + " 秒后开始追杀");
        }

        if (preparingTicks <= 0) {
            startRunning();
        }
    }

    private void startRunning() {
        state = GameState.RUNNING;
        actionBarTicks = 0;
        wildcardManager.reset();
        compassTracker.giveCompasses(context());
        messageManager.broadcast(server, "RUNNING 阶段开始，猎人开始追踪逃亡者。");
    }

    private void tickRunning() {
        GameContext context = context();
        compassTracker.tick(context, wildcardManager.getActiveRule());
        respawnManager.tick(context, compassTracker);
        wildcardManager.tick(context);
        checkWinConditions();

        actionBarTicks--;
        if (actionBarTicks <= 0) {
            actionBarTicks = config.getActionBarIntervalTicks();
            for (ServerPlayerEntity player : context.getParticipants()) {
                PlayerRole role = teamManager.getRole(player);
                String roleName = role == null ? "旁观" : role.getDisplayName();
                messageManager.actionBar(player, "RUNNING | 你的身份: " + roleName);
            }
        }
    }

    private void tickEnding() {
        endingTicks--;
        if (endingTicks <= 0) {
            cleanupAndReset(server);
        }
    }

    private void handlePlayerDeath(ServerPlayerEntity player) {
        if (state != GameState.RUNNING) {
            return;
        }

        GameContext context = context();
        wildcardManager.onPlayerDeath(context, player);

        PlayerRole role = teamManager.getRole(player);
        if (role == PlayerRole.RUNNER) {
            enterEnding("逃亡者 " + player.getName().getString() + " 死亡，猎人阵营获胜。");
            return;
        }

        if (role == PlayerRole.HUNTER) {
            respawnManager.onHunterDeath(player, config.getHunterRespawnTicks());
            messageManager.toParticipants(context, "猎人 " + player.getName().getString() + " 死亡，将在 " + config.hunterRespawnSeconds + " 秒后重新加入追杀。");
        }
    }

    private void handleAfterRespawn(ServerPlayerEntity player) {
        if (state != GameState.RUNNING) {
            return;
        }

        if (teamManager.isHunter(player)) {
            respawnManager.onAfterHunterRespawn(player);
            compassTracker.giveCompass(player);
        }
    }

    private void handleDisconnect(ServerPlayerEntity player) {
        debugMenuPlayers.remove(player.getUuid());
        if (player.getUuid().equals(wildcardTestPlayerUuid)) {
            wildcardTestPlayerUuid = null;
        }
        PlayerRole role = teamManager.leave(player);
        respawnManager.remove(player);
        if (role != null && state != GameState.WAITING) {
            checkWinConditions();
        }
    }

    private void handleServerStopping(MinecraftServer stoppingServer) {
        cleanupAndReset(stoppingServer);
    }

    private void checkWinConditions() {
        if (state != GameState.RUNNING || server == null) {
            return;
        }

        if (teamManager.getRunners(server).isEmpty()) {
            enterEnding("没有在线逃亡者，猎人阵营获胜。");
            return;
        }

        if (teamManager.getHunters(server).isEmpty()) {
            enterEnding("没有在线猎人，逃亡者阵营获胜。");
        }
    }

    private void enterEnding(String reason) {
        if (state == GameState.ENDING || state == GameState.WAITING) {
            return;
        }

        state = GameState.ENDING;
        endingTicks = config.getEndingTicks();
        clearRoundEffects(context());
        messageManager.broadcast(server, reason);
    }

    private void cleanupAndReset(MinecraftServer cleanupServer) {
        MinecraftServer targetServer = cleanupServer != null ? cleanupServer : server;
        if (targetServer != null) {
            clearRoundEffects(debugContext(targetServer));
        }

        teamManager.clear();
        state = GameState.WAITING;
        server = null;
        preparingTicks = 0;
        endingTicks = 0;
        actionBarTicks = 0;
        wildcardTestPlayerUuid = null;
        wildcardManager.reset();
        compassTracker.reset();
        respawnManager.clear();
    }

    private void clearRoundEffects(GameContext context) {
        wildcardManager.clear(context);
        bossBarManager.clear();
        respawnManager.clear(context);
        compassTracker.clear(context);

        for (ServerPlayerEntity player : context.getParticipants()) {
            player.removeStatusEffect(StatusEffects.SPEED);
            player.removeStatusEffect(StatusEffects.JUMP_BOOST);
            player.removeStatusEffect(StatusEffects.SLOW_FALLING);
            player.removeStatusEffect(StatusEffects.GLOWING);
            player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
    }

    private GameContext context() {
        return new GameContext(server, config, teamManager, random);
    }

    private GameContext debugContext(MinecraftServer targetServer) {
        if (wildcardTestPlayerUuid == null) {
            return new GameContext(targetServer, config, teamManager, random);
        }

        ServerPlayerEntity tester = targetServer.getPlayerManager().getPlayer(wildcardTestPlayerUuid);
        if (tester == null) {
            return new GameContext(targetServer, config, teamManager, random);
        }

        return new GameContext(targetServer, config, teamManager, random, List.of(tester));
    }

    private void notifyConfigChanged() {
        wildcardManager.onConfigChanged(config);
        compassTracker.onConfigChanged(config);
    }
}
