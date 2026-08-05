package dev.vryxon.simplechestlocker.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.vryxon.simplechestlocker.LockManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

import static dev.vryxon.simplechestlocker.helpers.MsgHelper.sendMsg;

public final class LabelCommands {
    private LabelCommands() {}

    public static int handleLabel(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        ServerLevel world = player.level().getLevel();

        String name = StringArgumentType.getString(ctx, "label").trim();

        if (!validateLabel(player, name)) {
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

        if (LockManager.hasLabel(world, owner, pos, name)) {
            sendMsg(player, "§cYou already have another container with that label!");
            return 0;
        }

        LockManager.setLabel(world, owner, pos, name);
        sendMsg(player, "§aContainer labeled §e" + name);
        return 1;
    }

    public static int handleLabelTargeted(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        String name = StringArgumentType.getString(ctx, "label").trim();
        String targetToken = StringArgumentType.getString(ctx, "target").trim();
        return labelTargeted(player, targetToken, name);
    }

    public static int handleUnlabel(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        ServerLevel world = player.level().getLevel();
        BlockPos pos = CommandHelper.getTargetedContainerPos(player);

        try {
            String target = StringArgumentType.getString(ctx, "target").trim();
            if (!target.isEmpty()) {
                GlobalPos globalPos = CommandHelper.resolveTarget(player, target);
                if (globalPos == null) {
                    sendMsg(player, "§cNo container found for §e" + target + "§c!");
                    return 0;
                }
                ServerLevel targetWorld = player.level().getServer().getLevel(globalPos.dimension());
                if (targetWorld == null) {
                    sendMsg(player, "§cThat container's dimension isn't loaded!");
                    return 0;
                }
                world = targetWorld;
                pos = globalPos.pos();
            }
        } catch (IllegalArgumentException ignored) {
            // no target argument provided
        }

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

        String name = StringArgumentType.getString(ctx, "label").trim();

        if (!validateLabel(player, name)) {
            return 0;
        }

        BlockPos pos = CommandHelper.getTargetedContainerPos(player);

        if (!CommandHelper.requireTargetedContainer(player, pos)) {
            return 0;
        }
        if (!CommandHelper.requireOwnership(player, world, pos)) {
            return 0;
        }

        return renameTargeted(player, world, pos, name);
    }

    public static int handleRenameTargeted(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        String name = StringArgumentType.getString(ctx, "label").trim();
        String targetToken = StringArgumentType.getString(ctx, "target").trim();
        return renameTargeted(player, targetToken, name);
    }

    private static int labelTargeted(ServerPlayer player, String targetToken, String label) {
        ServerLevel world = player.level().getLevel();
        GlobalPos globalPos = CommandHelper.resolveTarget(player, targetToken);

        if (globalPos == null) {
            sendMsg(player, "§cNo container found for §e" + targetToken + "§c!");
            return 0;
        }
        if (!validateLabel(player, label)) {
            return 0;
        }

        ServerLevel targetWorld = player.level().getServer().getLevel(globalPos.dimension());
        if (targetWorld == null) {
            sendMsg(player, "§cThat container's dimension isn't loaded!");
            return 0;
        }

        UUID owner = player.getUUID();
        BlockPos pos = globalPos.pos();

        if (!LockManager.isLocked(targetWorld, pos)) {
            sendMsg(player, "§cThat container isn't locked!");
            return 0;
        }
        UUID posOwner = LockManager.getOwner(targetWorld, pos);
        if (!owner.equals(posOwner)) {
            sendMsg(player, "§cYou don't own it!");
            return 0;
        }
        if (LockManager.isLabeled(targetWorld, owner, pos)) {
            sendMsg(player, "§cThis container already has a label! Use §e/scl rename <label>§c.");
            return 0;
        }
        if (LockManager.hasLabel(targetWorld, owner, pos, label)) {
            sendMsg(player, "§cYou already have another container with that label!");
            return 0;
        }

        LockManager.setLabel(targetWorld, owner, pos, label);
        sendMsg(player, "§aContainer labeled §e" + label);
        return 1;
    }

    private static int renameTargeted(ServerPlayer player, String targetToken, String label) {
        ServerLevel world = player.level().getLevel();
        GlobalPos globalPos = CommandHelper.resolveTarget(player, targetToken);

        if (globalPos == null) {
            sendMsg(player, "§cNo container found for §e" + targetToken + "§c!");
            return 0;
        }
        if (!validateLabel(player, label)) {
            return 0;
        }

        ServerLevel targetWorld = player.level().getServer().getLevel(globalPos.dimension());
        if (targetWorld == null) {
            sendMsg(player, "§cThat container's dimension isn't loaded!");
            return 0;
        }

        return renameTargeted(player, targetWorld, globalPos.pos(), label);
    }

    private static int renameTargeted(ServerPlayer player, ServerLevel world, BlockPos pos, String label) {
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

    private static boolean validateLabel(ServerPlayer player, String label) {
        if (label.isEmpty()) {
            sendMsg(player, "§cLabel cannot be empty!");
            return false;
        }
        if (label.chars().anyMatch(Character::isWhitespace)) {
            sendMsg(player, "§cLabel cannot contain spaces!");
            return false;
        }
        if (Character.isDigit(label.charAt(0))) {
            sendMsg(player, "§cLabel cannot start with a number!");
            return false;
        }
        return true;
    }
}
