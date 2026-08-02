package dev.vryxon.simplechestlocker.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class SignInputHandler {

    private record PendingInput(ServerPlayer player, BlockPos pos, SignBlockEntity sign, Consumer<String> callback, long expiry) {}

    private static final Map<UUID, PendingInput> PENDING = new HashMap<>();

    private SignInputHandler() {}

    public static void request(ServerPlayer player, String prompt, Consumer<String> callback) {
        ServerLevel world = player.level().getLevel();
        BlockPos pos = findSignPos(player, world);
        if (pos == null) {
            callback.accept("");
            return;
        }

        BlockState signState = Blocks.OAK_SIGN.defaultBlockState();
        SignBlockEntity sign = new SignBlockEntity(pos, signState);
        SignText initialText = sign.getFrontText()
                .setMessage(0, Component.literal(""))
                .setMessage(1, Component.literal(prompt));

        player.connection.send(new ClientboundBlockUpdatePacket(pos, signState));

        sign.setLevel(world);
        player.connection.send(ClientboundBlockEntityDataPacket.create(sign, (be, registry) -> {
            CompoundTag tag = new CompoundTag();
            RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registry);
            SignText.DIRECT_CODEC.encodeStart(ops, initialText).result()
                    .ifPresent(t -> tag.put("front_text", t));
            return tag;
        }));
        sign.setLevel(null);

        long expiry = System.currentTimeMillis() + 60000;
        PENDING.put(player.getUUID(), new PendingInput(player, pos, sign, callback, expiry));
        player.connection.send(new ClientboundOpenSignEditorPacket(pos, true));
    }

    private static BlockPos findSignPos(ServerPlayer player, ServerLevel world) {
        BlockPos playerPos = player.blockPosition();
        for (int radius = 1; radius <= 5; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
                    for (int dy = -2; dy <= 2; dy++) {
                        BlockPos p = playerPos.offset(dx, dy, dz);
                        if (!world.getBlockState(p).isAir() || !world.getBlockState(p.above()).isAir()) continue;
                        if (!player.isWithinBlockInteractionRange(p, 4.0)) continue;
                        return p;
                    }
                }
            }
        }

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        BlockPos fallback = BlockPos.containing(eye.x - look.x * 3.0, eye.y - look.y * 3.0, eye.z - look.z * 3.0);
        if (player.isWithinBlockInteractionRange(fallback, 4.0)) {
            return fallback;
        }
        return null;
    }

    public static void handleSignUpdate(ServerPlayer player, BlockPos pos, String[] lines) {
        PendingInput input = PENDING.remove(player.getUUID());
        if (input == null) return;
        if (!input.pos().equals(pos)) return;
        if (System.currentTimeMillis() > input.expiry()) return;

        String text = lines.length > 0 ? lines[0].trim() : "";
        input.callback().accept(text);
        restoreClientBlock(player, pos);
    }

    public static boolean isPending(ServerPlayer player, BlockPos pos) {
        PendingInput input = PENDING.get(player.getUUID());
        return input != null && input.pos().equals(pos);
    }

    public static void cleanup(ServerPlayer player) {
        PENDING.remove(player.getUUID());
    }

    public static void tickCleanup() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, PendingInput>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, PendingInput> entry = it.next();
            PendingInput input = entry.getValue();
            if (now > input.expiry()) {
                it.remove();
                restoreClientBlock(input.player(), input.pos());
            }
        }
    }

    private static void restoreClientBlock(ServerPlayer player, BlockPos pos) {
        if (player == null || player.connection == null || !player.connection.isAcceptingMessages()) return;
        ServerLevel world = player.level().getLevel();
        player.connection.send(new ClientboundBlockUpdatePacket(world, pos));
    }
}
