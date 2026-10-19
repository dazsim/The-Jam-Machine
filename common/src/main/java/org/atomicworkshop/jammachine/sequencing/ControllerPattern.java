package org.atomicworkshop.jammachine.sequencing;

import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.UUID;

public class ControllerPattern
{
    UUID sequencerId;
    Sequencer locatedSequencer;
    BlockPos expectedSequencerLocation;
    List<Byte> patternAtInterval = Lists.newArrayList();

    public UUID getSequencerId()
    {
        return sequencerId;
    }

    public byte getPatternAtInterval(int currentInterval)
    {
        if (currentInterval < patternAtInterval.size()) {
            return patternAtInterval.get(currentInterval);
        }
        return 0;
    }

    public boolean isForLoadedSequencer(UUID id)
    {
        return locatedSequencer != null && locatedSequencer.getId().equals(id);
    }

    public Sequencer getSequencer() {
        return locatedSequencer;
    }
}