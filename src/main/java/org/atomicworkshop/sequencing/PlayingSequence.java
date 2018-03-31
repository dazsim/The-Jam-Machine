package org.atomicworkshop.sequencing;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.BlockNote;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.NoteBlockEvent.Play;

class PlayingSequence
{
	private final SequencerSet sequencerSet;
	private long nextIntervalMilliseconds;
	private int interval = -1;

	private int noteBlockSearch;


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
		++interval;
		if (interval >= 16) {
			interval = 0;
			//Setting the noteBlockSearch to 0 here ensures that the sounds played per pattern are deterministic
			//for a given pattern
			noteBlockSearch = 0;
		}

		for (final Sequencer sequencer : sequencerSet)
		{
			playSequencerSet(sequencer);
		}
	}

	private void playSequencerSet(Sequencer sequencer)
	{
		if ((interval & 3) == 0) {
			sequencer.updatePendingPattern();
		}

		final ImmutableList<AdjacentNoteBlock> availableNoteBlocks = sequencer.getAvailableNoteBlocks();
		if (availableNoteBlocks.isEmpty()) return;

		final Pattern currentPattern = sequencer.getCurrentPattern();
		for (final Byte pitchToPlay : currentPattern.getPitchesAtInterval(interval))
		{
			++noteBlockSearch;
			if (noteBlockSearch >= availableNoteBlocks.size()) {
				noteBlockSearch %= availableNoteBlocks.size();
			}
			final AdjacentNoteBlock noteBlock = availableNoteBlocks.get(noteBlockSearch);

			playNote(sequencer, noteBlock, pitchToPlay);
		}
	}

	private void playNote(Sequencer sequencer, AdjacentNoteBlock noteBlock, Byte pitchToPlay)
	{
		final BlockPos sequencerBlockPos = sequencer.getBlockPos();
		final BlockPos pos = sequencerBlockPos.offset(noteBlock.getDirection());
		final int instrumentId = noteBlock.getInstrument().ordinal();

		//Schedule sound to be played
		final World world = sequencer.getWorld();
		final Play e = new Play(world, pos, world.getBlockState(pos), pitchToPlay, instrumentId);
		if (MinecraftForge.EVENT_BUS.post(e)) return;

		final int playingInstrumentId = e.getInstrument().ordinal();
		final int playingNoteId = e.getVanillaNoteId();
		final float pitch = (float) StrictMath.pow(2.0D, (playingNoteId - 12) / 12.0D);
		final SoundEvent instrument = getInstrument(playingInstrumentId);

		final double x = pos.getX() + 0.5;
		final double y = pos.getY() + 0.5;
		final double z = pos.getZ() + 0.5;
		world.playSound(x, y, z, instrument, SoundCategory.RECORDS, 3.0F, pitch, false);

		//Trigger a note particle at location
		world.spawnParticle(EnumParticleTypes.NOTE, x, y + 0.7D, z, playingNoteId / 24.0D, 0.0D, 0.0D);
	}

	private static SoundEvent getInstrument(int eventId)
	{
		if (eventId < 0 || eventId >= BlockNote.INSTRUMENTS.size())
		{
			eventId = 0;
		}

		return BlockNote.INSTRUMENTS.get(eventId);
	}

	SequencerSet getSequencerSet()
	{
		return sequencerSet;
	}
}
