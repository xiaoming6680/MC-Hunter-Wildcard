package com.xiaoming.hunterwildcard.game;

import com.xiaoming.hunterwildcard.compass.CompassTracker;
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

public class GameManager {
    private static final GameManager INSTANCE = new GameManager();

    private final GameConfig config = new GameConfig();
    private final TeamManager teamManager = new TeamManager();
    private final BossBarManager bossBarManager = new BossBarManager();
    private final MessageManager messageManager = new MessageManager();
    private final CompassTracker compassTracker = new CompassTracker();
    private final RespawnManager respawnManager = new RespawnManager();
    private final WildcardManager wildcardManager = new WildcardManager(bossBarManager, messageManager);
    private final Random random = new Random();

    private GameState state = GameState.WAITING;
    private MinecraftServer server;
    private int preparingTicks;
    private int endingTicks;
    private int actionBarTicks;
    private boolean eventsRegistered;

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
        state = GameState.PREPARING;
        preparingTicks = config.preparingTicks;
        endingTicks = 0;
        actionBarTicks = 0;
        wildcardManager.reset();
        respawnManager.clear();
        compassTracker.reset();

        messageManager.broadcast(server, "游戏开始准备，60 秒后进入追杀阶段。");
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

    public GameConfig getConfig() {
        return config;
    }

    private void tick(MinecraftServer tickServer) {
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
            actionBarTicks = config.actionBarIntervalTicks;
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
            actionBarTicks = config.actionBarIntervalTicks;
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
            respawnManager.onHunterDeath(player, config.hunterRespawnTicks);
            messageManager.toParticipants(context, "猎人 " + player.getName().getString() + " 死亡，将在 10 秒后重新加入追杀。");
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
        endingTicks = config.endingTicks;
        clearRoundEffects(context());
        messageManager.broadcast(server, reason);
    }

    private void cleanupAndReset(MinecraftServer cleanupServer) {
        MinecraftServer targetServer = cleanupServer != null ? cleanupServer : server;
        if (targetServer != null) {
            clearRoundEffects(new GameContext(targetServer, config, teamManager, random));
        }

        teamManager.clear();
        state = GameState.WAITING;
        server = null;
        preparingTicks = 0;
        endingTicks = 0;
        actionBarTicks = 0;
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
}
