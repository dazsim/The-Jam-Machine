package org.atomicworkshop.jammachine.sequencing;

import org.atomicworkshop.jammachine.JamMachineMod;
import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

public class JamController extends SequencerSet
{
    private final Level world;
    private final BlockPos pos;
    private final List<ControllerPattern> sequencerPatternSections = Lists.newArrayList();
    private int sequenceInterval = 0;

    public JamController(Level world, BlockPos pos)
    {
        super(UUID.randomUUID());
        this.world = world;
        this.pos = pos;
    }

    @Override
    public void addSequencer(Sequencer sequencer)
    {
        if (sequencer.getWorld().dimension() != world.dimension()) {
            JamMachineMod.LOGGER.warn("SequencerSet world {} vs Sequencer world {}",
                    world.dimension(),
                    sequencer.getWorld().dimension()
            );
        }

        super.addSequencer(sequencer);
    }

    public Level getWorld()
    {
        return world;
    }

    public BlockPos getPos()
    {
        return pos;
    }

    @Override
    public void marchInterval()
    {
        sequenceInterval++;
    }

    @Override
    public int getCurrentPatternIndexForSequencer(UUID id)
    {
        for (final ControllerPattern section : sequencerPatternSections)
        {
            if (section.getSequencerId().equals(id)) {

                return section.getPatternAtInterval(sequenceInterval);
            }
        }

        return 0;
    }

    private long lastUpdateIndex = Long.MIN_VALUE;
    public void findSequencers()
    {
        if (lastUpdateIndex == MusicPlayer.getLastUpdateIndex()) return;
        lastUpdateIndex = MusicPlayer.getLastUpdateIndex();

        for (final ControllerPattern sequencerPatternSection : sequencerPatternSections)
        {
            if (sequencerPatternSection.locatedSequencer == null) {
                final Sequencer sequencer = MusicPlayer.getSequencerById(sequencerPatternSection.sequencerId);
                if (sequencer != null) {
                    sequencerPatternSection.locatedSequencer = sequencer;
                }
            }
        }
    }

    public ControllerPattern getControllerPatternForId(UUID id)
    {
        for (final ControllerPattern sequencerPatternSection : sequencerPatternSections)
        {
            if (sequencerPatternSection.isForLoadedSequencer(id)) {
                return sequencerPatternSection;
            }
        }
        return null;
    }

    public int getSequenceInterval() {
        return sequenceInterval;
    }

    public void setSequenceInterval(int sequenceInterval) {
        this.sequenceInterval = sequenceInterval;
    }
}