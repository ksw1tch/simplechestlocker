package me.toobad.simplechestlocker.mixin;

import me.toobad.simplechestlocker.LockConfig;
import me.toobad.simplechestlocker.LockManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.explosion.ExplosionImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;
import java.util.List;

@Mixin(ExplosionImpl.class)
public abstract class ExplosionImplMixin {

    @Shadow
    public abstract ServerWorld getWorld();

    @Inject(method = "getBlocksToDestroy", at = @At("RETURN"), cancellable = true)
    private void protectLockedContainers(CallbackInfoReturnable<List<BlockPos>> cir) {
        if (!LockConfig.get().protectFromExplosions) return;

        List<BlockPos> affectedBlocks = cir.getReturnValue();
        if (affectedBlocks == null || affectedBlocks.isEmpty()) return;

        ServerWorld serverWorld = getWorld();
        Iterator<BlockPos> iterator = affectedBlocks.iterator();
        while (iterator.hasNext()) {
            if (LockManager.isLocked(serverWorld, iterator.next())) {
                iterator.remove();
            }
        }
    }
}