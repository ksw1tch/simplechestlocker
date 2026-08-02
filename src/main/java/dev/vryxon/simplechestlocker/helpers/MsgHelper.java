package dev.vryxon.simplechestlocker.helpers;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class MsgHelper {

    public static void sendMsg(ServerPlayer player, String message) {
        player.displayClientMessage(Component.literal("\u00A77scl | \u00A7r" + message), false);
    }

    public static void sendMsgS(ServerPlayer player, String message) {
        player.displayClientMessage(Component.literal(message), false);
    }
}
