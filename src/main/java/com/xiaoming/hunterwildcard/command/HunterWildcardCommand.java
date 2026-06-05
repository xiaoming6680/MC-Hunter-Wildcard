package com.xiaoming.hunterwildcard.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.xiaoming.hunterwildcard.game.GameManager;
import com.xiaoming.hunterwildcard.network.HunterWildcardPackets;
import com.xiaoming.hunterwildcard.team.PlayerRole;
import com.xiaoming.hunterwildcard.wildcard.WildcardManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class HunterWildcardCommand {
    private static final Permission OP_LEVEL_TWO = new Permission.Level(PermissionLevel.GAMEMASTERS);

    private HunterWildcardCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("hw")
                .then(CommandManager.literal("start")
                        .requires(HunterWildcardCommand::canManageGame)
                        .executes(context -> {
                            GameManager.getInstance().start(context.getSource());
                            HunterWildcardPackets.syncAll(context.getSource().getServer());
                            return 1;
                        }))
                .then(CommandManager.literal("stop")
                        .requires(HunterWildcardCommand::canManageGame)
                        .executes(context -> {
                            GameManager.getInstance().stop(context.getSource());
                            HunterWildcardPackets.syncAll(context.getSource().getServer());
                            return 1;
                        }))
                .then(CommandManager.literal("config")
                        .then(CommandManager.literal("reload")
                                .requires(HunterWildcardCommand::canManageGame)
                                .executes(context -> {
                                    GameManager.getInstance().reloadConfig(context.getSource());
                                    HunterWildcardPackets.syncAll(context.getSource().getServer());
                                    return 1;
                                }))
                        .then(CommandManager.literal("save")
                                .requires(HunterWildcardCommand::canManageGame)
                                .executes(context -> {
                                    GameManager.getInstance().saveConfig(context.getSource());
                                    HunterWildcardPackets.syncAll(context.getSource().getServer());
                                    return 1;
                                })))
                .then(CommandManager.literal("wildcard")
                        .then(CommandManager.literal("roll")
                                .requires(HunterWildcardCommand::canManageGame)
                                .executes(context -> {
                                    GameManager.getInstance().rollWildcard(context.getSource());
                                    HunterWildcardPackets.syncAll(context.getSource().getServer());
                                    return 1;
                                }))
                        .then(CommandManager.literal("stop")
                                .requires(HunterWildcardCommand::canManageGame)
                                .executes(context -> {
                                    GameManager.getInstance().stopWildcard(context.getSource());
                                    HunterWildcardPackets.syncAll(context.getSource().getServer());
                                    return 1;
                                }))
                        .then(CommandManager.literal("list")
                                .executes(context -> listWildcards(context.getSource()))))
                .then(CommandManager.literal("ts")
                        .requires(HunterWildcardCommand::canManageGame)
                        .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                    boolean enabled = BoolArgumentType.getBool(context, "enabled");
                                    GameManager.getInstance().setDebugMenuEnabled(player, enabled);
                                    HunterWildcardPackets.sendSync(player);
                                    context.getSource().sendFeedback(() -> Text.literal(enabled
                                            ? "已打开猎人外卡调试页。"
                                            : "已关闭猎人外卡调试页。"), false);
                                    return 1;
                                })))
                .then(CommandManager.literal("join")
                        .then(CommandManager.literal("hunter")
                                .executes(context -> join(context.getSource(), PlayerRole.HUNTER)))
                        .then(CommandManager.literal("runner")
                                .executes(context -> join(context.getSource(), PlayerRole.RUNNER))))
                .then(CommandManager.literal("leave")
                        .executes(context -> {
                            GameManager.getInstance().leave(context.getSource().getPlayerOrThrow());
                            HunterWildcardPackets.syncAll(context.getSource().getServer());
                            return 1;
                        }))
                .then(CommandManager.literal("status")
                        .executes(context -> {
                            context.getSource().sendFeedback(() -> Text.literal(GameManager.getInstance().getStatusText()), false);
                            return 1;
                        })));
    }

    private static int join(ServerCommandSource source, PlayerRole role) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        GameManager.getInstance().join(player, role);
        HunterWildcardPackets.syncAll(source.getServer());
        return 1;
    }

    private static int listWildcards(ServerCommandSource source) {
        GameManager manager = GameManager.getInstance();
        source.sendFeedback(() -> Text.literal("猎人外卡列表:"), false);
        for (WildcardManager.WildcardStatus status : manager.getWildcardManager().getRuleStatuses(manager.getConfig())) {
            String state = status.enabled() ? "开启" : "关闭";
            source.sendFeedback(() -> Text.literal("- " + status.name() + ": " + state), false);
        }
        return 1;
    }

    public static boolean canManageGame(ServerCommandSource source) {
        return source.getPermissions().hasPermission(OP_LEVEL_TWO);
    }
}
