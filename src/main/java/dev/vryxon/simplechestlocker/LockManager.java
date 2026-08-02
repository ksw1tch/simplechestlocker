package dev.vryxon.simplechestlocker;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.vryxon.simplechestlocker.persistence.LockEntryCodecs;
import dev.vryxon.simplechestlocker.persistence.LockEntryCodecs.LabelEntry;
import dev.vryxon.simplechestlocker.persistence.LockEntryCodecs.LockEntry;
import dev.vryxon.simplechestlocker.persistence.LockEntryCodecs.TrustEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class LockManager extends SavedData {
    private static final String NAME = "simplechestlocker_data";

    private final Map<GlobalPos, UUID> lockedChests = new HashMap<>();
    private final Map<UUID, Map<GlobalPos, Set<String>>> trustedPlayers = new HashMap<>();
    private final Map<UUID, Map<GlobalPos, String>> labels = new HashMap<>();

    private static final Codec<LockManager> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LockEntryCodecs.LOCK_ENTRY_CODEC.listOf().optionalFieldOf("lockedChests", List.of()).forGetter(LockManager::toLockEntries),
            LockEntryCodecs.TRUST_ENTRY_CODEC.listOf().optionalFieldOf("trustedPlayers", List.of()).forGetter(LockManager::toTrustEntries),
            LockEntryCodecs.LABEL_ENTRY_CODEC.listOf().optionalFieldOf("labels", List.of()).forGetter(LockManager::toLabelEntries)
    ).apply(instance, LockManager::new));

    private static final SavedDataType<LockManager> TYPE = new SavedDataType<>(
            NAME,
            LockManager::new,
            CODEC,
            null
    );

    public static LockManager get(ServerLevel world) {
        return world.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(TYPE);
    }

    public LockManager() {
    }

    private LockManager(List<LockEntry> locks, List<TrustEntry> trusts, List<LabelEntry> labelEntries) {
        for (LockEntry entry : locks) {
            lockedChests.put(entry.toGlobalPos(), entry.owner());
        }

        for (TrustEntry entry : trusts) {
            trustedPlayers
                    .computeIfAbsent(entry.owner(), __ -> new HashMap<>())
                    .computeIfAbsent(entry.toGlobalPos(), __ -> new HashSet<>())
                    .addAll(entry.names());
        }

        for (LabelEntry entry : labelEntries) {
            labels
                    .computeIfAbsent(entry.owner(), __ -> new HashMap<>())
                    .put(entry.toGlobalPos(), entry.label());
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

        for (Map.Entry<UUID, Map<GlobalPos, Set<String>>> ownerEntry : trustedPlayers.entrySet()) {
            UUID owner = ownerEntry.getKey();

            for (Map.Entry<GlobalPos, Set<String>> chestEntry : ownerEntry.getValue().entrySet()) {
                entries.add(new TrustEntry(
                        owner,
                        chestEntry.getKey().dimension().identifier(),
                        chestEntry.getKey().pos(),
                        new ArrayList<>(chestEntry.getValue())
                ));
            }
        }

        return entries;
    }

    private List<LabelEntry> toLabelEntries() {
        List<LabelEntry> entries = new ArrayList<>();

        for (Map.Entry<UUID, Map<GlobalPos, String>> ownerEntry : labels.entrySet()) {
            UUID owner = ownerEntry.getKey();

            for (Map.Entry<GlobalPos, String> chestEntry : ownerEntry.getValue().entrySet()) {
                entries.add(new LabelEntry(
                        owner,
                        chestEntry.getKey().dimension().identifier(),
                        chestEntry.getKey().pos(),
                        chestEntry.getValue()
                ));
            }
        }

        return entries;
    }

    private static GlobalPos globalPosOf(ServerLevel world, BlockPos pos) {
        return GlobalPos.of(world.dimension(), pos);
    }

    public static boolean lockChest(ServerLevel world, UUID owner, BlockPos pos) {
        LockManager mgr = get(world);
        BlockPos[] parts = LockableContainers.resolveLockableBlocks(pos, world);

        for (BlockPos part : parts) {
            if (mgr.lockedChests.containsKey(globalPosOf(world, part))) {
                return false;
            }
        }

        for (BlockPos part : parts) {
            mgr.lockedChests.put(globalPosOf(world, part), owner);
        }

        mgr.setDirty();
        return true;
    }

    public static boolean isLocked(ServerLevel world, BlockPos pos) {
        return get(world).lockedChests.containsKey(globalPosOf(world, pos));
    }

    public static boolean isAnyLocked(ServerLevel world, BlockPos pos) {
        LockManager mgr = get(world);
        BlockPos[] parts = LockableContainers.resolveLockableBlocks(pos, world);

        for (BlockPos part : parts) {
            if (mgr.lockedChests.containsKey(globalPosOf(world, part))) {
                return true;
            }
        }

        return false;
    }

    public static UUID getOwner(ServerLevel world, BlockPos pos) {
        LockManager mgr = get(world);
        BlockPos[] parts = LockableContainers.resolveLockableBlocks(pos, world);

        for (BlockPos part : parts) {
            UUID owner = mgr.lockedChests.get(globalPosOf(world, part));
            if (owner != null) {
                return owner;
            }
        }

        return null;
    }

    public static void removeLock(ServerLevel world, BlockPos pos) {
        LockManager mgr = get(world);

        BlockPos[] parts = LockableContainers.resolveLockableBlocks(pos, world);

        for (BlockPos part : parts) {
            GlobalPos globalPos = globalPosOf(world, part);
            UUID owner = mgr.lockedChests.remove(globalPos);

            if (owner != null) {
                clearChestTrusts(mgr, owner, globalPos);
                clearChestLabel(mgr, owner, globalPos);
            }
        }

        mgr.setDirty();
    }

    private static void clearChestTrusts(LockManager mgr, UUID owner, GlobalPos globalPos) {
        Map<GlobalPos, Set<String>> trusts = mgr.trustedPlayers.get(owner);
        if (trusts != null) {
            trusts.remove(globalPos);
            if (trusts.isEmpty()) {
                mgr.trustedPlayers.remove(owner);
            }
        }
    }

    private static void clearChestLabel(LockManager mgr, UUID owner, GlobalPos globalPos) {
        Map<GlobalPos, String> ownerLabels = mgr.labels.get(owner);
        if (ownerLabels != null) {
            ownerLabels.remove(globalPos);
            if (ownerLabels.isEmpty()) {
                mgr.labels.remove(owner);
            }
        }
    }

    public static void addTrusted(ServerLevel world, UUID owner, BlockPos pos, String playerName) {
        LockManager mgr = get(world);
        BlockPos[] parts = LockableContainers.resolveLockableBlocks(pos, world);

        for (BlockPos part : parts) {
            mgr.trustedPlayers
                    .computeIfAbsent(owner, __ -> new HashMap<>())
                    .computeIfAbsent(globalPosOf(world, part), __ -> new HashSet<>())
                    .add(playerName);
        }

        mgr.setDirty();
    }

    public static void removeTrusted(ServerLevel world, UUID owner, BlockPos pos, String playerName) {
        LockManager mgr = get(world);
        BlockPos[] parts = LockableContainers.resolveLockableBlocks(pos, world);

        for (BlockPos part : parts) {
            Map<GlobalPos, Set<String>> ownerMap = mgr.trustedPlayers.get(owner);
            if (ownerMap == null) {
                continue;
            }

            Set<String> set = ownerMap.get(globalPosOf(world, part));
            if (set != null) {
                set.remove(playerName);
                if (set.isEmpty()) {
                    ownerMap.remove(globalPosOf(world, part));
                }
            }

            if (ownerMap.isEmpty()) {
                mgr.trustedPlayers.remove(owner);
            }
        }

        mgr.setDirty();
    }

    public static boolean isTrusted(ServerLevel world, UUID owner, BlockPos pos, String playerName) {
        LockManager mgr = get(world);
        BlockPos[] parts = LockableContainers.resolveLockableBlocks(pos, world);

        for (BlockPos part : parts) {
            if (mgr.trustedPlayers
                    .getOrDefault(owner, Collections.emptyMap())
                    .getOrDefault(globalPosOf(world, part), Collections.emptySet())
                    .contains(playerName)) {
                return true;
            }
        }

        return false;
    }

    public static Set<String> getTrusted(ServerLevel world, UUID owner, BlockPos pos) {
        BlockPos[] parts = LockableContainers.resolveLockableBlocks(pos, world);
        Set<String> merged = new HashSet<>();

        for (BlockPos part : parts) {
            merged.addAll(get(world).trustedPlayers
                    .getOrDefault(owner, Collections.emptyMap())
                    .getOrDefault(globalPosOf(world, part), Collections.emptySet()));
        }

        return Collections.unmodifiableSet(merged);
    }

    public static Set<GlobalPos> getLockedChestsForOwner(ServerLevel world, UUID owner) {
        LockManager mgr = get(world);
        Set<GlobalPos> result = new HashSet<>();
        for (Map.Entry<GlobalPos, UUID> entry : mgr.lockedChests.entrySet()) {
            if (entry.getValue().equals(owner)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public static Set<GlobalPos> getUniqueLockedContainersForOwner(ServerLevel world, UUID owner) {
        LockManager mgr = get(world);
        Set<GlobalPos> all = new HashSet<>();
        for (Map.Entry<GlobalPos, UUID> entry : mgr.lockedChests.entrySet()) {
            if (entry.getValue().equals(owner)) {
                all.add(entry.getKey());
            }
        }

        Set<GlobalPos> unique = new HashSet<>();
        for (GlobalPos gp : all) {
            ServerLevel level = world.getServer().getLevel(gp.dimension());
            if (level == null) {
                unique.add(gp);
                continue;
            }
            BlockPos[] parts = LockableContainers.resolveLockableBlocks(gp.pos(), level);
            if (parts.length <= 1) {
                unique.add(gp);
                continue;
            }
            BlockPos canonical = parts[0];
            for (BlockPos part : parts) {
                if (part.compareTo(canonical) < 0) {
                    canonical = part;
                }
            }
            if (gp.pos().equals(canonical)) {
                unique.add(gp);
            }
        }

        return unique;
    }

    public static void setLabel(ServerLevel world, UUID owner, BlockPos pos, String label) {
        LockManager mgr = get(world);
        BlockPos[] parts = LockableContainers.resolveLockableBlocks(pos, world);

        for (BlockPos part : parts) {
            mgr.labels
                    .computeIfAbsent(owner, __ -> new HashMap<>())
                    .put(globalPosOf(world, part), label);
        }

        mgr.setDirty();
    }

    public static void removeLabel(ServerLevel world, UUID owner, BlockPos pos) {
        LockManager mgr = get(world);
        BlockPos[] parts = LockableContainers.resolveLockableBlocks(pos, world);

        for (BlockPos part : parts) {
            Map<GlobalPos, String> ownerLabels = mgr.labels.get(owner);
            if (ownerLabels == null) {
                continue;
            }

            ownerLabels.remove(globalPosOf(world, part));

            if (ownerLabels.isEmpty()) {
                mgr.labels.remove(owner);
            }
        }

        mgr.setDirty();
    }

    public static String getLabel(ServerLevel world, UUID owner, BlockPos pos) {
        LockManager mgr = get(world);
        BlockPos[] parts = LockableContainers.resolveLockableBlocks(pos, world);

        for (BlockPos part : parts) {
            Map<GlobalPos, String> ownerLabels = mgr.labels.get(owner);
            if (ownerLabels == null) {
                continue;
            }
            String label = ownerLabels.get(globalPosOf(world, part));
            if (label != null) {
                return label;
            }
        }

        return null;
    }

    public static boolean isLabeled(ServerLevel world, UUID owner, BlockPos pos) {
        return getLabel(world, owner, pos) != null;
    }

    public static boolean hasLabel(ServerLevel world, UUID owner, BlockPos currentPos, String label) {
        LockManager mgr = get(world);

        Map<GlobalPos, String> ownerLabels = mgr.labels.get(owner);

        if (ownerLabels == null) {
            return false;
        }

        BlockPos[] parts = LockableContainers.resolveLockableBlocks(currentPos, world);

        for (Map.Entry<GlobalPos, String> entry : ownerLabels.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(label)) {
                boolean isPart = false;
                for (BlockPos part : parts) {
                    if (entry.getKey().equals(globalPosOf(world, part))) {
                        isPart = true;
                        break;
                    }
                }
                if (!isPart) {
                    return true;
                }
            }
        }

        return false;
    }

    public static GlobalPos getPosFromLabel(ServerLevel world, UUID owner, String label) {
        LockManager mgr = get(world);

        Map<GlobalPos, String> ownerLabels = mgr.labels.get(owner);

        if (ownerLabels == null) {
            return null;
        }

        for (Map.Entry<GlobalPos, String> entry : ownerLabels.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(label)) {
                return entry.getKey();
            }
        }

        return null;
    }

    public static Collection<String> getAllLabels(ServerLevel world, UUID owner) {
        LockManager mgr = get(world);

        Map<GlobalPos, String> ownerLabels = mgr.labels.get(owner);

        if (ownerLabels == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableCollection(ownerLabels.values());
    }
}