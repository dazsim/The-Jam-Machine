package org.atomicworkshop.jammachine.sequencing;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.primitives.Bytes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.core.UUIDUtil;
import org.atomicworkshop.jammachine.JamMachineMod;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static org.atomicworkshop.jammachine.Reference.*;

public class Sequencer
{
    private Level world;
    private BlockPos blockPos;
    private final Pattern[] patterns;
    private final AdjacentNoteBlock[] adjacentNoteBlocks;
    private UUID id;
    private ImmutableList<AdjacentNoteBlock> currentAdjacentNoteBlocks = ImmutableList.of();
    private int beatsPerMinute;
    private int pendingPatternIndex;
    private int currentPatternIndex;
    private int currentInterval;
    private boolean isAtStart;
    private int noteBlockSearch;
    private String name;
    private boolean isProgramming;
    private LinkedList<Integer> programList = new LinkedList<>();
    private int programIndex;

    public Sequencer(Level world, BlockPos blockPos)
    {
        this.world = world;
        this.blockPos = blockPos;
        adjacentNoteBlocks = new AdjacentNoteBlock[] {
                new AdjacentNoteBlock(Direction.NORTH),
                new AdjacentNoteBlock(Direction.EAST),
                new AdjacentNoteBlock(Direction.SOUTH),
                new AdjacentNoteBlock(Direction.WEST),
        };

        patterns = new Pattern[8];
        for (int i = 0; i < 8; i++)
        {
            patterns[i] = new Pattern();
        }

        beatsPerMinute = 120;

        id = UUID.randomUUID();
        isAtStart = true;
    }

    private void setAdjacentNoteBlock(Direction direction, NoteBlockInstrument sound)
    {
        adjacentNoteBlocks[direction.get2DDataValue()].setInstrument(sound);
        final List<AdjacentNoteBlock> availableNoteBlocks = Lists.newArrayList();
        for (final AdjacentNoteBlock adjacentNoteBlock : adjacentNoteBlocks)
        {
            if (adjacentNoteBlock.canPlay())
            {
                availableNoteBlocks.add(adjacentNoteBlock);
            }
        }
        currentAdjacentNoteBlocks = ImmutableList.copyOf(availableNoteBlocks);
    }

    private NoteBlockInstrument getInstrumentFromNoteBlock(Direction direction) {
        return adjacentNoteBlocks[direction.get2DDataValue()].getInstrument();
    }

    BlockPos getBlockPos()
    {
        return blockPos;
    }

    public UUID getId() {
        return id;
    }

    public void setDesiredBPM(int beatsPerMinute)
    {
        JamMachineMod.LOGGER.info("Setting to BPM to {}", beatsPerMinute);
        this.beatsPerMinute = beatsPerMinute;
    }

    public int getBeatsPerMinute()
    {
        return beatsPerMinute;
    }

    public int getPendingPatternIndex()
    {
        return pendingPatternIndex;
    }

    public void setPendingPatternIndex(int pendingPatternIndex)
    {
        if (pendingPatternIndex > patterns.length) {
            pendingPatternIndex %= patterns.length;
        }
        if (isProgramming) {
            programList.add(pendingPatternIndex);
        } else {
            if (this.pendingPatternIndex != pendingPatternIndex) {
                JamMachineMod.LOGGER.info("Setting to Pending Pattern {}", currentPatternIndex);
                this.pendingPatternIndex = pendingPatternIndex;
            }
        }
    }

    public void setCurrentPatternIndex(int currentPatternIndex)
    {
        if (currentPatternIndex > patterns.length) {
            currentPatternIndex %= patterns.length;
        }

        if (this.currentPatternIndex != currentPatternIndex) {
            JamMachineMod.LOGGER.info("Setting to Current Pattern {}", currentPatternIndex);
            this.currentPatternIndex = currentPatternIndex;
        }
    }

    public int getCurrentPatternIndex()
    {
        return currentPatternIndex;
    }

    public void updatePendingPattern()
    {
        if (isProgramming) {
            programIndex++;
            if (programIndex >= programList.size()) {
                programIndex = 0;
            }
            currentPatternIndex = programList.get(0);

            int nextProgramIndex = programIndex + 1;
            if (nextProgramIndex >= programList.size()) {
                nextProgramIndex = 0;
            }

            pendingPatternIndex = programList.get(nextProgramIndex);
        } else {
            currentPatternIndex = pendingPatternIndex;
        }
    }

    public Pattern getCurrentPattern()
    {
        return patterns[currentPatternIndex];
    }

    public ImmutableList<AdjacentNoteBlock> getAvailableNoteBlocks()
    {
        return currentAdjacentNoteBlocks;
    }

    public Level getWorld()
    {
        return world;
    }

    public void readFromNBT(CompoundTag compound)
    {
        id = compound.read(NBT.sequencerId, UUIDUtil.CODEC).orElse(EMPTY_UUID);
        if (id.equals(EMPTY_UUID)) {
            id = UUID.randomUUID();
        }
        beatsPerMinute = compound.getIntOr(NBT.beatsPerMinute, 0);

        currentPatternIndex = compound.getIntOr(NBT.currentPatternIndex, 0);
        pendingPatternIndex = compound.getIntOr(NBT.pendingPatternIndex, 0);
        name = compound.getStringOr(NBT.name, "");

        final ListTag nbtPatterns = compound.getListOrEmpty(NBT.pattern);

        for (int patternIndex = 0; patternIndex < patterns.length; patternIndex++)
        {
            final CompoundTag patternNBT = nbtPatterns.getCompoundOrEmpty(patternIndex);
            final Pattern pattern = patterns[patternIndex];

            for (int interval = 0; interval < 16; interval++)
            {
                final byte[] pitchesAtInterval = patternNBT.getByteArray(String.valueOf(interval)).orElse(new byte[0]);

                for (int i = 0; i < 25; i++)
                {
                    pattern.resetPitchAtInterval(interval, i);
                }

                for (final byte pitch : pitchesAtInterval)
                {

                    pattern.setPitchAtInterval(interval, pitch);
                }
            }
        }

        programList.clear();
        for (final int programPatternIndex : compound.getIntArray(NBT.program).orElse(new int[0])) {
            programList.add(programPatternIndex);
        }
        isProgramming = compound.getBooleanOr(NBT.isProgramming, false);
    }

    public CompoundTag writeToNBT()
    {
        final CompoundTag tagCompound = new CompoundTag();
        tagCompound.store(NBT.sequencerId, UUIDUtil.CODEC, id);
        tagCompound.putInt(NBT.beatsPerMinute, beatsPerMinute);
        tagCompound.putInt(NBT.currentPatternIndex, currentPatternIndex);
        tagCompound.putInt(NBT.pendingPatternIndex, pendingPatternIndex);
        if (name != null) {
            tagCompound.putString(NBT.name, name);
        }

        final ListTag patternList = new ListTag();
        for (int patternIndex = 0; patternIndex < patterns.length; patternIndex++)
        {
            final Pattern pattern = patterns[patternIndex];

            final CompoundTag patternNBT = new CompoundTag();

            for (int interval = 0; interval < 16; interval++)
            {
                final Iterable<Byte> pitchesAtInterval = pattern.getPitchesAtInterval(interval);
                final ArrayList<Byte> pitchList = Lists.newArrayList(pitchesAtInterval);
                if (!pitchList.isEmpty()) {
                    final ByteArrayTag pitchArray = new ByteArrayTag(Bytes.toArray(pitchList));
                    patternNBT.put(String.valueOf(interval), pitchArray);
                }
            }

            patternList.add(patternNBT);
        }
        tagCompound.put(NBT.pattern, patternList);

        if (!programList.isEmpty()) {
            final int[] programPatternIndices = new int[programList.size()];
            int index = 0;
            for (final Integer programPatternIndex : programList) {
                programPatternIndices[index++] = programPatternIndex;
            }

            tagCompound.putIntArray(NBT.program, programPatternIndices);
        }

        tagCompound.putBoolean(NBT.isProgramming, isProgramming);

        return tagCompound;
    }

    public int getCurrentInterval()
    {
        return currentInterval;
    }

    public void setCurrentInterval(int currentInterval)
    {
        this.currentInterval = currentInterval;
    }

    public int incrementNoteBlockNumber()
    {
        ++noteBlockSearch;
        while (noteBlockSearch >= currentAdjacentNoteBlocks.size()) {
            noteBlockSearch -= currentAdjacentNoteBlocks.size();
        }
        return noteBlockSearch;
    }

    public boolean incrementInterval()
    {
        if (isAtStart) {
            isAtStart = false;
            return false;
        }

        boolean marchControllerPattern = false;
        ++currentInterval;
        if (currentInterval >= 16) {
            currentInterval = 0;
            //Setting the noteBlockSearch to 0 here ensures that the sounds played per pattern are deterministic
            //for a given pattern
            noteBlockSearch = 0;

            marchControllerPattern = true;
        }

        if (currentInterval == 0) {

            updatePendingPattern();
        }

        return marchControllerPattern;
    }

    public boolean verifyNoteBlockFacing(Direction facing)
    {
        final BlockPos offset = blockPos.relative(facing.getOpposite());
        final BlockState noteBlockState = world.getBlockState(offset);

        @Nullable
        final NoteBlockInstrument instrument;
        if (Blocks.NOTE_BLOCK.equals(noteBlockState.getBlock()))
        {
            instrument = noteBlockState.getValue(NoteBlock.INSTRUMENT);
        } else
        {
            instrument = null;
        }

        final NoteBlockInstrument instrumentFromNoteBlock = getInstrumentFromNoteBlock(facing);

        if (instrument != instrumentFromNoteBlock) {
            setAdjacentNoteBlock(facing, instrument);
            return true;
        }
        return false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProgramming(boolean isProgramming)
    {
        this.isProgramming = isProgramming;
        programList.clear();
        programList.add(currentPatternIndex < 0 ? 0 : currentPatternIndex);
    }

    public boolean isProgramming()
    {
        return isProgramming;
    }

    public boolean isPlaying() {
        return !this.isAtStart || this.currentInterval != 0;
    }

    public void reset()
    {
        isAtStart = true;
        programIndex = 0;
        currentInterval = 0;

        if (isProgramming && !programList.isEmpty()) {
            currentPatternIndex = programList.getFirst();
            if (programList.size() > 1) {
                pendingPatternIndex = programList.get(1);
            } else {
                pendingPatternIndex = currentPatternIndex;
            }
        } else {
            currentPatternIndex = 0;
            pendingPatternIndex = 0;
        }
    }

    public int getProgramLength() {
        return programList.size();
    }

    public void setWorld(Level world) {
        this.world = world;
    }

    public void setPos(BlockPos pos) {
        this.blockPos = pos;
    }
}