package org.atomicworkshop.jammachine.sequencing;

import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.core.Direction;

public class AdjacentNoteBlock
{
    private NoteBlockInstrument instrument = null;
    private final Direction direction;

    AdjacentNoteBlock(Direction direction)
    {
        this.direction = direction;
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