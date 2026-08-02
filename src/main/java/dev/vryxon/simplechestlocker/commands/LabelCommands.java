package dev.vryxon.simplechestlocker.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.vryxon.simplechestlocker.LockManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

import static dev.vryxon.simplechestlocker.helpers.MsgHelper.sendMsg;

public final class LabelCommands {
    private LabelCommands() {}

    public static int handleLabel(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        ServerLevel world = player.level().getLevel();

        String label = StringArgumentType.getString(ctx, "label").trim();

        if (label.isEmpty()) {
            sendMsg(player, "§cLabel cannot be empty!");
            return 0;
        }

        BlockPos pos = CommandHelper.getTargetedContainerPos(player);

        if (!CommandHelper.requireTargetedContainer(player, pos)) {
            return 0;
        }
        if (!CommandHelper.requireOwnership(player, world, pos)) {
            return 0;
        }

        UUID owner = player.getUUID();

        if (LockManager.isLabeled(world, owner, pos)) {
            sendMsg(player, "§cThis container already has a label! Use §e/scl rename <label>§c.");
            return 0;
        }

        if (LockManager.hasLabel(world, owner, pos, label)) {
            sendMsg(player, "§cYou already have another container with that label!");
            return 0;
        }

        LockManager.setLabel(world, owner, pos, label);
        sendMsg(player, "§aContainer labeled §e" + label);
        return 1;
    }

    public static int handleUnlabel(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        ServerLevel world = player.level().getLevel();

        BlockPos pos = CommandHelper.getTargetedContainerPos(player);

        if (!CommandHelper.requireTargetedContainer(player, pos)) {
            return 0;
        }
        if (!CommandHelper.requireOwnership(player, world, pos)) {
            return 0;
        }

        UUID owner = player.getUUID();

        if (!LockManager.isLabeled(world, owner, pos)) {
            sendMsg(player, "§cThis container doesn't have a label!");
            return 0;
        }

        String label = LockManager.getLabel(world, owner, pos);

        LockManager.removeLabel(world, owner, pos);

        sendMsg(player, "§aRemoved label §e" + label);
        return 1;
    }

    public static int handleRename(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        ServerLevel world = player.level().getLevel();

        String label = StringArgumentType.getString(ctx, "label").trim();

        if (label.isEmpty()) {
            sendMsg(player, "§cLabel cannot be empty!");
            return 0;
        }

        BlockPos pos = CommandHelper.getTargetedContainerPos(player);

        if (!CommandHelper.requireTargetedContainer(player, pos)) {
            return 0;
        }
        if (!CommandHelper.requireOwnership(player, world, pos)) {
            return 0;
        }

        UUID owner = player.getUUID();

        if (!LockManager.isLabeled(world, owner, pos)) {
            sendMsg(player, "§cThis container doesn't have a label! Use §e/scl label <label>§c first.");
            return 0;
        }

        if (LockManager.hasLabel(world, owner, pos, label)) {
            sendMsg(player, "§cYou already have another container with that label!");
            return 0;
        }

        String oldLabel = LockManager.getLabel(world, owner, pos);

        if (oldLabel.equalsIgnoreCase(label)) {
            sendMsg(player, "§eThat is already the current label.");
            return 0;
        }

        LockManager.setLabel(world, owner, pos, label);

        sendMsg(player, "§aRenamed container from §e" + oldLabel + "§a to §e" + label);
        return 1;
    }
}