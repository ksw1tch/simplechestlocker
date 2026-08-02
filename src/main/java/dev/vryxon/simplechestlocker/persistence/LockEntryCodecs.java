package dev.vryxon.simplechestlocker.persistence;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

public final class LockEntryCodecs {
    public static final Identifier DEFAULT_DIMENSION = Identifier.parse("minecraft:overworld");

    private LockEntryCodecs() {}

    public static final Codec<LockEntry> LOCK_ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("dim").forGetter(LockEntry::dimensionId),
            BlockPos.CODEC.fieldOf("pos").forGetter(LockEntry::pos),
            UUIDUtil.STRING_CODEC.fieldOf("owner").forGetter(LockEntry::owner)
    ).apply(instance, LockEntry::new));

    public static final Codec<TrustEntry> TRUST_ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.STRING_CODEC.fieldOf("owner").forGetter(TrustEntry::owner),
            Identifier.CODEC.optionalFieldOf("dim", DEFAULT_DIMENSION).forGetter(TrustEntry::dimensionId),
            BlockPos.CODEC.fieldOf("pos").forGetter(TrustEntry::pos),
            Codec.STRING.listOf().fieldOf("names").forGetter(TrustEntry::names)
    ).apply(instance, TrustEntry::new));

    public static final Codec<LabelEntry> LABEL_ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.STRING_CODEC.fieldOf("owner").forGetter(LabelEntry::owner),
            Identifier.CODEC.optionalFieldOf("dim", DEFAULT_DIMENSION).forGetter(LabelEntry::dimensionId),
            BlockPos.CODEC.fieldOf("pos").forGetter(LabelEntry::pos),
            Codec.STRING.fieldOf("label").forGetter(LabelEntry::label)
    ).apply(instance, LabelEntry::new));

    public record LockEntry(Identifier dimensionId, BlockPos pos, UUID owner) {
        public GlobalPos toGlobalPos() {
            ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimensionId);
            return GlobalPos.of(dimKey, pos);
        }
    }

    public record TrustEntry(UUID owner, Identifier dimensionId, BlockPos pos, List<String> names) {
        public GlobalPos toGlobalPos() {
            ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimensionId);
            return GlobalPos.of(dimKey, pos);
        }
    }

    public record LabelEntry(UUID owner, Identifier dimensionId, BlockPos pos, String label) {
        public GlobalPos toGlobalPos() {
            ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimensionId);
            return GlobalPos.of(dimKey, pos);
        }
    }
}