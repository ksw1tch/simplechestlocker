package dev.vryxon.simplechestlocker;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
                if (player instanceof ServerPlayer sp) {
                    sp.sendOverlayMessage(Component.literal("This container is locked!").withColor(TextColor.RED));
                }
                return InteractionResult.FAIL;
            }

            return InteractionResult.PASS;
        });

        PlayerBlockBreakEvents.BEFORE.register((world, player, breakPos, state, blockEntity) -> {
            if (world.isClientSide() || !(world instanceof ServerLevel serverWorld)) return true;

            BlockPos[] parts = LockableContainers.resolveLockableBlocks(breakPos, serverWorld);

            if (!LockManager.isAnyLocked(serverWorld, breakPos)) {
                return true;
            }

            UUID owner = LockManager.getOwner(serverWorld, breakPos);
            if (owner == null) {
                return true;
            }

            UUID playerId = player.getUUID();

            if (!owner.equals(playerId)) {
                if (player instanceof ServerPlayer sp) {
                    sp.sendOverlayMessage(Component.literal("You cannot break a locked container!").withColor(TextColor.RED));
                }
                return false;
            }

            try {
                LockManager.migrateLockOnPartRemoved(serverWorld, breakPos, parts);
                LockManager.removeLock(serverWorld, breakPos, false);
            } catch (Throwable t) {
                dev.vryxon.simplechestlocker.SimpleChestLocker.LOGGER.warn(
                        "Failed to update lock data after breaking {}: {}", breakPos, t.toString());
            }
            return true;
        });
    }

    public static boolean isInventoryLocked(ServerLevel world, BlockPos pos) {
        if (!LockManager.isLocked(world, pos)) return false;
        return world.getBlockEntity(pos) instanceof Container;
    }
}