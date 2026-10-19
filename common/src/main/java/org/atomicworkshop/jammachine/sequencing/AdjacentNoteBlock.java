package org.atomicworkshop.jammachine.sequencing;

import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class AdjacentNoteBlock
{
    private NoteBlockInstrument instrument = null;
    private final Direction direction;
    private final BlockPos pos;

    AdjacentNoteBlock(Direction direction)
    {
        this.direction = direction;
        this.pos = null;
    }

    //a note block reached over a cable
    AdjacentNoteBlock(BlockPos pos, NoteBlockInstrument instrument)
    {
        this.direction = null;
        this.pos = pos;
        this.instrument = instrument;
    }

    public BlockPos getPos(BlockPos sequencerPos)
    {
        return pos != null ? pos : sequencerPos.relative(direction);
    }

    boolean canPlay()
    {
        return instrument != null;
    }

    public NoteBlockInstrument getInstrument()
    {
        return instrument;
    }

    public void setInstrument(NoteBlockInstrument instrument) {
        this.instrument = instrument;
    }

    public Direction getDirection()
    {
        return direction;
    }
}