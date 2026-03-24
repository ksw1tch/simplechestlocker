package me.toobad.simplechestlocker;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;

import java.util.*;

public class LockManager extends PersistentState {
    private static final String NAME = "simplechestlocker_data";

    private final Map<GlobalPos, UUID> lockedChests = new HashMap<>();
    private final Map<UUID, Map<BlockPos, Set<String>>> trustedPlayers = new HashMap<>();

    private static final Codec<LockEntry> LOCK_ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("dim").forGetter(LockEntry::dimensionId),
            BlockPos.CODEC.fieldOf("pos").forGetter(LockEntry::pos),
            Uuids.STRING_CODEC.fieldOf("owner").forGetter(LockEntry::owner)
    ).apply(instance, LockEntry::new));

    private static final Codec<TrustEntry> TRUST_ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Uuids.STRING_CODEC.fieldOf("owner").forGetter(TrustEntry::owner),
            BlockPos.CODEC.fieldOf("pos").forGetter(TrustEntry::pos),
            Codec.STRING.listOf().fieldOf("names").forGetter(TrustEntry::names)
    ).apply(instance, TrustEntry::new));

    private static final Codec<LockManager> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LOCK_ENTRY_CODEC.listOf().optionalFieldOf("lockedChests", List.of()).forGetter(LockManager::toLockEntries),
            TRUST_ENTRY_CODEC.listOf().optionalFieldOf("trustedPlayers", List.of()).forGetter(LockManager::toTrustEntries)
    ).apply(instance, LockManager::new));

    private static final PersistentStateType<LockManager> TYPE = new PersistentStateType<>(
            NAME,
            LockManager::new,
            CODEC,
            null
    );

    public static LockManager get(ServerWorld world) {
        // Store once in the overworld PSM so all dimensions share the same data.
        return world.getServer()
                .getOverworld()
                .getPersistentStateManager()
                .getOrCreate(TYPE);
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
                    entry.getKey().dimension().getValue(),
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

    public static boolean lockChest(ServerWorld world, UUID owner, BlockPos pos) {
        LockManager mgr = get(world);
        GlobalPos key = GlobalPos.create(world.getRegistryKey(), pos);

        if (mgr.lockedChests.containsKey(key)) {
            return false;
        }

        mgr.lockedChests.put(key, owner);
        mgr.markDirty();
        return true;
    }

    public static boolean isLocked(ServerWorld world, BlockPos pos) {
        return get(world).lockedChests.containsKey(GlobalPos.create(world.getRegistryKey(), pos));
    }

    public static UUID getOwner(ServerWorld world, BlockPos pos) {
        return get(world).lockedChests.get(GlobalPos.create(world.getRegistryKey(), pos));
    }

    public static void removeLock(ServerWorld world, BlockPos pos) {
        LockManager mgr = get(world);
        mgr.lockedChests.remove(GlobalPos.create(world.getRegistryKey(), pos));
        mgr.markDirty();
    }

    // --- Trust API (per-chest) ---

    public static void addTrusted(ServerWorld world, UUID owner, BlockPos pos, String playerName) {
        LockManager mgr = get(world);
        mgr.trustedPlayers
                .computeIfAbsent(owner, __ -> new HashMap<>())
                .computeIfAbsent(pos, __ -> new HashSet<>())
                .add(playerName);
        mgr.markDirty();
    }

    public static void removeTrusted(ServerWorld world, UUID owner, BlockPos pos, String playerName) {
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

        mgr.markDirty();
    }

    public static boolean isTrusted(ServerWorld world, UUID owner, BlockPos pos, String playerName) {
        LockManager mgr = get(world);
        return mgr.trustedPlayers
                .getOrDefault(owner, Collections.emptyMap())
                .getOrDefault(pos, Collections.emptySet())
                .contains(playerName);
    }

    public static Set<String> getTrusted(ServerWorld world, UUID owner, BlockPos pos) {
        return Collections.unmodifiableSet(
                get(world).trustedPlayers
                        .getOrDefault(owner, Collections.emptyMap())
                        .getOrDefault(pos, Collections.emptySet())
        );
    }

    private record LockEntry(Identifier dimensionId, BlockPos pos, UUID owner) {
        private GlobalPos toGlobalPos() {
            RegistryKey<World> dimKey = RegistryKey.of(RegistryKeys.WORLD, dimensionId);
            return GlobalPos.create(dimKey, pos);
        }
    }

    private record TrustEntry(UUID owner, BlockPos pos, List<String> names) {
    }
}