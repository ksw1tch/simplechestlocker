package dev.vryxon.simplechestlocker.networking;

import dev.vryxon.simplechestlocker.LockManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class LockOverlayServer {

    private LockOverlayServer() {}

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(LockOverlayPayloads.ServerboundLockQuery.TYPE,
                LockOverlayPayloads.ServerboundLockQuery.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LockOverlayPayloads.ClientboundLockInfo.TYPE,
                LockOverlayPayloads.ClientboundLockInfo.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LockOverlayPayloads.ClientboundLockInvalidate.TYPE,
                LockOverlayPayloads.ClientboundLockInvalidate.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(LockOverlayPayloads.ServerboundLockQuery.TYPE,
                (payload, context) -> {
                    ServerPlayer player = context.player();
                    BlockPos pos = payload.pos();
                    ServerLevel world = player.level().getLevel();

                    boolean locked = LockManager.isAnyLocked(world, pos);
                    String ownerName = "";
                    String label = "";
                    List<String> trusted = new ArrayList<>();

                    if (locked) {
                        UUID owner = LockManager.getOwner(world, pos);
                        if (owner != null) {
                            var onlineOwner = player.level().getServer().getPlayerList().getPlayer(owner);
                            if (onlineOwner != null) {
                                ownerName = onlineOwner.getName().getString();
                            } else {
                                ownerName = owner.toString();
                            }
                            label = LockManager.getLabel(world, owner, pos);
                            if (label == null) label = "";
                            trusted = new ArrayList<>(LockManager.getTrusted(world, owner, pos));
                        }
                    }

                    LockOverlayPayloads.ClientboundLockInfo info =
                            new LockOverlayPayloads.ClientboundLockInfo(pos, locked, ownerName, label, trusted);
                    ServerPlayNetworking.send(player, info);
                });
    }

    public static void broadcastInvalidate(ServerLevel world, BlockPos pos) {
        LockOverlayPayloads.ClientboundLockInvalidate packet =
                new LockOverlayPayloads.ClientboundLockInvalidate(pos);
        for (ServerPlayer p : world.players()) {
            ServerPlayNetworking.send(p, packet);
        }
    }
}
