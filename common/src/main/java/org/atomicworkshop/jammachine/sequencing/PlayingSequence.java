package org.atomicworkshop.jammachine.sequencing;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;

class PlayingSequence
{
    private final SequencerSet sequencerSet;
    private long nextIntervalMilliseconds;


    PlayingSequence(SequencerSet sequencerSet)
    {
        this.sequencerSet = sequencerSet;
    }

    long getBeatsPerMinute()
    {
        return sequencerSet.getBeatsPerMinute();
    }

    long getNextIntervalMillis()
    {
        return nextIntervalMilliseconds;
    }

    void setNextIntervalMillis(long milliseconds)
    {
        nextIntervalMilliseconds = milliseconds;
    }

    void playNextInterval()
    {
        for (final Sequencer sequencer : sequencerSet)
        {
            if (sequencer.incrementInterval()) {
                sequencerSet.marchInterval();
                final int currentPatternIndexForSequencer = sequencerSet.getCurrentPatternIndexForSequencer(sequencer.getId());
                sequencer.setCurrentPatternIndex(currentPatternIndexForSequencer);
            }

            playSequencer(sequencer);
        }
    }

    private void playSequencer(Sequencer sequencer)
    {
        if (sequencer.getAvailableNoteBlocks().isEmpty()) return;
        final ImmutableList<AdjacentNoteBlock> touching = sequencer.getTouchingNoteBlocks();

        final Pattern currentPattern = sequencer.getCurrentPattern();
        final int interval = sequencer.getCurrentInterval();
        for (final Byte pitchToPlay : currentPattern.getPitchesAtInterval(interval))
        {
            if (!touching.isEmpty()) {
                MusicPlayer.playNote(sequencer, touching.get(sequencer.incrementNoteBlockNumber()), pitchToPlay);
            }
            for (final AdjacentNoteBlock noteBlock : sequencer.getNoteBlocksOn(sequencer.colourOf(currentPattern, interval, pitchToPlay))) {
                MusicPlayer.playNote(sequencer, noteBlock, pitchToPlay);
            }
        }
    }

    void join(SequencerSet other)
    {
        Sequencer leader = null;
        for (final Sequencer sequencer : sequencerSet) {
            leader = sequencer;
            break;
        }
        for (final Sequencer sequencer : other) {
            if (sequencer.isIn(sequencerSet)) continue;
            sequencer.reset();
            if (leader != null) {
                sequencer.syncTo(leader);
            }
            sequencerSet.addSequencer(sequencer);
        }
        sequencerSet.updateBpm();
    }

    SequencerSet getSequencerSet()
    {
        return sequencerSet;
    }
}