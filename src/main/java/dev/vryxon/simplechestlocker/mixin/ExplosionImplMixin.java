package dev.vryxon.simplechestlocker.mixin;

import dev.vryxon.simplechestlocker.LockConfig;
import dev.vryxon.simplechestlocker.LockManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerExplosion;

@Mixin(ServerExplosion.class)
public abstract class ExplosionImplMixin {

    @Shadow
    public abstract ServerLevel level();

    @Inject(method = "calculateExplodedPositions", at = @At("RETURN"), cancellable = true)
    private void protectLockedContainers(CallbackInfoReturnable<List<BlockPos>> cir) {
        if (!LockConfig.get().protectFromExplosions) return;

        List<BlockPos> affectedBlocks = cir.getReturnValue();
        if (affectedBlocks == null || affectedBlocks.isEmpty()) return;

        ServerLevel serverWorld = level();
        Iterator<BlockPos> iterator = affectedBlocks.iterator();
        while (iterator.hasNext()) {
            if (LockManager.isLocked(serverWorld, iterator.next())) {
                iterator.remove();
            }
        }
    }
}