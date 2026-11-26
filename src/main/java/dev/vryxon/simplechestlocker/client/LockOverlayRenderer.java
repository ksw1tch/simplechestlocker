package dev.vryxon.simplechestlocker.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.vryxon.simplechestlocker.SimpleChestLocker;
import dev.vryxon.simplechestlocker.LockableContainers;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class LockOverlayRenderer {

    private LockOverlayRenderer() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            LockClientState.tick();
            prefetchAroundPlayer();
        });
        LevelRenderEvents.COLLECT_SUBMITS.register(ctx -> renderWorld(ctx));
    }

    private static void prefetchAroundPlayer() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        ClientLevel level = mc.level;
        BlockPos playerPos = mc.player.blockPosition();
        int radius = 8;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    BlockPos pos = playerPos.offset(dx, dy, dz);
                    if (!level.isLoaded(pos)) continue;
                    BlockState state = level.getBlockState(pos);
                    Block block = state.getBlock();
                    if (block == Blocks.AIR) continue;
                    if (!LockableContainers.isLockable(level, pos)) continue;
                    LockClientState.request(pos);
                }
            }
        }
    }

    private static final java.util.Set<BlockPos> RENDERED_THIS_FRAME = new java.util.HashSet<>();
    private static final java.util.Map<BlockPos, ItemStackRenderState> STATE_CACHE = new java.util.HashMap<>();

    private static BlockPos canonicalHalf(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return pos;
        BlockPos[] parts = LockableContainers.resolveLockableBlocks(pos, mc.level);
        if (parts.length <= 1) return pos;
        BlockPos canonical = parts[0];
        for (BlockPos part : parts) {
            int cmp = part.getX() != canonical.getX()
                    ? Integer.compare(part.getX(), canonical.getX())
                    : (part.getY() != canonical.getY()
                        ? Integer.compare(part.getY(), canonical.getY())
                        : Integer.compare(part.getZ(), canonical.getZ()));
            if (cmp < 0) canonical = part;
        }
        return canonical;
    }

    private static Direction facingFromBlock(BlockState state) {
        if (state.hasProperty(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING)) {
            try {
                return state.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
            } catch (Throwable ignore) {}
        }
        if (state.hasProperty(net.minecraft.world.level.block.DirectionalBlock.FACING)) {
            try {
                return state.getValue(net.minecraft.world.level.block.DirectionalBlock.FACING);
            } catch (Throwable ignore) {}
        }
        return null;
    }

    private static boolean isLookingAtLock(Minecraft mc, BlockPos canonical, Vec3 iconAnchor) {
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos hitPos = ((BlockHitResult) mc.hitResult).getBlockPos();
            BlockPos[] parts = LockableContainers.resolveLockableBlocks(canonical, mc.level);
            boolean onChest = false;
            for (BlockPos part : parts) {
                if (hitPos.equals(part)) { onChest = true; break; }
            }
            if (!onChest) return false;
        }
        if (mc.player == null) return false;
        Vec3 eye = mc.gameRenderer.mainCamera().position();
        Vec3 look = mc.player.getViewVector(1.0F);
        Vec3 toIcon = iconAnchor.subtract(eye);
        double dist = toIcon.length();
        if (dist < 1.0e-3) return true;
        double cos = Math.max(-1.0, Math.min(1.0, toIcon.normalize().dot(look)));
        double angle = Math.acos(cos);
        return angle <= Math.atan(0.35 / dist);
    }

    private static int chestLight(Minecraft mc, BlockPos pos) {
        if (mc.level == null) return 0xF000F0;
        try {
            int emission = mc.level.getBlockState(pos).getLightEmission();
            int light = mc.level.getRawBrightness(pos, emission);
            if (light == 0) return 0xF000F0;
            return light;
        } catch (Throwable ignore) {
            return 0xF000F0;
        }
    }

    private static void renderWorld(net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        SubmitNodeCollector collector = ctx.submitNodeCollector();
        var cameraState = ctx.levelState().cameraRenderState;
        if (cameraState == null) return;
        Vec3 camPos = cameraState.pos != null ? cameraState.pos : mc.gameRenderer.mainCamera().position();

        RENDERED_THIS_FRAME.clear();
        ItemModelResolver resolver = mc.getItemModelResolver();

        java.util.Map<BlockPos, ItemStackRenderState> fresh = new java.util.HashMap<>();

        for (java.util.Map.Entry<BlockPos, LockClientState.Info> entry : LockClientState.snapshot().entrySet()) {
            BlockPos pos = entry.getKey();
            LockClientState.Info info = entry.getValue();
            if (info == null || !info.locked()) continue;

            BlockPos canonical = canonicalHalf(pos);
            if (!RENDERED_THIS_FRAME.add(canonical)) continue;

            if (!LockableContainers.isLockable(mc.level, canonical)) {
                LockClientState.invalidate(pos);
                continue;
            }

            BlockPos[] parts = LockableContainers.resolveLockableBlocks(canonical, mc.level);
            double cx, cy, cz;
            if (parts.length > 1) {
                cx = 0; cz = 0;
                for (BlockPos part : parts) {
                    cx += part.getX() + 0.5;
                    cz += part.getZ() + 0.5;
                }
                cx /= parts.length;
                cz /= parts.length;
                cy = parts[0].getY() + 0.5;
            } else {
                cx = canonical.getX() + 0.5;
                cy = canonical.getY() + 0.5;
                cz = canonical.getZ() + 0.5;
            }
            Vec3 blockCenter = new Vec3(cx, cy, cz);

            BlockState state = mc.level.getBlockState(canonical);
            Direction facing = facingFromBlock(state);

            Vec3 faceNormal;
            float yawDeg = 0f;
            if (facing != null && facing != Direction.UP && facing != Direction.DOWN) {
                faceNormal = new Vec3(facing.getStepX(), 0, facing.getStepZ());
                yawDeg = (float) Math.toDegrees(Math.atan2(faceNormal.x, faceNormal.z));
            } else {
                faceNormal = new Vec3(0, 0, 1);
                yawDeg = 0f;
            }

            Vec3 anchor = blockCenter
                    .add(faceNormal.scale(0.5))
                    .add(0, -0.05, 0);

            ItemStackRenderState state2 = STATE_CACHE.get(canonical);
            if (state2 == null) {
                state2 = fresh.computeIfAbsent(canonical, k -> new ItemStackRenderState());
            } else {
                state2.clear();
                fresh.put(canonical, state2);
            }

            ItemStack stack = new ItemStack(
                    info.locked() && LockClientState.isUnlockedForLocalPlayer(info)
                            ? LockVisuals.UNLOCKED
                            : LockVisuals.LOCKED
            );
            resolver.updateForNonLiving(state2, stack, ItemDisplayContext.FIXED, mc.player);

            PoseStack mtx = new PoseStack();
            mtx.pushPose();
            double anchorX = anchor.x - camPos.x;
            double anchorY = anchor.y - camPos.y;
            double anchorZ = anchor.z - camPos.z;
            mtx.translate(anchorX, anchorY, anchorZ);
            mtx.rotateDegrees(com.mojang.math.Axis.YP, yawDeg);
            float iconScale = 0.50f;
            mtx.translate(0, 0, 0.5f * iconScale);
            mtx.scale(iconScale, iconScale, iconScale);
            int light = 0xF000F0;
            try {
                state2.submit(mtx, collector, light, OverlayTexture.NO_OVERLAY, 0);
            } catch (Throwable t) {
                SimpleChestLocker.LOGGER.warn("Failed to submit lock icon for {}: {}", canonical, t.toString());
            }
            mtx.popPose();

            if (!info.label().isEmpty()
                    && LockClientState.isUnlockedForLocalPlayer(info)
                    && isLookingAtLock(mc, canonical, anchor)) {
                Vec3 labelAnchor = blockCenter
                        .add(faceNormal.scale(0.7))
                        .add(0, -0.30, 0);
                try {
                    ComponentHolder.submitLabel(collector, labelAnchor, info.label(), cameraState, camPos);
                    logLabelSubmit(canonical, labelAnchor, info.label());
                } catch (Throwable t) {
                    SimpleChestLocker.LOGGER.error("Failed to submit label for {}: {}", canonical, t.toString());
                }
            } else if (!info.label().isEmpty()) {
                SimpleChestLocker.LOGGER.info("[SCL-DEBUG] Label skipped for {}: ownerMatch={} aimingAtIcon={}",
                        canonical,
                        LockClientState.isUnlockedForLocalPlayer(info),
                        isLookingAtLock(mc, canonical, anchor));
            }
        }

        for (java.util.Map.Entry<BlockPos, ItemStackRenderState> e : STATE_CACHE.entrySet()) {
            if (fresh.containsKey(e.getKey())) continue;
            e.getValue().clear();
        }
        java.util.Set<BlockPos> invalidated = new java.util.HashSet<>(STATE_CACHE.keySet());
        invalidated.removeAll(fresh.keySet());
        STATE_CACHE.keySet().removeAll(invalidated);
    }

    private static final class ComponentHolder {
        static void submitLabel(SubmitNodeCollector collector, Vec3 worldPos, String label,
                                net.minecraft.client.renderer.state.level.CameraRenderState cameraState, Vec3 camPos) {
            PoseStack pose = new PoseStack();
            pose.pushPose();
            pose.translate(-camPos.x, -camPos.y, -camPos.z);
            collector.submitNameTag(pose, worldPos, 0,
                    net.minecraft.network.chat.Component.literal(label),
                    false, 0xF000F0, cameraState);
            pose.popPose();
        }
    }

    private static long lastLabelSubmitLog = 0L;

    private static void logLabelSubmit(BlockPos canonical, Vec3 anchor, String label) {
        long now = System.currentTimeMillis();
        if (now - lastLabelSubmitLog < 1000L) return;
        lastLabelSubmitLog = now;
        SimpleChestLocker.LOGGER.info("[SCL-DEBUG] Submitting label '{}' for {} at ({}, {}, {})",
                label, canonical, (float) anchor.x, (float) anchor.y, (float) anchor.z);
    }
}
