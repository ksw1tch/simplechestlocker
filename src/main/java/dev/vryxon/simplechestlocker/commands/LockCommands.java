package dev.vryxon.simplechestlocker.commands;

import com.mojang.brigadier.context.CommandContext;
import dev.vryxon.simplechestlocker.LockManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import static dev.vryxon.simplechestlocker.helpers.MsgHelper.sendMsg;

public final class LockCommands {
    private LockCommands() {}

    public static int handleLock(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        BlockPos pos = CommandHelper.getTargetedContainerPos(player);
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

    public static int handleUnlock(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        ServerLevel world = player.level().getLevel();
        BlockPos pos = CommandHelper.getTargetedContainerPos(player);

        if (!CommandHelper.requireTargetedContainer(player, pos)) {
            return 0;
        }
        if (!CommandHelper.requireOwnership(player, world, pos)) {
            return 0;
        }

        LockManager.removeLock(world, pos);
        sendMsg(player, "§aContainer unlocked.");
        return 1;
    }
}