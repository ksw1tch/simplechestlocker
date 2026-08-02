package dev.vryxon.simplechestlocker;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;

import java.util.UUID;

public class LockEvents {

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClientSide() || !(world instanceof ServerLevel serverWorld)) return InteractionResult.PASS;

            BlockPos clickPos = hit.getBlockPos();
            String playerName = player.getName().getString();

            if (!LockManager.isAnyLocked(serverWorld, clickPos)) {
                return InteractionResult.PASS;
            }

            UUID owner = LockManager.getOwner(serverWorld, clickPos);
            if (owner == null) {
                return InteractionResult.PASS;
            }

            if (!owner.equals(player.getUUID()) &&
                    !LockManager.isTrusted(serverWorld, owner, clickPos, playerName)) {
                player.displayClientMessage(Component.literal("§cThis container is locked!"), true);
                return InteractionResult.FAIL;
            }

            return InteractionResult.PASS;
        });

        PlayerBlockBreakEvents.BEFORE.register((world, player, breakPos, state, blockEntity) -> {
            if (world.isClientSide() || !(world instanceof ServerLevel serverWorld)) return true;

            if (!LockManager.isAnyLocked(serverWorld, breakPos)) {
                return true;
            }

            UUID owner = LockManager.getOwner(serverWorld, breakPos);
            if (owner == null) {
                return true;
            }

            UUID playerId = player.getUUID();

            if (!owner.equals(playerId)) {
                player.displayClientMessage(Component.literal("§cYou cannot break a locked container!"), true);
                return false;
            }

            LockManager.removeLock(serverWorld, breakPos);
            return true;
        });
    }

    public static boolean isInventoryLocked(ServerLevel world, BlockPos pos) {
        if (!LockManager.isLocked(world, pos)) return false;
        return world.getBlockEntity(pos) instanceof Container;
    }
}