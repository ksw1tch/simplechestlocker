package dev.vryxon.simplechestlocker.networking;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class LockOverlayPayloads {

    private LockOverlayPayloads() {}

    public static final Identifier CLIENTBOUND_LOCK_INFO_ID =
            Identifier.fromNamespaceAndPath("simplechestlocker", "lock_info");
    public static final Identifier CLIENTBOUND_LOCK_INVALIDATE_ID =
            Identifier.fromNamespaceAndPath("simplechestlocker", "lock_invalidate");
    public static final Identifier SERVERBOUND_LOCK_QUERY_ID =
            Identifier.fromNamespaceAndPath("simplechestlocker", "lock_query");

    public record ClientboundLockInfo(
            BlockPos pos,
            boolean locked,
            String ownerName,
            String label,
            java.util.List<String> trusted
    ) implements CustomPacketPayload {

        public static final Type<ClientboundLockInfo> TYPE = new Type<>(CLIENTBOUND_LOCK_INFO_ID);

        public static final StreamCodec<FriendlyByteBuf, ClientboundLockInfo> STREAM_CODEC =
                StreamCodec.composite(
                        BlockPos.STREAM_CODEC, ClientboundLockInfo::pos,
                        ByteBufCodecs.BOOL, ClientboundLockInfo::locked,
                        ByteBufCodecs.STRING_UTF8, ClientboundLockInfo::ownerName,
                        ByteBufCodecs.STRING_UTF8, ClientboundLockInfo::label,
                        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), ClientboundLockInfo::trusted,
                        ClientboundLockInfo::new
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ClientboundLockInvalidate(BlockPos pos) implements CustomPacketPayload {

        public static final Type<ClientboundLockInvalidate> TYPE = new Type<>(CLIENTBOUND_LOCK_INVALIDATE_ID);

        public static final StreamCodec<FriendlyByteBuf, ClientboundLockInvalidate> STREAM_CODEC =
                StreamCodec.composite(
                        BlockPos.STREAM_CODEC, ClientboundLockInvalidate::pos,
                        ClientboundLockInvalidate::new
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ServerboundLockQuery(BlockPos pos) implements CustomPacketPayload {

        public static final Type<ServerboundLockQuery> TYPE = new Type<>(SERVERBOUND_LOCK_QUERY_ID);

        public static final StreamCodec<FriendlyByteBuf, ServerboundLockQuery> STREAM_CODEC =
                StreamCodec.composite(
                        BlockPos.STREAM_CODEC, ServerboundLockQuery::pos,
                        ServerboundLockQuery::new
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
