package dev.vryxon.simplechestlocker;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import static dev.vryxon.simplechestlocker.helpers.MsgHelper.sendMsg;
import static dev.vryxon.simplechestlocker.helpers.MsgHelper.sendMsgS;

public class CommandHandler {
    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    Commands.literal("scl")
                            .then(Commands.literal("version")
                                    .executes(CommandHandler::version))

                            .then(Commands.literal("lock")
                                    .executes(CommandHandler::handleLock))

                            .then(Commands.literal("unlock")
                                    .executes(CommandHandler::handleUnlock))

                            .then(Commands.literal("trust")
                                    .then(Commands.argument("player", StringArgumentType.word())
                                            .suggests(CommandHandler::suggestOnlinePlayers)
                                            .executes(CommandHandler::handleTrust)))

                            .then(Commands.literal("untrust")
                                    .then(Commands.argument("player", StringArgumentType.word())
                                            .suggests(CommandHandler::suggestOnlinePlayers)
                                            .executes(CommandHandler::handleUntrust)))

                            .then(Commands.literal("list")
                                    .executes(CommandHandler::handleList))
            );
        });
    }

    private static int version(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        sendMsgS(player, "\u00A77SimpleChestLocker version: \u00A7r" + SimpleChestLocker.ver);
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestOnlinePlayers(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
                ctx.getSource().getServer().getPlayerList().getPlayerNamesArray(),
                builder
        );
    }

    private static BlockPos getTargetedContainerPos(ServerPlayer player) {
        HitResult hit = player.pick(6.0, player.getYRot(), false);
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = ((BlockHitResult) hit).getBlockPos();
            if (LockableContainers.isLockable(player.level(), pos)) {
                return pos;
            }
        }
        return null;
    }

    private static int handleLock(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        BlockPos pos = getTargetedContainerPos(player);
        ServerLevel world = player.level().getLevel();
        if (pos == null) {
            sendMsg(player, "§cNo container in sight!");
            return 0;
        }

        if (LockManager.lockChest(world, player.getUUID(), pos)) {
            sendMsg(player, "§aContainer locked!");
            return 1;
        } else {
            sendMsg(player, "§cAlready locked!");
            return 0;
        }
    }

    private static int handleUnlock(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        ServerLevel world = player.level().getLevel();
        BlockPos pos = getTargetedContainerPos(player);
        if (pos == null) {
            sendMsg(player, "§cNo container in sight!");
            return 0;
        }

        if (!LockManager.isLocked(world, pos)) {
            sendMsg(player, "§cThis container isn't locked!");
            return 0;
        }

        UUID owner = LockManager.getOwner(world, pos);
        if (!owner.equals(player.getUUID())) {
            sendMsg(player, "§cYou don't own it!");
            return 0;
        }

        LockManager.removeLock(world, pos);
        sendMsg(player, "§aContainer unlocked.");
        return 1;
    }

    private static int handleTrust(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        String target = StringArgumentType.getString(ctx, "player");
        ServerLevel world = player.level().getLevel();
        BlockPos pos = getTargetedContainerPos(player);
        if (pos == null) {
            sendMsg(player, "§cNo container in sight!");
            return 0;
        }

        if (!LockManager.isLocked(world, pos)) {
            sendMsg(player, "§cNot locked!");
            return 0;
        }

        UUID owner = LockManager.getOwner(world, pos);
        if (!owner.equals(player.getUUID())) {
            sendMsg(player, "§cYou don't own it!");
            return 0;
        }

        LockManager.addTrusted(world, owner, pos, target);
        sendMsg(player, "§aTrusted §e" + target);
        return 1;
    }

    private static int handleUntrust(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        String target = StringArgumentType.getString(ctx, "player");
        ServerLevel world = player.level().getLevel();
        BlockPos pos = getTargetedContainerPos(player);
        if (pos == null) {
            sendMsg(player, "§cNo container in sight!");
            return 0;
        }

        if (!LockManager.isLocked(world, pos)) {
            sendMsg(player, "§cNot locked!");
            return 0;
        }

        UUID owner = LockManager.getOwner(world, pos);
        if (!owner.equals(player.getUUID())) {
            sendMsg(player, "§cYou don't own it!");
            return 0;
        }

        LockManager.removeTrusted(world, owner, pos, target);
        sendMsg(player, "§aUntrusted §e" + target);
        return 1;
    }

    private static int handleList(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        ServerLevel world = player.level().getLevel();
        BlockPos pos = getTargetedContainerPos(player);
        if (pos == null) {
            sendMsg(player, "§cNo container in sight!");
            return 0;
        }

        if (!LockManager.isLocked(world, pos)) {
            sendMsg(player, "§cNot locked!");
            return 0;
        }

        UUID owner = LockManager.getOwner(world, pos);
        if (!owner.equals(player.getUUID())) {
            sendMsg(player, "§cYou don't own it!");
            return 0;
        }

        Set<String> trusted = LockManager.getTrusted(world, owner, pos);
        if (trusted.isEmpty()) {
            sendMsg(player, "§eNo trusted players.");
        } else {
            sendMsg(player, "§aTrusted: §e" + String.join(", ", trusted));
        }
        return 1;
    }
}