package dev.vryxon.simplechestlocker.helpers;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

public class MsgHelper {

    public static void sendMsg(ServerPlayer player, String message) {
        player.sendSystemMessage(prefixed(toComponent(message)), false);
    }

    public static void sendMsgS(ServerPlayer player, String message) {
        player.sendSystemMessage(toComponent(message), false);
    }

    private static Component prefixed(Component body) {
        return Component.literal("scl | ")
                .withStyle(ChatFormatting.GRAY)
                .copy()
                .append(body);
    }

    private static MutableComponent toComponent(String text) {
        MutableComponent result = Component.empty();
        java.util.List<ChatFormatting> active = new java.util.ArrayList<>();
        StringBuilder buf = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                ChatFormatting fmt = ChatFormatting.getByCode(text.charAt(i + 1));
                if (fmt != null) {
                    if (buf.length() > 0) {
                        result.append(buildSegment(buf.toString(), active));
                        buf.setLength(0);
                    }
                    if (isColorOrReset(fmt)) {
                        active.clear();
                    }
                    if (fmt != ChatFormatting.RESET) {
                        active.add(fmt);
                    }
                    i += 2;
                    continue;
                }
            }
            buf.append(c);
            i++;
        }
        if (buf.length() > 0) {
            result.append(buildSegment(buf.toString(), active));
        }
        return result;
    }

    private static MutableComponent buildSegment(String text, java.util.List<ChatFormatting> styles) {
        MutableComponent part = Component.literal(text);
        if (!styles.isEmpty()) {
            part = part.withStyle(styles.toArray(new ChatFormatting[0]));
        }
        return part;
    }

    private static boolean isColorOrReset(ChatFormatting fmt) {
        return fmt == ChatFormatting.RESET || fmt.ordinal() <= ChatFormatting.WHITE.ordinal();
    }
}
