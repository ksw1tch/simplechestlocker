package dev.vryxon.simplechestlocker.client;

import dev.vryxon.simplechestlocker.networking.LockOverlayPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class LockClientState {
    private static final Map<BlockPos, Info> CACHE = new HashMap<>();
    private static final Map<BlockPos, Long> PENDING = new HashMap<>();
    private static final long TTL_MS = 5_000L;
    private static final long PENDING_COOLDOWN_MS = 1_000L;

    private LockClientState() {}

    public record Info(boolean locked, String ownerName, String label, List<String> trusted) {}

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(LockOverlayPayloads.ClientboundLockInfo.TYPE, (payload, ctx) -> {
            BlockPos pos = payload.pos();
            Info info = new Info(payload.locked(), payload.ownerName(),
                    payload.label(), new ArrayList<>(payload.trusted()));
            if (!payload.label().isEmpty()) {
                dev.vryxon.simplechestlocker.SimpleChestLocker.LOGGER.info(
                        "[SCL-DEBUG] Received lock info at {} locked={} owner='{}' label='{}'",
                        pos, payload.locked(), payload.ownerName(), payload.label());
            }
            synchronized (CACHE) {
                CACHE.put(pos, info);
                PENDING.remove(pos);
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(LockOverlayPayloads.ClientboundLockInvalidate.TYPE, (payload, ctx) -> {
            invalidate(payload.pos());
        });
    }

    public static void invalidate(BlockPos pos) {
        synchronized (CACHE) {
            CACHE.remove(pos);
        }
    }

    public static Info get(BlockPos pos) {
        synchronized (CACHE) {
            Info info = CACHE.get(pos);
            if (info != null) {
                long ts = System.currentTimeMillis() - 1L;
                boolean fresh = true;
                Long last = PENDING.get(pos);
                if (last == null) fresh = false;
                if (last != null && System.currentTimeMillis() - last > TTL_MS) fresh = false;
                if (!fresh) {
                    request(pos);
                }
                return info;
            }
        }
        request(pos);
        return null;
    }

    public static void request(BlockPos pos) {
        long now = System.currentTimeMillis();
        synchronized (CACHE) {
            Long last = PENDING.get(pos);
            if (last != null && now - last < PENDING_COOLDOWN_MS) return;
            PENDING.put(pos, now);
        }
        ClientPlayNetworking.send(new LockOverlayPayloads.ServerboundLockQuery(pos));
    }

    public static void tick() {
        synchronized (CACHE) {
            long now = System.currentTimeMillis();
            Iterator<Map.Entry<BlockPos, Info>> it = CACHE.entrySet().iterator();
            while (it.hasNext()) {
                Info info = it.next().getValue();
                if (info == null) it.remove();
            }
            Iterator<Map.Entry<BlockPos, Long>> pit = PENDING.entrySet().iterator();
            while (pit.hasNext()) {
                Map.Entry<BlockPos, Long> e = pit.next();
                if (now - e.getValue() > PENDING_COOLDOWN_MS * 2) {
                    pit.remove();
                }
            }
        }
    }

    public static boolean isUnlockedForLocalPlayer(Info info) {
        if (info == null || !info.locked()) return false;
        String localName = Minecraft.getInstance().getUser().getName();
        if (info.ownerName() != null && info.ownerName().equalsIgnoreCase(localName)) return true;
        for (String t : info.trusted()) {
            if (t != null && t.equalsIgnoreCase(localName)) return true;
        }
        return false;
    }

    public static Map<BlockPos, Info> snapshot() {
        synchronized (CACHE) {
            return Collections.unmodifiableMap(new HashMap<>(CACHE));
        }
    }
}
