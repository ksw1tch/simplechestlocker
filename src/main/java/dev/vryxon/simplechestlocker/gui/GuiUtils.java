package dev.vryxon.simplechestlocker.gui;

import dev.vryxon.simplechestlocker.LockableContainers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class GuiUtils {
    private GuiUtils() {}

    public static BlockPos getTargetedContainerPos(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return null;

        HitResult hit = serverPlayer.pick(6.0, serverPlayer.getYRot(), false);
        if (hit.getType() != HitResult.Type.BLOCK) return null;

        BlockHitResult blockHit = (BlockHitResult) hit;
        BlockPos pos = blockHit.getBlockPos();
        if (!LockableContainers.isLockable(serverPlayer.level(), pos)) return null;
        return pos;
    }
}