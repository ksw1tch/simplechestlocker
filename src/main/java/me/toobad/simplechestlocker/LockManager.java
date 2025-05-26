package me.toobad.simplechestlocker;

import net.minecraft.nbt.*;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.*;

public class LockManager extends PersistentState {
    private static final String NAME = "simplechestlocker_data";
    private static LockManager instance;

    private final Map<GlobalPos, UUID> lockedChests = new HashMap<>();
    private final Map<UUID, Map<BlockPos, Set<String>>> trustedPlayers = new HashMap<>();

    /** Singleton fetch (per‑world) */
    public static LockManager get(ServerWorld world) {
        if (instance == null) {
            instance = world.getPersistentStateManager().getOrCreate(
                    new PersistentState.Type<>(LockManager::new, LockManager::fromNbt, null),
                    NAME
            );
        }
        return instance;
    }

    // --- Locking API ---

    public static boolean lockChest(ServerWorld world, UUID owner, BlockPos pos) {
        LockManager mgr = get(world);
        GlobalPos key = GlobalPos.create(world.getRegistryKey(), pos);
        if (mgr.lockedChests.containsKey(key)) return false;
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

    // --- Trust API (per‑chest) ---

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
        if (ownerMap == null) return;
        Set<String> set = ownerMap.get(pos);
        if (set != null) {
            set.remove(playerName);
            if (set.isEmpty()) ownerMap.remove(pos);
        }
        if (ownerMap.isEmpty()) mgr.trustedPlayers.remove(owner);
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

    // --- NBT Persistence ---

    /** Deserialize all data from NBT */
    private static LockManager fromNbt(NbtCompound nbt, net.minecraft.registry.RegistryWrapper.WrapperLookup regs) {
        LockManager mgr = new LockManager();

        // Locked chests
        NbtList lockList = nbt.getList("lockedChests", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < lockList.size(); i++) {
            NbtCompound tag = lockList.getCompound(i);
            UUID owner = tag.getUuid("owner");
            BlockPos pos = BlockPos.fromLong(tag.getLong("pos"));

            // Use the public single‑arg Identifier.parse
            String dimString = tag.getString("dim");
            Identifier dimId = Identifier.of(dimString);
            RegistryKey<World> dimKey = RegistryKey.of(RegistryKeys.WORLD, dimId);

            mgr.lockedChests.put(GlobalPos.create(dimKey, pos), owner);
        }

        // Trusted players
        NbtList trustList = nbt.getList("trustedPlayers", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < trustList.size(); i++) {
            NbtCompound tag = trustList.getCompound(i);
            UUID owner = tag.getUuid("owner");
            BlockPos pos = BlockPos.fromLong(tag.getLong("pos"));

            NbtList names = tag.getList("names", NbtElement.STRING_TYPE);
            Set<String> set = mgr.trustedPlayers
                    .computeIfAbsent(owner, __ -> new HashMap<>())
                    .computeIfAbsent(pos, __ -> new HashSet<>());

            for (int j = 0; j < names.size(); j++) {
                set.add(names.getString(j));
            }
        }

        return mgr;
    }

    /** Serialize all data to NBT */
    @Override
    public NbtCompound writeNbt(NbtCompound nbt, net.minecraft.registry.RegistryWrapper.WrapperLookup regs) {
        // Locked chests
        NbtList lockList = new NbtList();
        for (var entry : lockedChests.entrySet()) {
            NbtCompound tag = new NbtCompound();
            tag.putUuid("owner", entry.getValue());
            tag.putLong("pos", entry.getKey().pos().asLong());
            tag.putString("dim", entry.getKey().dimension().getValue().toString());
            lockList.add(tag);
        }
        nbt.put("lockedChests", lockList);

        // Trusted players
        NbtList trustList = new NbtList();
        for (var ownerEntry : trustedPlayers.entrySet()) {
            UUID owner = ownerEntry.getKey();
            for (var chestEntry : ownerEntry.getValue().entrySet()) {
                BlockPos pos = chestEntry.getKey();
                Set<String> names = chestEntry.getValue();

                NbtCompound tag = new NbtCompound();
                tag.putUuid("owner", owner);
                tag.putLong("pos", pos.asLong());

                NbtList nameList = new NbtList();
                for (String name : names) {
                    nameList.add(NbtString.of(name));
                }
                tag.put("names", nameList);
                trustList.add(tag);
            }
        }
        nbt.put("trustedPlayers", trustList);

        return nbt;
    }
}
