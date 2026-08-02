package dev.vryxon.simplechestlocker.mixin;

import dev.vryxon.simplechestlocker.gui.SignInputHandler;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleSignUpdate", at = @At("HEAD"), cancellable = true)
    private void onSignUpdate(ServerboundSignUpdatePacket packet, CallbackInfo ci) {
        ServerPlayer sp = this.player;
        if (sp != null && SignInputHandler.isPending(sp, packet.getPos())) {
            SignInputHandler.handleSignUpdate(sp, packet.getPos(), packet.getLines());
            ci.cancel();
        }
    }
}