package org.atomicworkshop.jammachine.sequencing;

import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

public final class MusicPlayer
{
    private static final List<PlayingSequence> playingSequences = Lists.newArrayList();
    private static final Object sequenceLock = new Object();
    private static long lastUpdateIndex = Long.MIN_VALUE;

    public static void playSong(SequencerSet sequencerSet)
    {
        if (sequencerSet == null) return;
        sequencerSet.updateBpm();
        if (sequencerSet.getBeatsPerMinute() == 0) return;
        final long currentTimeMillis = System.nanoTime() / 1000000;

        synchronized(sequenceLock) {
            for (final PlayingSequence playingSequence : playingSequences)
            {
                if (playingSequence.getSequencerSet().getId().equals(sequencerSet.getId())) return;
            }

            final PlayingSequence e = new PlayingSequence(sequencerSet);

            e.setNextIntervalMillis(currentTimeMillis + (250 / (e.getBeatsPerMinute() / 60)));

            for (final Sequencer sequencer : sequencerSet)
            {
                sequencer.reset();
            }

            playingSequences.add(e);
        }
    }

    public static void stopPlaying(UUID sequencerSetId) {
        if (sequencerSetId == null) return;
        synchronized (sequenceLock) {
            playingSequences.removeIf(playingSequence -> playingSequence.getSequencerSet().getId().equals(sequencerSetId));
        }
    }

    public static void onWorldUnloaded(LevelAccessor unloaded) {
        if (!(unloaded instanceof Level level)) return;
        final Object dimensionId = level.dimension();
        synchronized (sequenceLock) {
            playingSequences.removeIf(ps ->
                    StreamSupport
                            .stream(ps.getSequencerSet().spliterator(), true)
                            .anyMatch(sequencer -> sequencer.getWorld().dimension() == dimensionId)
            );

        }
        synchronized (trackingLock) {
            trackedControllers.removeIf(controller ->
                    controller.getWorld().dimension() == dimensionId);
            trackedSequencers.removeIf(controller ->
                    controller.getWorld().dimension() == dimensionId);
        }
    }

    public static void onClientTick() {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isPaused()) return;
        if (minecraft.level == null) {
            synchronized (sequenceLock) {
                playingSequences.clear();
            }
            return;
        }

        final long currentTimeMillis = System.nanoTime() / 1000000;

        synchronized (sequenceLock) {
            if (playingSequences.isEmpty()) return;

            for (final PlayingSequence playingSequence : playingSequences)
            {
                if (playingSequence.getSequencerSet().getBeatsPerMinute() == 0) {
                    continue;
                }
                final long nextIntervalMillis = playingSequence.getNextIntervalMillis();
                if (currentTimeMillis >= nextIntervalMillis) {
                    final long millisToNextInterval = 250 / (playingSequence.getBeatsPerMinute() / 60);
                    playingSequence.setNextIntervalMillis(currentTimeMillis + millisToNextInterval);
                    playingSequence.playNextInterval();
                }
            }
        }
    }

    public static void playNote(Sequencer sequencer, AdjacentNoteBlock noteBlock, Byte pitchToPlay)
    {
        final BlockPos sequencerBlockPos = sequencer.getBlockPos();
        final BlockPos pos = sequencerBlockPos.relative(noteBlock.getDirection());

        final Level world = sequencer.getWorld();
        final NoteBlockInstrument instrument = noteBlock.getInstrument();
        if (world == null || instrument == null) return;

        final int playingNoteId = pitchToPlay;
        final float pitch = (float) StrictMath.pow(2.0D, (playingNoteId - 12) / 12.0D);
        final SoundEvent sound = instrument.getSoundEvent().value();

        final double x = pos.getX() + 0.5;
        final double y = pos.getY() + 0.5;
        final double z = pos.getZ() + 0.5;
        world.playLocalSound(x, y, z, sound, SoundSource.RECORDS, 3.0F, pitch, false);

        //Trigger a note particle at location
        world.addParticle(ParticleTypes.NOTE, x, y + 0.7D, z, playingNoteId / 24.0D, 0.0D, 0.0D);
    }

    private static final List<Sequencer> trackedSequencers = Lists.newArrayList();
    private static final List<JamController> trackedControllers = Lists.newArrayList();
    private static final Object trackingLock = new Object();

    public static void stopTrackingSequencerAt(Level worldIn, BlockPos pos)
    {
        synchronized (trackingLock)
        {
            lastUpdateIndex++;
            trackedSequencers.removeIf(sequencer ->
                    sequencer.getWorld().dimension() == worldIn.dimension() &&
                            pos.equals(sequencer.getBlockPos()));
        }
    }

    public static void stopTrackingControllerAt(Level worldIn, BlockPos pos)
    {
        synchronized (trackingLock)
        {
            lastUpdateIndex++;
            trackedControllers.removeIf(controller ->
                    controller.getWorld().dimension() == worldIn.dimension() &&
                            pos.equals(controller.getPos()));
        }
    }

    public static void startTracking(JamController controller)
    {
        synchronized (trackingLock)
        {
            lastUpdateIndex++;
            for (final JamController trackedController : trackedControllers)
            {
                if (trackedController.getId().equals(controller.getId())) {
                    return;
                }
            }
            trackedControllers.add(controller);
        }
    }

    public static void startTracking(Sequencer sequencer)
    {
        synchronized (trackingLock)
        {
            lastUpdateIndex++;
            for (final Sequencer trackedSequencer : trackedSequencers)
            {
                if (trackedSequencer.getId().equals(sequencer.getId())) {
                    return;
                }
            }
            trackedSequencers.add(sequencer);
        }
    }

    public static Sequencer getSequencerById(UUID sequencerId)
    {
        synchronized (trackingLock)
        {
            for (final Sequencer trackedSequencer : trackedSequencers)
            {
                if (trackedSequencer.getId().equals(sequencerId)) {
                    return trackedSequencer;
                }
            }
        }
        return null;
    }

    public static long getLastUpdateIndex()
    {
        synchronized (trackingLock)
        {
            return lastUpdateIndex;
        }
    }
}
