package dev.vryxon.simplechestlocker.mixin;

import dev.vryxon.simplechestlocker.LockConfig;
import dev.vryxon.simplechestlocker.LockManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonBaseBlock.class)
public abstract class PistonBlockMixin {

    @Inject(method = "moveBlocks", at = @At("HEAD"), cancellable = true)
    private void preventPistonInteractionWithLockedBlock(Level world, BlockPos pos, Direction dir, boolean extend, CallbackInfoReturnable<Boolean> cir) {
        if (world.isClientSide()) return;
        if (!extend) return;
        if (!LockConfig.get().protectFromPistons) return;
        if (!(world instanceof ServerLevel serverWorld)) return;

        BlockPos targetPos = pos.relative(dir);
        if (LockManager.isLocked(serverWorld, targetPos)) {
            cir.setReturnValue(false);
        }
    }
}