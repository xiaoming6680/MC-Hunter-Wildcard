package com.xiaoming.hunterwildcard.game;

import com.xiaoming.hunterwildcard.compass.CompassTracker;
import com.xiaoming.hunterwildcard.config.ModConfig;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets;
import com.xiaoming.hunterwildcard.prepare.HunterBoundaryManager;
import com.xiaoming.hunterwildcard.respawn.RespawnManager;
import com.xiaoming.hunterwildcard.team.PlayerRole;
import com.xiaoming.hunterwildcard.team.TeamManager;
import com.xiaoming.hunterwildcard.ui.BossBarManager;
import com.xiaoming.hunterwildcard.ui.MessageManager;
import com.xiaoming.hunterwildcard.util.HunterWildcardText;
import com.xiaoming.hunterwildcard.wildcard.WildcardManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.rule.GameRules;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
    private final HunterBoundaryManager hunterBoundaryManager = new HunterBoundaryManager();
    private final WinConditionManager winConditionManager = new WinConditionManager();
    private final WildcardManager wildcardManager = new WildcardManager(bossBarManager, messageManager);
    private final Random random = new Random();
    private final Set<UUID> debugMenuPlayers = new HashSet<>();
    private final Map<UUID, List<ItemStack>> preservedDeathInventories = new HashMap<>();

    private GameState state = GameState.WAITING;
    private MinecraftServer server;
    private int preparingTicks;
    private int endingTicks;
    private int actionBarTicks;
    private boolean eventsRegistered;
    private UUID wildcardTestPlayerUuid;
    private Boolean previousLocatorBarRule;

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
                handlePlayerDeath(player, damageSource);
            } else {
                handleEntityKilled(entity, damageSource);
            }

            if (entity instanceof EnderDragonEntity) {
                handleDragonDeath();
            }
        });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer) {
                handlePlayerAttack(serverPlayer, entity);
            }
            return ActionResult.PASS;
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer) {
                handleItemUse(serverPlayer, hand, player.getStackInHand(hand));
            }
            return ActionResult.PASS;
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> handleAfterRespawn(newPlayer));
        ServerLifecycleEvents.SERVER_STOPPING.register(this::handleServerStopping);
    }

    public void start(ServerCommandSource source) {
        if (state != GameState.WAITING) {
            source.sendError(HunterWildcardText.translatable("msg.game.already_started_or_ending"));
            return;
        }

        if (teamManager.count(PlayerRole.HUNTER) == 0 || teamManager.count(PlayerRole.RUNNER) == 0) {
            source.sendError(HunterWildcardText.translatable("msg.game.need_teams"));
            return;
        }

        server = source.getServer();
        if (wildcardManager.hasRuleInProgress()) {
            wildcardManager.clear(debugContext(server));
            wildcardTestPlayerUuid = null;
        }
        state = GameState.PREPARING;
        preparingTicks = config.getPreparingTicks();
        endingTicks = 0;
        actionBarTicks = 0;
        disableLocatorBarForRound(server);
        wildcardManager.reset();
        respawnManager.clear();
        winConditionManager.clear();
        compassTracker.reset();
        hunterBoundaryManager.start(context());
        bossBarManager.updatePrepareBar(context(), preparingTicks, config.getPreparingTicks());

        HunterWildcardPackets.clearChat(server);
        messageManager.broadcast(server, HunterWildcardText.translatable("msg.game.preparing_started", config.preparingSeconds));
        source.sendFeedback(() -> HunterWildcardText.translatable("command.start.preparing"), true);
    }

    public void stop(ServerCommandSource source) {
        MinecraftServer currentServer = source.getServer();
        if (state == GameState.WAITING && teamManager.count(PlayerRole.HUNTER) == 0 && teamManager.count(PlayerRole.RUNNER) == 0) {
            source.sendFeedback(() -> HunterWildcardText.translatable("msg.game.none_running"), false);
            return;
        }

        cleanupAndReset(currentServer);
        messageManager.broadcast(currentServer, HunterWildcardText.translatable("msg.game.stopped_by_admin"));
        source.sendFeedback(() -> HunterWildcardText.translatable("command.stop.stopped"), true);
    }

    public void join(ServerPlayerEntity player, PlayerRole role) {
        if (state != GameState.WAITING) {
            messageManager.direct(player, HunterWildcardText.translatable("msg.team.cannot_switch_started"));
            return;
        }

        teamManager.join(player, role);
        messageManager.direct(player, HunterWildcardText.translatable("msg.team.joined", role.getDisplayText()));
    }

    public void leave(ServerPlayerEntity player) {
        if (state != GameState.WAITING) {
            messageManager.direct(player, HunterWildcardText.translatable("msg.team.cannot_leave_started"));
            return;
        }

        PlayerRole oldRole = teamManager.leave(player);
        respawnManager.remove(player);
        hunterBoundaryManager.remove(player);
        compassTracker.removeCompass(player);
        if (oldRole == null) {
            messageManager.direct(player, HunterWildcardText.translatable("msg.team.not_in_team"));
            return;
        }

        messageManager.direct(player, HunterWildcardText.translatable("msg.team.left", oldRole.getDisplayText()));
        checkWinConditions();
    }

    public Text getStatusText() {
        String wildcardName = wildcardManager.getActiveRuleName();
        Text wildcardText = wildcardName == null
                ? HunterWildcardText.translatable("common.none")
                : HunterWildcardText.wildcardName(wildcardName);
        return HunterWildcardText.translatable(
                "command.status",
                HunterWildcardText.translatable("state." + state.name().toLowerCase()),
                teamManager.count(PlayerRole.HUNTER),
                teamManager.count(PlayerRole.RUNNER),
                wildcardText
        );
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

    public int getPhaseRemainingTicks() {
        return switch (state) {
            case PREPARING -> Math.max(0, preparingTicks);
            case ENDING -> Math.max(0, endingTicks);
            default -> -1;
        };
    }

    public int getActiveWildcardRemainingTicks() {
        return wildcardManager.getActiveRemainingTicks();
    }

    public int getTicksUntilNextWildcard() {
        return wildcardManager.getTicksUntilNextWildcard();
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
        if (state != GameState.WAITING) {
            return false;
        }

        config.copyFrom(ModConfig.load());
        notifyConfigChanged();
        return true;
    }

    public void reloadConfig(ServerCommandSource source) {
        if (reloadConfig()) {
            source.sendFeedback(() -> HunterWildcardText.translatable("command.config.reload.success"), true);
        } else {
            source.sendError(HunterWildcardText.translatable("msg.config.cannot_reload_started"));
        }
    }

    public void saveConfig(ServerCommandSource source) {
        if (state != GameState.WAITING) {
            source.sendError(HunterWildcardText.translatable("msg.config.cannot_save_started"));
            return;
        }

        if (saveConfig()) {
            source.sendFeedback(() -> HunterWildcardText.translatable("command.config.save.success"), true);
        } else {
            source.sendError(HunterWildcardText.translatable("msg.config.save_failed"));
        }
    }

    public void rollWildcard(ServerCommandSource source) {
        if (state != GameState.RUNNING) {
            source.sendError(HunterWildcardText.translatable("msg.wildcard.roll_requires_running"));
            return;
        }

        boolean started = wildcardManager.rollNow(context());
        if (started) {
            source.sendFeedback(() -> HunterWildcardText.translatable("command.wildcard.roll.success"), true);
        } else {
            source.sendError(HunterWildcardText.translatable("msg.wildcard.none_available"));
        }
    }

    public void testWildcard(ServerCommandSource source, String wildcardName, ServerPlayerEntity tester) {
        MinecraftServer targetServer = source.getServer();
        GameContext testContext = new GameContext(targetServer, config, teamManager, random, List.of(tester));
        boolean started = wildcardManager.startRuleByName(testContext, wildcardName);
        if (started) {
            server = targetServer;
            wildcardTestPlayerUuid = tester.getUuid();
            source.sendFeedback(() -> HunterWildcardText.translatable("command.wildcard.test.success", HunterWildcardText.wildcardName(wildcardName)), true);
        } else {
            source.sendError(HunterWildcardText.translatable("msg.wildcard.unavailable_or_disabled", Text.literal(wildcardName)));
        }
    }

    public void stopWildcard(ServerCommandSource source) {
        if (state != GameState.RUNNING) {
            source.sendError(HunterWildcardText.translatable("msg.wildcard.stop_requires_running"));
            return;
        }

        boolean stopped = wildcardManager.stopActiveRule(context());
        if (stopped) {
            source.sendFeedback(() -> HunterWildcardText.translatable("command.wildcard.stop.success"), true);
        } else {
            source.sendError(HunterWildcardText.translatable("msg.wildcard.none_running"));
        }
    }

    public void debugStopWildcard(ServerCommandSource source) {
        GameContext testContext = state == GameState.RUNNING && server != null ? context() : debugContext(source.getServer());
        boolean stopped = wildcardManager.stopActiveRule(testContext);
        if (stopped) {
            wildcardTestPlayerUuid = null;
            source.sendFeedback(() -> HunterWildcardText.translatable("command.wildcard.stop.success"), true);
        } else {
            source.sendError(HunterWildcardText.translatable("msg.wildcard.none_running"));
        }
    }

    private void tick(MinecraftServer tickServer) {
        if (state == GameState.WAITING && wildcardManager.hasRuleInProgress()) {
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
        GameContext context = context();
        hunterBoundaryManager.tick(context);
        bossBarManager.updatePrepareBar(context, preparingTicks, config.getPreparingTicks());

        if (preparingTicks <= 0) {
            startRunning();
        }
    }

    private void startRunning() {
        state = GameState.RUNNING;
        actionBarTicks = 0;
        GameContext context = context();
        hunterBoundaryManager.clear();
        bossBarManager.clearPrepareBar();
        respawnManager.start(context);
        winConditionManager.start(context);
        wildcardManager.reset();
        compassTracker.giveCompasses(context);
        messageManager.broadcast(server, HunterWildcardText.translatable("msg.game.running_started"));
    }

    private void tickRunning() {
        GameContext context = context();
        compassTracker.tick(context, wildcardManager.getActiveRule());
        compassTracker.removeRunnerCompasses(context);
        respawnManager.tick(context, compassTracker);
        wildcardManager.tick(context);
        checkWinConditions();
        if (state != GameState.RUNNING) {
            return;
        }

        String winReason = winConditionManager.tick(context);
        if (winReason != null) {
            enterEnding(winReason);
            return;
        }

        actionBarTicks--;
        if (actionBarTicks <= 0) {
            actionBarTicks = config.getActionBarIntervalTicks();
            for (ServerPlayerEntity player : context.getParticipants()) {
                if (respawnManager.isWaitingForRespawn(player)) {
                    continue;
                }

                PlayerRole role = teamManager.getRole(player);
                Text roleName = role == null ? HunterWildcardText.translatable("role.spectator") : role.getDisplayText();
                messageManager.actionBar(player, HunterWildcardText.translatable("hud.actionbar.running_role", roleName));
            }
        }
    }

    private void tickEnding() {
        endingTicks--;
        if (endingTicks <= 0) {
            cleanupAndReset(server);
        }
    }

    private void handlePlayerDeath(ServerPlayerEntity player, DamageSource damageSource) {
        if (state != GameState.RUNNING) {
            return;
        }

        GameContext context = context();
        wildcardManager.onPlayerDeath(context, player);

        PlayerRole role = teamManager.getRole(player);
        ServerPlayerEntity hunterKiller = damageSource.getAttacker() instanceof ServerPlayerEntity attacker && teamManager.isHunter(attacker)
                ? attacker
                : null;
        ServerPlayerEntity runnerKiller = damageSource.getAttacker() instanceof ServerPlayerEntity attacker && teamManager.isRunner(attacker)
                ? attacker
                : null;
        if (role == PlayerRole.HUNTER && runnerKiller != null) {
            HunterWildcardPackets.sendHudFeedback(
                    context,
                    HunterWildcardText.spec("hud.feedback.counter_kill.title"),
                    runnerKiller.getName().getString() + " -> " + player.getName().getString(),
                    HunterWildcardText.spec("hud.feedback.counter_kill.subtitle"),
                    "runner"
            );
        }
        RespawnManager.DeathOutcome outcome = respawnManager.onPlayerDeath(context, player, role, hunterKiller);
        if (outcome.message() != null) {
            messageManager.toParticipants(context, outcome.message());
        }
        if (outcome.endingReason() != null) {
            enterEnding(outcome.endingReason());
        }
    }

    private void handleEntityKilled(LivingEntity entity, DamageSource damageSource) {
        if (state != GameState.RUNNING || !(damageSource.getAttacker() instanceof ServerPlayerEntity killer)) {
            return;
        }

        wildcardManager.onEntityKilled(context(), killer, entity);
    }

    private void handleAfterRespawn(ServerPlayerEntity player) {
        restorePreservedDeathInventory(player);
        if (state != GameState.RUNNING) {
            return;
        }

        respawnManager.onAfterRespawn(context(), player, compassTracker);
    }

    public void handlePlayerAttack(ServerPlayerEntity player, Entity target) {
        GameContext eventContext = wildcardEventContext(player);
        if (eventContext != null) {
            wildcardManager.onPlayerAttack(eventContext, player, target);
        }
    }

    public void handleItemUse(ServerPlayerEntity player, Hand hand, ItemStack stack) {
        GameContext eventContext = wildcardEventContext(player);
        if (eventContext != null) {
            wildcardManager.onItemUse(eventContext, player, hand, stack);
        }
    }

    public void handlePlayerAteFood(ServerPlayerEntity player, ItemStack eatenStack) {
        GameContext eventContext = wildcardEventContext(player);
        if (eventContext != null) {
            wildcardManager.onPlayerAteFood(eventContext, player, eatenStack);
        }
    }

    public void handleBlockPlaced(ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state) {
        GameContext eventContext = wildcardEventContext(player);
        if (eventContext != null) {
            wildcardManager.onBlockPlaced(eventContext, player, world, pos, state);
        }
    }

    public boolean handleDeathInventoryDrop(ServerPlayerEntity player) {
        if (state != GameState.RUNNING) {
            return false;
        }

        PlayerRole role = teamManager.getRole(player);
        if (role == PlayerRole.HUNTER) {
            compassTracker.normalizeHunterCompasses(player);
            if (config.hunterDeathNoDrops) {
                preserveDeathInventory(player);
                return true;
            }

            compassTracker.removeCompass(player);
            return false;
        }

        if (role == PlayerRole.RUNNER && config.runnerDeathNoDrops) {
            preserveDeathInventory(player);
            return true;
        }

        return false;
    }

    private void preserveDeathInventory(ServerPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        List<ItemStack> stacks = new ArrayList<>(inventory.size());
        for (int slot = 0; slot < inventory.size(); slot++) {
            stacks.add(inventory.getStack(slot).copy());
        }
        preservedDeathInventories.put(player.getUuid(), stacks);
    }

    private void restorePreservedDeathInventory(ServerPlayerEntity player) {
        List<ItemStack> stacks = preservedDeathInventories.remove(player.getUuid());
        if (stacks == null) {
            return;
        }

        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = slot < stacks.size() ? stacks.get(slot).copy() : ItemStack.EMPTY;
            inventory.setStack(slot, stack);
        }
        if (teamManager.isHunter(player)) {
            compassTracker.normalizeHunterCompasses(player);
        } else if (teamManager.isRunner(player)) {
            compassTracker.removeCompass(player);
        }
    }

    private void handleDragonDeath() {
        if (state != GameState.RUNNING || server == null) {
            return;
        }

        String reason = winConditionManager.onDragonKilled(context());
        if (reason != null) {
            enterEnding(reason);
        }
    }

    private void handleDisconnect(ServerPlayerEntity player) {
        debugMenuPlayers.remove(player.getUuid());
        if (player.getUuid().equals(wildcardTestPlayerUuid)) {
            wildcardTestPlayerUuid = null;
        }
        PlayerRole role = teamManager.leave(player);
        respawnManager.remove(player);
        hunterBoundaryManager.remove(player);
        preservedDeathInventories.remove(player.getUuid());
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
            enterEnding(HunterWildcardText.spec("msg.win.no_online_runners"));
            return;
        }

        if (teamManager.getHunters(server).isEmpty()) {
            enterEnding(HunterWildcardText.spec("msg.win.no_online_hunters"));
        }
    }

    public void endGameWithReason(String reason) {
        enterEnding(reason);
    }

    private void enterEnding(String reason) {
        if (state == GameState.ENDING || state == GameState.WAITING) {
            return;
        }

        state = GameState.ENDING;
        endingTicks = config.getEndingTicks();
        GameContext endingContext = context();
        WinningSide winningSide = classifyWinner(reason);
        HunterWildcardPackets.clearChat(server);
        sendEndingFeedback(reason, winningSide);
        sendEndingSummary(endingContext, reason, winningSide);
        launchWinnerFireworks(endingContext, winningSide);
        clearRoundEffects(endingContext);
    }

    private void sendEndingFeedback(String reason, WinningSide winningSide) {
        if (winningSide == WinningSide.NONE) {
            return;
        }

        boolean runnerWin = winningSide == WinningSide.RUNNERS;
        String title = runnerWin
                ? HunterWildcardText.spec("hud.feedback.runner_victory.title")
                : HunterWildcardText.spec("hud.feedback.hunter_victory.title");
        String style = runnerWin ? "runner" : "hunter";
        HunterWildcardPackets.sendHudFeedback(server, title, compactEndingReason(reason), HunterWildcardText.spec("hud.feedback.ending.line2"), style);
    }

    private WinningSide classifyWinner(String reason) {
        String key = specKey(reason);
        if (key.equals(HunterWildcardText.key("msg.win.no_online_hunters"))
                || key.startsWith(HunterWildcardText.key("msg.win.runner."))) {
            return WinningSide.RUNNERS;
        }
        if (key.equals(HunterWildcardText.key("msg.win.no_online_runners"))
                || key.startsWith(HunterWildcardText.key("msg.win.hunter."))) {
            return WinningSide.HUNTERS;
        }
        return WinningSide.NONE;
    }

    private void sendEndingSummary(GameContext context, String reason, WinningSide winningSide) {
        Text winnerName = switch (winningSide) {
            case HUNTERS -> HunterWildcardText.translatable("team.hunters");
            case RUNNERS -> HunterWildcardText.translatable("team.runners");
            case NONE -> HunterWildcardText.translatable("common.undecided");
        };
        Formatting winnerColor = switch (winningSide) {
            case HUNTERS -> Formatting.RED;
            case RUNNERS -> Formatting.AQUA;
            case NONE -> Formatting.GOLD;
        };
        List<ServerPlayerEntity> hunters = context.getHunters();
        List<ServerPlayerEntity> runners = context.getRunners();
        String wildcardName = wildcardManager.getActiveRuleName();
        Text wildcardText = wildcardName == null || wildcardName.isBlank()
                ? HunterWildcardText.translatable("common.none")
                : HunterWildcardText.wildcardName(wildcardName);

        broadcastEndingLine(Text.empty());
        broadcastEndingLine(HunterWildcardText.translatable("msg.ending.summary.header").formatted(Formatting.GOLD, Formatting.BOLD));
        broadcastEndingLine(Text.empty()
                .append(HunterWildcardText.translatable("msg.ending.summary.winner.label").formatted(Formatting.GRAY))
                .append(winnerName.copy().formatted(winnerColor, Formatting.BOLD)));
        broadcastEndingLine(Text.empty()
                .append(HunterWildcardText.translatable("msg.ending.summary.reason.label").formatted(Formatting.GRAY))
                .append((reason == null || reason.isBlank() ? HunterWildcardText.translatable("common.no_reason") : HunterWildcardText.fromSpec(reason)).copy().formatted(Formatting.WHITE)));
        broadcastEndingLine(Text.empty()
                .append(HunterWildcardText.translatable("msg.ending.summary.wildcard.label").formatted(Formatting.GRAY))
                .append(wildcardText.copy().formatted(Formatting.YELLOW)));
        broadcastEndingLine(Text.empty()
                .append(HunterWildcardText.translatable("msg.ending.summary.online.label").formatted(Formatting.GRAY))
                .append(HunterWildcardText.translatable("msg.ending.summary.online.value", hunters.size(), runners.size()).formatted(Formatting.WHITE)));
        broadcastEndingLine(Text.empty()
                .append(HunterWildcardText.translatable("msg.ending.summary.hunters.label").formatted(Formatting.RED))
                .append(formatPlayerNames(hunters).copy().formatted(Formatting.WHITE)));
        broadcastEndingLine(Text.empty()
                .append(HunterWildcardText.translatable("msg.ending.summary.runners.label").formatted(Formatting.AQUA))
                .append(formatPlayerNames(runners).copy().formatted(Formatting.WHITE)));
        broadcastEndingLine(HunterWildcardText.translatable("msg.ending.summary.footer").formatted(Formatting.GOLD));
    }

    private void broadcastEndingLine(Text text) {
        if (server != null) {
            server.getPlayerManager().broadcast(text, false);
        }
    }

    private Text formatPlayerNames(List<ServerPlayerEntity> players) {
        if (players.isEmpty()) {
            return HunterWildcardText.translatable("msg.ending.summary.no_online_players");
        }

        int displayed = Math.min(players.size(), 6);
        var names = Text.empty();
        for (int i = 0; i < displayed; i++) {
            if (i > 0) {
                names.append(Text.literal(", "));
            }
            names.append(players.get(i).getName());
        }
        if (players.size() > displayed) {
            names.append(HunterWildcardText.translatable("msg.ending.summary.more_players", players.size()));
        }
        return names;
    }

    private void launchWinnerFireworks(GameContext context, WinningSide winningSide) {
        List<ServerPlayerEntity> winners = switch (winningSide) {
            case HUNTERS -> context.getHunters();
            case RUNNERS -> context.getRunners();
            case NONE -> List.of();
        };
        for (ServerPlayerEntity winner : winners) {
            ServerWorld world = winner.getEntityWorld();
            for (int i = 0; i < 2; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double radius = 0.8D + random.nextDouble() * 0.7D;
                double x = winner.getX() + Math.cos(angle) * radius;
                double y = winner.getY() + 4.0D + i * 0.6D;
                double z = winner.getZ() + Math.sin(angle) * radius;
                FireworkRocketEntity firework = new FireworkRocketEntity(world, createWinnerFirework(winningSide), x, y, z, false);
                world.spawnEntity(firework);
            }
        }
    }

    private ItemStack createWinnerFirework(WinningSide winningSide) {
        ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET);
        IntList colors = winningSide == WinningSide.RUNNERS
                ? IntList.of(0x55D6FF, 0xFFFFFF, 0x7FC2FF)
                : IntList.of(0xFF4D4D, 0xFFD966, 0xFFFFFF);
        IntList fadeColors = winningSide == WinningSide.RUNNERS
                ? IntList.of(0x2F80FF, 0xFFFFFF)
                : IntList.of(0xFF9F3F, 0xFFFFFF);
        FireworkExplosionComponent explosion = new FireworkExplosionComponent(
                FireworkExplosionComponent.Type.STAR,
                colors,
                fadeColors,
                true,
                true
        );
        stack.set(DataComponentTypes.FIREWORKS, new FireworksComponent(1, List.of(explosion)));
        return stack;
    }

    private String compactEndingReason(String reason) {
        String key = specKey(reason);
        if (key.equals(HunterWildcardText.key("msg.win.runner.dragon"))) {
            return HunterWildcardText.spec("hud.feedback.reason.dragon");
        }
        if (key.equals(HunterWildcardText.key("msg.win.runner.survive_time"))) {
            return HunterWildcardText.spec("hud.feedback.reason.survive_time");
        }
        if (key.equals(HunterWildcardText.key("msg.win.runner.reach_location"))) {
            return HunterWildcardText.spec("hud.feedback.reason.reach_location");
        }
        if (key.equals(HunterWildcardText.key("msg.win.runner.collect_item"))) {
            return HunterWildcardText.spec("hud.feedback.reason.collect_item");
        }
        if (key.equals(HunterWildcardText.key("msg.win.hunter.kill_target"))) {
            return HunterWildcardText.spec("hud.feedback.reason.kill_target_complete");
        }
        if (key.equals(HunterWildcardText.key("msg.win.hunter.all_runners_out"))) {
            return HunterWildcardText.spec("hud.feedback.reason.all_runners_out");
        }
        if (key.equals(HunterWildcardText.key("msg.win.no_online_runners"))) {
            return HunterWildcardText.spec("hud.feedback.reason.runners_offline");
        }
        if (key.equals(HunterWildcardText.key("msg.win.no_online_hunters"))) {
            return HunterWildcardText.spec("hud.feedback.reason.hunters_offline");
        }

        return reason == null ? "" : reason;
    }

    private String specKey(String spec) {
        if (spec == null || spec.isBlank()) {
            return "";
        }

        int separator = spec.indexOf(HunterWildcardText.SPEC_SEPARATOR);
        return separator < 0 ? spec : spec.substring(0, separator);
    }

    private void cleanupAndReset(MinecraftServer cleanupServer) {
        MinecraftServer targetServer = cleanupServer != null ? cleanupServer : server;
        if (targetServer != null) {
            clearRoundEffects(debugContext(targetServer));
            restoreLocatorBarRule(targetServer);
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
        hunterBoundaryManager.clear();
        winConditionManager.clear();
        preservedDeathInventories.clear();
        previousLocatorBarRule = null;
    }

    private void clearRoundEffects(GameContext context) {
        wildcardManager.clear(context);
        bossBarManager.clear();
        hunterBoundaryManager.clear();
        winConditionManager.clear(context);
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

    private GameContext wildcardEventContext(ServerPlayerEntity player) {
        if (state == GameState.RUNNING && server != null) {
            return context();
        }

        if (!wildcardManager.hasRuleInProgress()) {
            return null;
        }

        MinecraftServer targetServer = player.getEntityWorld().getServer();
        if (targetServer == null) {
            return null;
        }

        server = targetServer;
        return debugContext(targetServer);
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

    private void disableLocatorBarForRound(MinecraftServer targetServer) {
        if (targetServer == null) {
            return;
        }

        if (previousLocatorBarRule == null) {
            previousLocatorBarRule = targetServer.getOverworld().getGameRules().getValue(GameRules.LOCATOR_BAR);
        }
        targetServer.getOverworld().getGameRules().setValue(GameRules.LOCATOR_BAR, false, targetServer);
    }

    private void restoreLocatorBarRule(MinecraftServer targetServer) {
        if (targetServer == null || previousLocatorBarRule == null) {
            return;
        }

        targetServer.getOverworld().getGameRules().setValue(GameRules.LOCATOR_BAR, previousLocatorBarRule, targetServer);
    }

    private enum WinningSide {
        HUNTERS,
        RUNNERS,
        NONE
    }
}
