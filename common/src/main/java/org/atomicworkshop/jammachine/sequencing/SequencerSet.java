package org.atomicworkshop.jammachine.sequencing;

import com.google.common.collect.Lists;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.UUID;
import java.util.function.Consumer;

public class SequencerSet implements Iterable<Sequencer>
{
    private int beatsPerMinute;
    private List<Sequencer> sequencers = Lists.newArrayList();

    private final Object listLock = new Object();
    private final UUID id;

    public SequencerSet(UUID id)
    {
        this.id = id;
    }

    public void updateBpm()
    {
        for (final Sequencer sequencer : sequencers) {
            //FIXME: (Steven) solve this properly using a controller block
            if (sequencer.getBeatsPerMinute() > 0) {
                beatsPerMinute = Math.max(sequencer.getBeatsPerMinute(), beatsPerMinute);
            }
        }
    }

    public int getBeatsPerMinute()
    {
        return beatsPerMinute;
    }

    @Override
    public Iterator<Sequencer> iterator()
    {
        return sequencers.iterator();
    }

    @Override
    public void forEach(Consumer<? super Sequencer> consumer)
    {
        sequencers.forEach(consumer);
    }

    @Override
    public Spliterator<Sequencer> spliterator()
    {
        return sequencers.spliterator();
    }

    public UUID getId()
    {
        return id;
    }

    public void addSequencer(Sequencer sequencer)
    {
        synchronized (listLock) {
            List<Sequencer> sequencers = Lists.newArrayList(this.sequencers);
            sequencers.add(sequencer);
            this.sequencers = sequencers;
        }
    }

    public void removingSequencer(Sequencer sequencer) {
        synchronized (listLock) {
            List<Sequencer> sequencers = Lists.newArrayList(this.sequencers);
            sequencers.remove(sequencer);
            this.sequencers = sequencers;
        }
    }

    public void readFromNBT(CompoundTag compoundTag)
    {

    }

    public Tag writeToNBT()
    {
        return null;
    }

    public void marchInterval()
    {

    }

    public int getCurrentPatternIndexForSequencer(UUID id)
    {
        for (final Sequencer sequencer : sequencers)
        {
            if (sequencer.getId().equals(id)) {
                return sequencer.getCurrentPatternIndex();
            }
        }
        return 0;
    }
}