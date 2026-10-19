package org.atomicworkshop.jammachine.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.atomicworkshop.jammachine.Reference;
import org.atomicworkshop.jammachine.tiles.SequencerTileEntity;

public record SequencerSetPitchAtInterval(int pitch, int interval, boolean isEnabled, BlockPos pos, ResourceKey<Level> dimension) implements CustomPacketPayload {
    public static final Type<SequencerSetPitchAtInterval> TYPE = new Type<>(Reference.resource("set_pitch_at_interval"));

    public static final StreamCodec<ByteBuf, SequencerSetPitchAtInterval> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SequencerSetPitchAtInterval::pitch,
            ByteBufCodecs.INT, SequencerSetPitchAtInterval::interval,
            ByteBufCodecs.BOOL, SequencerSetPitchAtInterval::isEnabled,
            BlockPos.STREAM_CODEC, SequencerSetPitchAtInterval::pos,
            ResourceKey.streamCodec(Registries.DIMENSION), SequencerSetPitchAtInterval::dimension,
            SequencerSetPitchAtInterval::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayer player)
    {
        if (pitch < 0 || pitch >= 25 || interval < 0 || interval >= 16) return;
        ServerLevel world = player.level();
        if (world.dimension() != this.dimension || !player.isWithinBlockInteractionRange(pos, 4.0)) return;
        if (!world.isLoaded(pos)) return;
        BlockEntity tileEntity = world.getBlockEntity(pos);
        if (tileEntity instanceof SequencerTileEntity) {
            ((SequencerTileEntity)tileEntity).notifyPitchAtIntervalChanged(pitch, interval, isEnabled);
        }
    }
}
