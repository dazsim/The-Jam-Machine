package org.atomicworkshop.jammachine.tiles;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.atomicworkshop.jammachine.ClientConfig;
import org.atomicworkshop.jammachine.JamMachineMod;
import org.atomicworkshop.jammachine.Reference;
import org.atomicworkshop.jammachine.Reference.NBT;
import org.atomicworkshop.jammachine.gui.SequencerContainer;
import org.atomicworkshop.jammachine.libraries.ItemLibrary;
import org.atomicworkshop.jammachine.libraries.TileEntityTypeLibrary;
import org.atomicworkshop.jammachine.sequencing.*;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public class SequencerTileEntity extends BlockEntity implements Nameable, MenuProvider {

    private static final int IS_PLAYING = 0;
    private static final int CHANGE_PATTERN = 1;
    private static final int RUN_PROGRAM = 2;
    private static final int ENABLE_NOTE = 3;
    private static final int DISABLE_NOTE = 4;
    private static final int CHANGE_BPM = 5;
    private boolean hasCard = false;
    private String customName;

    public SequencerTileEntity(BlockPos pos, BlockState state)
    {
        super(TileEntityTypeLibrary.sequencer_tile, pos, state);
        sequencerSetId = UUID.randomUUID();
        sequencer = new Sequencer(null, pos);
    }

    private UUID sequencerSetId;
    //TODO: fix privacy later, right now the TESR needs access

    public Sequencer sequencer;
    private boolean isPlaying;

    private boolean hasController()
    {
        return false;
    }
    public boolean getHasCard()
    {
        return hasCard;
    }

    public void setHasCard(boolean iHasCard)
    {
        hasCard = iHasCard;
        sendUpdates();
    }

    @Override
    public void setRemoved()
    {
        super.setRemoved();
        sequencer.setCurrentInterval(-1);
        MusicPlayer.stopPlaying(sequencerSetId);
        if (level != null) {
            MusicPlayer.stopTrackingSequencerAt(level, worldPosition);
        }
    }

    @Override
    public void setLevel(Level world) {
        super.setLevel(world);
        sequencer.setWorld(world);
        MusicPlayer.startTracking(sequencer);
    }

    @Override
    protected void loadAdditional(ValueInput compound)
    {
        super.loadAdditional(compound);

        final boolean wasPlaying = isPlaying;
        isPlaying = compound.getBooleanOr(NBT.isPlaying, false);
        hasCard = compound.getBooleanOr(NBT.hasCard, false);
        sequencerSetId = compound.read(NBT.songId, UUIDUtil.CODEC).orElse(Reference.EMPTY_UUID);

        if (sequencerSetId.equals(Reference.EMPTY_UUID)) {
            sequencerSetId = UUID.randomUUID();
        }

        compound.read(NBT.sequence, CompoundTag.CODEC).ifPresent(this::readSequence);
        this.customName = compound.getStringOr(NBT.name, null);

        updatePlayStatus(wasPlaying);
    }

    private void readCustomDataFromNBT(CompoundTag compound)
    {
        compound.getCompound(NBT.sequence).ifPresent(this::readSequence);
        this.customName = compound.getStringOr(NBT.name, null);
    }

    private void readSequence(CompoundTag compoundTag)
    {
        if (!compoundTag.isEmpty())
        {
            sequencer.readFromNBT(compoundTag);

            if (sequencer.getBeatsPerMinute() < 60) {
                sequencer.setDesiredBPM(120);
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput compound)
    {
        super.saveAdditional(compound);

        compound.putBoolean(NBT.isPlaying, isPlaying);
        compound.putBoolean(NBT.hasCard, hasCard);
        compound.store(NBT.songId, UUIDUtil.CODEC, sequencerSetId);

        if (hasCustomName()) {
            compound.putString(NBT.name, customName);
        }
        compound.store(NBT.sequence, CompoundTag.CODEC, sequencer.writeToNBT());
    }

    private CompoundTag writeCustomDataToNBT(CompoundTag compound)
    {
        if (hasCustomName()) {
            compound.putString(NBT.name, customName);
        }
        compound.put(NBT.sequence, sequencer.writeToNBT());
        return compound;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries)
    {
        return saveCustomOnly(registries);
    }

    private void updatePlayStatus(boolean wasPlaying)
    {
        if (isPlaying && !wasPlaying) {
            startPlaying();
        } else if (!isPlaying && wasPlaying) {
            stopPlaying();
        }
    }

    private void startPlaying()
    {
        if (level == null || !level.isClientSide()) return;

        //TODO: Find Synchronizer and get the Sequencer set for it.
        final SequencerSet sequencerSet;

        if (!hasController()) {
            sequencerSet = new SequencerSet(sequencerSetId);
        } else {
            //TODO: Resolve SequencerSet from sequencer
            sequencerSet = new SequencerSet(sequencerSetId);
        }

        sequencerSet.addSequencer(sequencer);

        sequencerSet.updateBpm();

        for (final Direction horizontal : Reference.HORIZONTALS)
        {
            sequencer.verifyNoteBlockFacing(horizontal);
        }

        MusicPlayer.playSong(sequencerSet);
    }

    private void sendUpdates() {
        if (level == null) return;

        final BlockState state = level.getBlockState(worldPosition);
        level.sendBlockUpdated(worldPosition, state, state, 3);
        setChanged();
    }

    public void stopPlaying()
    {
        sequencer.setCurrentInterval(-1);

        MusicPlayer.stopPlaying(sequencerSetId);
        if (level != null && !level.isClientSide()) {
            level.blockEvent(worldPosition, this.getBlockState().getBlock(), IS_PLAYING, 0);
        }
    }

    public void notifyPowered(boolean powered)
    {
        if (isPlaying != powered) {
            isPlaying = powered;
            level.blockEvent(worldPosition, this.getBlockState().getBlock(), IS_PLAYING, isPlaying ? 1 : 0);
            setChanged();
        }
    }

    @Override
    public boolean triggerEvent(int id, int type)
    {
        final int controlCode = id & 7;

        if (controlCode == IS_PLAYING) {
            final boolean wasPlaying = isPlaying;
            isPlaying = type != 0;
            updatePlayStatus(wasPlaying);
            return true;
        }
        if (controlCode == CHANGE_PATTERN) {
            sequencer.setPendingPatternIndex(type);
            if (!isPlaying)
            {
                sequencer.setCurrentPatternIndex(type);
            }
            return true;
        }
        if (controlCode == RUN_PROGRAM) {
            sequencer.setProgramming(type != 0);
            return true;
        }
        if (controlCode == CHANGE_BPM) {
            sequencer.setDesiredBPM(type);
            return true;
        }
        if (controlCode == ENABLE_NOTE) {
            sequencer.getCurrentPattern().setPitchAtInterval(id >> 4, type);
            if (!ClientConfig.USE_GUI && level.isClientSide()) {
                ImmutableList<AdjacentNoteBlock> availableNoteBlocks = sequencer.getAvailableNoteBlocks();
                if (!availableNoteBlocks.isEmpty()) {
                    AdjacentNoteBlock noteBlock = availableNoteBlocks.get(0);
                    MusicPlayer.playNote(sequencer, noteBlock, (byte) type);
                }
            }
            return true;
        }
        if (controlCode == DISABLE_NOTE) {
            sequencer.getCurrentPattern().resetPitchAtInterval(id >> 4, type);
            return true;
        }

        return false;
    }

    public static void tick(Level world, BlockPos pos, BlockState state, SequencerTileEntity tileEntity)
    {
        final int blockToCheck = (int)(world.getGameTime() & 3);
        final Direction facing = Direction.from2DDataValue(blockToCheck);

        tileEntity.sequencer.verifyNoteBlockFacing(facing);
    }

    public void loadFromCard(ItemStack heldItemStack)
    {
        final CustomData data = heldItemStack.get(DataComponents.CUSTOM_DATA);
        if (data != null)
        {
            readCustomDataFromNBT(data.copyTag());
        }
        if (!level.isClientSide())
        {
            sendUpdates();
        }
    }

    public ItemStack saveToCard() {
        final ItemStack i = new ItemStack(ItemLibrary.punchCardWritten,1);
        i.set(DataComponents.CUSTOM_DATA, CustomData.of(writeCustomDataToNBT(new CompoundTag())));

        return i;
    }

    public boolean checkPlayerInteraction(double x, double z, Player playerIn)
    {
        if (sequencer == null) {
            sequencer = new Sequencer(level, worldPosition);
            MusicPlayer.startTracking(sequencer);
        }

        if (x < 0 || x > 1 || z < 0 || z > 1) return false;

        if (checkSequenceGridInteraction(x, z)) return true;
        if (checkBankInteraction(x, z)) return true;
        if (checkBPMInteraction(x, z)) return true;
        if (checkRunButtonInteraction(x, z)) return true;
        if (checkCardSlotInteraction(x, z)) {
            ejectCard(playerIn.getX(), playerIn.getY(), playerIn.getZ());
            return true;
        }

        return false;
    }

    private boolean checkSequenceGridInteraction(double x, double z)
    {
        //Scale to button grid, these may not be exact because we're using item rendering instead of generating quads.
        //Minecraft probably applies additional scaling/transforms when rendering the item (they appear to be centered,
        //rather than top-left aligned as the code implies)
        x *= 28;
        x -= 2.5;
        z *= 30;
        z -= 2.4;
        JamMachineMod.LOGGER.info("adjusted hitlocation {},{}", x, z);

        JamMachineMod.LOGGER.info("checking player interaction at scaled {},{}", x, z);

        if (x >= 0 && x < 16 && z >= 0 && z < 25) {
            final int pitch = 24 - (int)z;
            final int interval = (int)x;

            final Pattern currentPattern = sequencer.getCurrentPattern();
            notifyPitchAtIntervalChanged(pitch, interval, !currentPattern.isPitchActiveAtInterval(pitch, interval));
            return true;
        }
        return false;
    }

    public void notifyPitchAtIntervalChanged(int pitch, int interval, boolean isEnabled) {
        //Hit a sequence button
        final Pattern currentPattern = sequencer.getCurrentPattern();
        currentPattern.setPitchAtInterval(interval, pitch, isEnabled);

        JamMachineMod.LOGGER.info("Sett pitch {} at interval {} - {}", pitch, interval, isEnabled);
        if (!level.isClientSide())
        {
            if (isEnabled) {
                level.blockEvent(worldPosition, getBlockState().getBlock(), ENABLE_NOTE | (interval << 4), pitch);
            } else {
                level.blockEvent(worldPosition, getBlockState().getBlock(), DISABLE_NOTE | (interval << 4), pitch);
            }
            setChanged();
        }
    }

    private boolean checkCardSlotInteraction(double clickX, double clickZ)
    {
        //check coords for ejecting a card.
        if (this.hasCard)
        {
            final double cardSlotLeft = 0.7192042839004351;
            final double cardSlotRight = 0.9325510942881259;
            final double cardSlotTop = 0.8448828111319173;
            final double cardSlotBottom = 0.9298803321497289;
            if ((clickX>= cardSlotLeft && clickX<= cardSlotRight) && (clickZ>= cardSlotTop && clickZ<= cardSlotBottom))
            {
                return true;
            }
        }
        return false;
    }

    private boolean checkRunButtonInteraction(double clickX, double clickZ)
    {
        //check coords for ejecting a card.
        final double runButtonLeft = 0.7192042839004351;
        final double runButtonRight = 0.9325510942881259;
        final double runButtonTop = 0.3479098384432291;
        final double runButtonBottom = 0.41390746154313085;
        if ((clickX>= runButtonLeft && clickX<= runButtonRight) && (clickZ>= runButtonTop && clickZ<= runButtonBottom))
        {
            if (!level.isClientSide())
            {
                sequencer.setProgramming(!sequencer.isProgramming());

                level.blockEvent(worldPosition, getBlockState().getBlock(), RUN_PROGRAM, sequencer.isProgramming() ? 1 : 0);
            }
            return true;
        }

        return false;
    }

    private boolean checkBankInteraction(double clickX, double clickZ)
    {
        //check coords for setting bank
        final double bankLeft = 0.707334934013371;
        final double bankRight = 0.9897283611457244;
        final double bankTop = 0.4415902484574872;
        final double bankBottom = 0.5620629764642562;

        if ((clickX>=bankLeft && clickX<=bankRight) && (clickZ>=bankTop && clickZ<=bankBottom))
        {
            if (sequencer == null) {
                sequencer = new Sequencer(level, worldPosition);
                MusicPlayer.startTracking(sequencer);
            }
            final double bankWidth = bankRight - bankLeft;
            final double bankHeight = bankBottom-bankTop;
            final double offX = clickX - bankLeft;
            final double offZ = clickZ - bankTop;
            int index;

            index = (int)((offX / bankWidth) * 4);
            index += ((int)((offZ / bankHeight) * 2)) * 4;


			/*sequencer.setPendingPatternIndex(index);
			if (!isPlaying)
			{
				sequencer.setCurrentPatternIndex(index);
			}*/

            if (!level.isClientSide())
            {
                level.blockEvent(worldPosition, getBlockState().getBlock(), CHANGE_PATTERN, index);
                setChanged();
            }
            return true;
        }
        return false;
    }

    private boolean checkBPMInteraction(double clickX, double clickZ)
    {
        //BPM UI buttons
        final double bpmLeft = 0.7045204265288701;
        final double bpmRight = 0.9327918869342732;
        final double bpmTop = 0.21970650094097977;
        final double bpmBottom = 0.2705630830397361;

        if ((clickX>= bpmLeft && clickX<= bpmRight) && (clickZ>= bpmTop && clickZ<= bpmBottom))
        {
            final double bpmUIWidth = bpmRight - bpmLeft;
            final double offX = clickX - bpmLeft;

            final int index = (int)((offX / bpmUIWidth) * 4);

            switch (index) {
                case 0:
                    sequencer.setDesiredBPM(Math.max(sequencer.getBeatsPerMinute() - 10, 60));
                    break;
                case 1:
                    sequencer.setDesiredBPM(Math.max(sequencer.getBeatsPerMinute() - 1, 60));
                    break;
                case 2:
                    sequencer.setDesiredBPM(Math.min(sequencer.getBeatsPerMinute() +1, 240));
                    break;
                case 3:
                    sequencer.setDesiredBPM(Math.min(sequencer.getBeatsPerMinute() +10, 240));
                    break;
            }
            if (!level.isClientSide())
            {
                level.blockEvent(worldPosition, getBlockState().getBlock(), CHANGE_BPM, sequencer.getBeatsPerMinute());
                setChanged();
            }
            return true;
        }
        return false;
    }

    public void ejectCard(double x, double y, double z)
    {
        JamMachineMod.LOGGER.info("Eject Card");

        ItemStack card = saveToCard();
        if (!level.isClientSide())
        {
            ItemEntity entityitem = new ItemEntity(this.level, x, y + 0.5, z, card);
            entityitem.setDefaultPickUpDelay();
            level.addFreshEntity(entityitem);
        }
        setHasCard(false);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state)
    {
        if (hasCard && level != null && !level.isClientSide()) {
            ejectCard(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        }
    }

    public Sequencer getSequencer()
    {
        return sequencer;
    }

    /**
     * Returns true if this thing is named
     */
    @Override
    public boolean hasCustomName()
    {
        return this.customName != null && !this.customName.isEmpty();
    }

    public void setCustomName(String name)
    {
        this.customName = name;
        sendUpdates();
    }

    @Override
    public Component getDisplayName()
    {
        if (hasCustomName()) {
            return Component.literal(this.customName);
        }
        return getName();
    }

    @Override
    public Component getName()
    {
        return Component.translatable("block.jammachine.sequencer");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int screenId, Inventory playerInventory, Player player) {
        return new SequencerContainer(screenId, playerInventory, ContainerLevelAccess.create(level, worldPosition));
    }
}
