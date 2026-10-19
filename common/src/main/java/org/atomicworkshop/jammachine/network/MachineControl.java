package org.atomicworkshop.jammachine.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.atomicworkshop.jammachine.Reference;
import org.atomicworkshop.jammachine.tiles.ControllerTileEntity;
import org.atomicworkshop.jammachine.tiles.SequencerTileEntity;

//a button pressed in the sequencer or controller screen
public record MachineControl(BlockPos pos, int action, int value, int extra) implements CustomPacketPayload {
    public static final int SET_BPM = 0;
    public static final int SELECT_PATTERN = 1;
    public static final int TOGGLE_PROGRAM = 2;
    public static final int TOGGLE_PLAY = 3;
    //value is the sequencer slot on the controller, extra the pattern
    public static final int SEQUENCER_PATTERN = 4;
    public static final int TRACK_COLOUR = 5;
    //value is pattern, interval and pitch packed as for a note index, extra the colour
    public static final int PAINT_NOTE = 6;

    public static int noteIndex(int pattern, int interval, int pitch) {
        return (pattern * 16 + interval) * 25 + pitch;
    }

    public static final Type<MachineControl> TYPE = new Type<>(Reference.resource("machine_control"));

    public static final StreamCodec<ByteBuf, MachineControl> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, MachineControl::pos,
            ByteBufCodecs.VAR_INT, MachineControl::action,
            ByteBufCodecs.VAR_INT, MachineControl::value,
            ByteBufCodecs.VAR_INT, MachineControl::extra,
            MachineControl::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayer player) {
        final ServerLevel world = player.level();
        if (!player.isWithinBlockInteractionRange(pos, 4.0) || !world.isLoaded(pos)) return;
        if (world.getBlockEntity(pos) instanceof SequencerTileEntity sequencer) {
            switch (action) {
                case SET_BPM -> {
                    if (!sequencer.hasController()) sequencer.setBeatsPerMinute(value);
                }
                case SELECT_PATTERN -> sequencer.selectPattern(value);
                case TOGGLE_PROGRAM -> sequencer.toggleProgramming();
                case TRACK_COLOUR -> sequencer.setTrackColour(value);
                case PAINT_NOTE -> sequencer.paintNote(value / 400, (value / 25) % 16, value % 25, extra);
                default -> {}
            }
        } else if (world.getBlockEntity(pos) instanceof ControllerTileEntity controller) {
            switch (action) {
                case SET_BPM -> controller.setBeatsPerMinute(value);
                case TOGGLE_PLAY -> controller.togglePlaying();
                case SEQUENCER_PATTERN -> controller.selectPattern(value, extra);
                default -> {}
            }
        }
    }
}
