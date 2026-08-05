package dev.vryxon.simplechestlocker;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.vryxon.simplechestlocker.commands.*;
import dev.vryxon.simplechestlocker.gui.SCLMenuOpener;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public class CommandHandler {
    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    Commands.literal("scl")
                            .then(Commands.literal("version")
                                    .executes(InfoCommands::handleVersion))

                            .then(Commands.literal("help")
                                    .executes(InfoCommands::handleHelp))

                            .then(Commands.literal("lock")
                                    .executes(LockCommands::handleLock))

                            .then(Commands.literal("unlock")
                                    .executes(LockCommands::handleUnlock)
                                    .then(Commands.argument("target", StringArgumentType.greedyString())
                                            .suggests(CommandHelper::suggestTargets)
                                            .executes(LockCommands::handleUnlock)))

                            .then(Commands.literal("label")
                                    .then(Commands.argument("label", StringArgumentType.word())
                                            .executes(LabelCommands::handleLabel)
                                            .then(Commands.argument("target", StringArgumentType.word())
                                                    .suggests(CommandHelper::suggestTargets)
                                                    .executes(LabelCommands::handleLabelTargeted))))

                            .then(Commands.literal("rename")
                                    .then(Commands.argument("label", StringArgumentType.word())
                                            .executes(LabelCommands::handleRename)
                                            .then(Commands.argument("target", StringArgumentType.word())
                                                    .suggests(CommandHelper::suggestTargets)
                                                    .executes(LabelCommands::handleRenameTargeted))))

                            .then(Commands.literal("unlabel")
                                    .executes(LabelCommands::handleUnlabel)
                                    .then(Commands.argument("target", StringArgumentType.greedyString())
                                            .suggests(CommandHelper::suggestTargets)
                                            .executes(LabelCommands::handleUnlabel)))

                            .then(Commands.literal("trust")
                                    .then(Commands.argument("player", StringArgumentType.word())
                                            .suggests(CommandHelper::suggestOnlinePlayers)
                                            .executes(TrustCommands::handleTrust)
                                            .then(Commands.argument("label", StringArgumentType.greedyString())
                                                    .suggests(CommandHelper::suggestTargets)
                                                    .executes(TrustCommands::handleTrustLabel))))

                            .then(Commands.literal("untrust")
                                    .then(Commands.argument("player", StringArgumentType.word())
                                            .suggests(CommandHelper::suggestOnlinePlayers)
                                            .executes(TrustCommands::handleUntrust)
                                            .then(Commands.argument("label", StringArgumentType.greedyString())
                                                    .suggests(CommandHelper::suggestTargets)
                                                    .executes(TrustCommands::handleUntrustLabel))))

                            .then(Commands.literal("players")
                                    .executes(InfoCommands::handlePlayers)
                                    .then(Commands.argument("label", StringArgumentType.greedyString())
                                            .suggests(CommandHelper::suggestTargets)
                                            .executes(InfoCommands::handlePlayersLabel)))

                            .then(Commands.literal("list")
                                    .executes(InfoCommands::handleList)
                                    .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                            .executes(InfoCommands::handleList)))

                            .then(Commands.literal("gui")
                                    .executes(CommandHandler::openGui))
            );
        });
    }

    private static int openGui(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;
        SCLMenuOpener.get(player).openMainMenu();
        return 1;
    }
}