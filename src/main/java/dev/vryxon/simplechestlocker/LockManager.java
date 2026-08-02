package dev.vryxon.simplechestlocker;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class LockManager extends SavedData {
    private static final String NAME = "simplechestlocker_data";

    private final Map<GlobalPos, UUID> lockedChests = new HashMap<>();
    private final Map<UUID, Map<BlockPos, Set<String>>> trustedPlayers = new HashMap<>();

    private static final Codec<LockEntry> LOCK_ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("dim").forGetter(LockEntry::dimensionId),
            BlockPos.CODEC.fieldOf("pos").forGetter(LockEntry::pos),
            UUIDUtil.STRING_CODEC.fieldOf("owner").forGetter(LockEntry::owner)
    ).apply(instance, LockEntry::new));

    private static final Codec<TrustEntry> TRUST_ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.STRING_CODEC.fieldOf("owner").forGetter(TrustEntry::owner),
            BlockPos.CODEC.fieldOf("pos").forGetter(TrustEntry::pos),
            Codec.STRING.listOf().fieldOf("names").forGetter(TrustEntry::names)
    ).apply(instance, TrustEntry::new));

    private static final Codec<LockManager> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LOCK_ENTRY_CODEC.listOf().optionalFieldOf("lockedChests", List.of()).forGetter(LockManager::toLockEntries),
            TRUST_ENTRY_CODEC.listOf().optionalFieldOf("trustedPlayers", List.of()).forGetter(LockManager::toTrustEntries)
    ).apply(instance, LockManager::new));

    private static final SavedDataType<LockManager> TYPE = new SavedDataType<>(
            NAME,
            LockManager::new,
            CODEC,
            null
    );

    public static LockManager get(ServerLevel world) {
        // Store once in the overworld PSM so all dimensions share the same data.
        return world.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(TYPE);
    }

    public LockManager() {
    }

    private LockManager(List<LockEntry> locks, List<TrustEntry> trusts) {
        for (LockEntry entry : locks) {
            lockedChests.put(entry.toGlobalPos(), entry.owner());
        }

        for (TrustEntry entry : trusts) {
            trustedPlayers
                    .computeIfAbsent(entry.owner(), __ -> new HashMap<>())
                    .computeIfAbsent(entry.pos(), __ -> new HashSet<>())
                    .addAll(entry.names());
        }
    }

    private List<LockEntry> toLockEntries() {
        List<LockEntry> entries = new ArrayList<>(lockedChests.size());
        for (Map.Entry<GlobalPos, UUID> entry : lockedChests.entrySet()) {
            entries.add(new LockEntry(
                    entry.getKey().dimension().identifier(),
                    entry.getKey().pos(),
                    entry.getValue()
            ));
        }
        return entries;
    }

    private List<TrustEntry> toTrustEntries() {
        List<TrustEntry> entries = new ArrayList<>();

        for (Map.Entry<UUID, Map<BlockPos, Set<String>>> ownerEntry : trustedPlayers.entrySet()) {
            UUID owner = ownerEntry.getKey();

            for (Map.Entry<BlockPos, Set<String>> chestEntry : ownerEntry.getValue().entrySet()) {
                entries.add(new TrustEntry(
                        owner,
                        chestEntry.getKey(),
                        new ArrayList<>(chestEntry.getValue())
                ));
            }
        }

        return entries;
    }

    // --- Locking API ---

    public static boolean lockChest(ServerLevel world, UUID owner, BlockPos pos) {
        LockManager mgr = get(world);
        GlobalPos key = GlobalPos.of(world.dimension(), pos);

        if (mgr.lockedChests.containsKey(key)) {
            return false;
        }

        mgr.lockedChests.put(key, owner);
        mgr.setDirty();
        return true;
    }

    public static boolean isLocked(ServerLevel world, BlockPos pos) {
        return get(world).lockedChests.containsKey(GlobalPos.of(world.dimension(), pos));
    }

    public static UUID getOwner(ServerLevel world, BlockPos pos) {
        return get(world).lockedChests.get(GlobalPos.of(world.dimension(), pos));
    }

    public static void removeLock(ServerLevel world, BlockPos pos) {
        LockManager mgr = get(world);
        mgr.lockedChests.remove(GlobalPos.of(world.dimension(), pos));
        mgr.setDirty();
    }

    // --- Trust API (per-chest) ---

    public static void addTrusted(ServerLevel world, UUID owner, BlockPos pos, String playerName) {
        LockManager mgr = get(world);
        mgr.trustedPlayers
                .computeIfAbsent(owner, __ -> new HashMap<>())
                .computeIfAbsent(pos, __ -> new HashSet<>())
                .add(playerName);
        mgr.setDirty();
    }

    public static void removeTrusted(ServerLevel world, UUID owner, BlockPos pos, String playerName) {
        LockManager mgr = get(world);
        Map<BlockPos, Set<String>> ownerMap = mgr.trustedPlayers.get(owner);

        if (ownerMap == null) {
            return;
        }

        Set<String> set = ownerMap.get(pos);
        if (set != null) {
            set.remove(playerName);
            if (set.isEmpty()) {
                ownerMap.remove(pos);
            }
        }

        if (ownerMap.isEmpty()) {
            mgr.trustedPlayers.remove(owner);
        }

        mgr.setDirty();
    }

    public static boolean isTrusted(ServerLevel world, UUID owner, BlockPos pos, String playerName) {
        LockManager mgr = get(world);
        return mgr.trustedPlayers
                .getOrDefault(owner, Collections.emptyMap())
                .getOrDefault(pos, Collections.emptySet())
                .contains(playerName);
    }

    public static Set<String> getTrusted(ServerLevel world, UUID owner, BlockPos pos) {
        return Collections.unmodifiableSet(
                get(world).trustedPlayers
                        .getOrDefault(owner, Collections.emptyMap())
                        .getOrDefault(pos, Collections.emptySet())
        );
    }

    private record LockEntry(Identifier dimensionId, BlockPos pos, UUID owner) {
        private GlobalPos toGlobalPos() {
            ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimensionId);
            return GlobalPos.of(dimKey, pos);
        }
    }

    private record TrustEntry(UUID owner, BlockPos pos, List<String> names) {
    }
}