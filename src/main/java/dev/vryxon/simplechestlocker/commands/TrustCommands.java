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

public final class TrustCommands {
    private TrustCommands() {}

    public static int handleTrust(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        String target = StringArgumentType.getString(ctx, "player");

        ServerLevel world = player.level().getLevel();
        return trustPlayer(player, world, target, CommandHelper.getTargetedContainerPos(player), null);
    }

    public static int handleTrustLabel(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        String target = StringArgumentType.getString(ctx, "player");
        String label = StringArgumentType.getString(ctx, "label");

        GlobalPos globalPos = CommandHelper.resolveTarget(player, label);

        if (globalPos == null) {
            sendMsg(player, "§cNo container found for §e" + label + "§c!");
            return 0;
        }

        return trustPlayerGlobal(player, target, globalPos, label);
    }

    public static int handleUntrust(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        String target = StringArgumentType.getString(ctx, "player");

        ServerLevel world = player.level().getLevel();
        return untrustPlayer(player, world, target, CommandHelper.getTargetedContainerPos(player), null);
    }

    public static int handleUntrustLabel(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        String target = StringArgumentType.getString(ctx, "player");
        String label = StringArgumentType.getString(ctx, "label");

        GlobalPos globalPos = CommandHelper.resolveTarget(player, label);

        if (globalPos == null) {
            sendMsg(player, "§cNo container found for §e" + label + "§c!");
            return 0;
        }

        return untrustPlayerGlobal(player, target, globalPos, label);
    }

    private static int trustPlayerGlobal(ServerPlayer player, String target, GlobalPos globalPos, String label) {
        ServerLevel world = player.level().getServer().getLevel(globalPos.dimension());
        if (world == null) {
            sendMsg(player, "§cThat container's dimension isn't loaded!");
            return 0;
        }
        return trustPlayer(player, world, target, globalPos.pos(), label);
    }

    private static int untrustPlayerGlobal(ServerPlayer player, String target, GlobalPos globalPos, String label) {
        ServerLevel world = player.level().getServer().getLevel(globalPos.dimension());
        if (world == null) {
            sendMsg(player, "§cThat container's dimension isn't loaded!");
            return 0;
        }
        return untrustPlayer(player, world, target, globalPos.pos(), label);
    }

    private static int trustPlayer(ServerPlayer player, ServerLevel world, String target, BlockPos pos, String label) {
        if (pos == null) {
            sendMsg(player, "§cNo container found!");
            return 0;
        }

        if (!LockManager.isAnyLocked(world, pos)) {
            sendMsg(player, "§cThat container isn't locked!");
            return 0;
        }

        UUID owner = LockManager.getOwner(world, pos);

        if (!owner.equals(player.getUUID())) {
            sendMsg(player, "§cYou don't own it!");
            return 0;
        }

        if (LockManager.isTrusted(world, owner, pos, target)) {
            sendMsg(player, "§e" + target + " is already trusted.");
            return 0;
        }

        LockManager.addTrusted(world, owner, pos, target);
        if (label != null) {
            sendMsg(player, "§aTrusted §e" + target + "§a with §e" + label);
        } else {
            sendMsg(player, "§aTrusted §e" + target);
        }
        return 1;
    }

    private static int untrustPlayer(ServerPlayer player, ServerLevel world, String target, BlockPos pos, String label) {
        if (pos == null) {
            sendMsg(player, "§cNo container found!");
            return 0;
        }

        if (!LockManager.isAnyLocked(world, pos)) {
            sendMsg(player, "§cThat container isn't locked!");
            return 0;
        }

        UUID owner = LockManager.getOwner(world, pos);

        if (!owner.equals(player.getUUID())) {
            sendMsg(player, "§cYou don't own it!");
            return 0;
        }

        if (!LockManager.isTrusted(world, owner, pos, target)) {
            sendMsg(player, "§e" + target + " isn't trusted.");
            return 0;
        }

        LockManager.removeTrusted(world, owner, pos, target);
        if (label != null) {
            sendMsg(player, "§aUntrusted §e" + target + "§a with §e" + label);
        } else {
            sendMsg(player, "§aUntrusted §e" + target);
        }
        return 1;
    }
}