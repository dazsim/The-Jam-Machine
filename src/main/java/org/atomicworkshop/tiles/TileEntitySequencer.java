package org.atomicworkshop.tiles;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.world.NoteBlockEvent.Instrument;
import org.atomicworkshop.Reference.NBT;
import org.atomicworkshop.sequencing.MusicPlayer;
import org.atomicworkshop.sequencing.Pattern;
import org.atomicworkshop.sequencing.Sequencer;
import org.atomicworkshop.sequencing.SequencerSet;
import javax.annotation.Nullable;
import java.util.UUID;

import static net.minecraftforge.event.world.NoteBlockEvent.Instrument.*;

public class TileEntitySequencer extends TileEntity implements ITickable
{

	public TileEntitySequencer()
	{
		sequencerSetId = UUID.randomUUID();
	}

	private UUID sequencerSetId;

	private Sequencer sequencer = null;
	private boolean isPlaying;

	private boolean hasSynchronizer()
	{
		return false;
	}

	@Override
	public void onChunkUnload()
	{
		stopPlaying();
	}

	@Override
	public void readFromNBT(NBTTagCompound compound)
	{
		super.readFromNBT(compound);
		isPlaying = compound.getBoolean(NBT.isPlaying);

		sequencerSetId = compound.getUniqueId(NBT.songId);
		if (sequencerSetId.equals(UUID.fromString("00000000-0000-0000-0000-000000000000"))) {
			sequencerSetId = UUID.randomUUID();
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound)
	{
		super.writeToNBT(compound);

		compound.setBoolean(NBT.isPlaying, isPlaying);
		compound.setUniqueId(NBT.songId, sequencerSetId);

		return compound;
	}

	@Nullable
	@Override
	public SPacketUpdateTileEntity getUpdatePacket()
	{
		final NBTTagCompound updateTag = getUpdateTag();
		return new SPacketUpdateTileEntity(pos, 0, updateTag);
	}

	@Override
	public NBTTagCompound getUpdateTag()
	{
		return writeToNBT(new NBTTagCompound());
	}

	@Override
	public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt)
	{
		super.onDataPacket(net, pkt);
		final boolean wasPlaying = isPlaying;
		handleUpdateTag(pkt.getNbtCompound());

		if (isPlaying && !wasPlaying) {
			startPlaying();
			sendUpdates();
		} else if (!isPlaying && wasPlaying) {
			stopPlaying();
			sendUpdates();
		}
	}

	private void startPlaying()
	{
		//TODO: Only create a demo song if there isn't any pattern data attached to this TE.
		createDemoSong();

		//TODO: Find Synchronizer and get the Sequencer set for it.
		final SequencerSet demoSong;

		if (!hasSynchronizer()) {
			demoSong = new SequencerSet(world, sequencerSetId);
		} else {
			//TODO: Resolve SequencerSet from sequencer
			demoSong = new SequencerSet(world, sequencerSetId);
		}
		demoSong.addSequencer(sequencer);

		demoSong.updateBpm();

		for (final EnumFacing horizontal : EnumFacing.HORIZONTALS)
		{
			verifyNoteBlockFacing(horizontal);
		}

		MusicPlayer.playSong(demoSong);
	}

	private void sendUpdates() {
		world.markBlockRangeForRenderUpdate(pos, pos);
		final IBlockState state = world.getBlockState(pos);
		world.notifyBlockUpdate(pos, state, state, 3);
		world.scheduleBlockUpdate(pos, getBlockType(),0,0);
		markDirty();
	}

	public void stopPlaying()
	{
		MusicPlayer.stopPlaying(new SequencerSet(world, sequencerSetId));
	}

	public void notifyPowered(boolean powered)
	{
		if (isPlaying != powered) {
			isPlaying = powered;
			sendUpdates();
		}
	}


	private void createDemoSong()
	{
		if (world == null) return;

		sequencer = new Sequencer(world, pos);
		sequencer.setDesiredBPM(120);

		final int pattern = world.rand.nextInt(4);
		final Pattern demoPattern = new Pattern();
		if (pattern == 0)
		{
//			sequencer.setAdjacentNoteBlock(EnumFacing.NORTH, PIANO);
//			sequencer.setAdjacentNoteBlock(EnumFacing.SOUTH, BASSGUITAR);
//			sequencer.setAdjacentNoteBlock(EnumFacing.EAST, PIANO);

			demoPattern.setPitchAtInternal(0, 6);
			demoPattern.setPitchAtInternal(1, 10);
			demoPattern.setPitchAtInternal(2, 13);

			demoPattern.setPitchAtInternal(4, 8);
			demoPattern.setPitchAtInternal(5, 11);
			demoPattern.setPitchAtInternal(6, 15);

			demoPattern.setPitchAtInternal(8, 10);
			demoPattern.setPitchAtInternal(9, 13);
			demoPattern.setPitchAtInternal(10, 17);

			demoPattern.setPitchAtInternal(12, 11);
			demoPattern.setPitchAtInternal(13, 15);
			demoPattern.setPitchAtInternal(14, 18);

		} else if (pattern == 1) {
//			sequencer.setAdjacentNoteBlock(EnumFacing.NORTH, BASSDRUM);

			demoPattern.setPitchAtInternal(0, 6);
			demoPattern.setPitchAtInternal(8, 6);

		} else if (pattern == 2) {
//			sequencer.setAdjacentNoteBlock(EnumFacing.NORTH, Instrument.SNARE);

			demoPattern.setPitchAtInternal(4, 6);
			demoPattern.setPitchAtInternal(12, 6);
		} else if (pattern == 3) {
//			sequencer.setAdjacentNoteBlock(EnumFacing.NORTH, Instrument.CLICKS);

			demoPattern.setPitchAtInternal(1, 14);
			demoPattern.setPitchAtInternal(2, 18);
			demoPattern.setPitchAtInternal(3, 5);

			demoPattern.setPitchAtInternal(5, 14);
			demoPattern.setPitchAtInternal(6, 18);
			demoPattern.setPitchAtInternal(7, 5);

			demoPattern.setPitchAtInternal(9, 16);
			demoPattern.setPitchAtInternal(10, 18);
			demoPattern.setPitchAtInternal(11, 5);

			demoPattern.setPitchAtInternal(13, 14);
			demoPattern.setPitchAtInternal(14, 18);
			demoPattern.setPitchAtInternal(15, 5 );
		}

		sequencer.setPattern(0, demoPattern);
	}


	@Override
	public void update()
	{
		if (sequencer == null) return;

		final int blockToCheck = (int)(world.getTotalWorldTime() & 3);
		final EnumFacing facing = EnumFacing.getHorizontal(blockToCheck);

		verifyNoteBlockFacing(facing);
	}

	private void verifyNoteBlockFacing(EnumFacing facing)
	{
		BlockPos offset = pos.offset(facing.getOpposite());
		final IBlockState noteBlockState = world.getBlockState(offset);
		final Instrument instrument;
		if (noteBlockState.getBlock() != Blocks.NOTEBLOCK) {
			instrument = null;
		} else {
			final IBlockState instrumentBlockState = world.getBlockState(offset.down());
			instrument = getInstrumentFromBlockState(instrumentBlockState);
		}
		final Instrument instrumentFromNoteBlock = sequencer.getInstrumentFromNoteBlock(facing);

		if (instrument != instrumentFromNoteBlock) {
			sequencer.setAdjacentNoteBlock(facing, instrument);
			sendUpdates();
		}
	}

	private static Instrument getInstrumentFromBlockState(IBlockState state)
	{
		//Blatantly ripped from TileEntityNote
		final Material material = state.getMaterial();

		if (material == Material.ROCK)
		{
			return BASSDRUM;
		}

		if (material == Material.SAND)
		{
			return SNARE;
		}

		if (material == Material.GLASS)
		{
			return CLICKS;
		}

		if (material == Material.WOOD)
		{
			return BASSGUITAR;
		}

		final Block block = state.getBlock();

		if (block == Blocks.CLAY)
		{
			return FLUTE;
		}

		if (block == Blocks.GOLD_BLOCK)
		{
			return BELL;
		}

		if (block == Blocks.WOOL)
		{
			return GUITAR;
		}

		if (block == Blocks.PACKED_ICE)
		{
			return CHIME;
		}

		if (block == Blocks.BONE_BLOCK)
		{
			return XYLOPHONE;
		}
		return PIANO;
	}
}
