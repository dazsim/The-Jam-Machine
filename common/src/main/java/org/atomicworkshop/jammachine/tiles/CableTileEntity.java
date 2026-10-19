package org.atomicworkshop.jammachine.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.atomicworkshop.jammachine.blocks.CableBlock;
import org.atomicworkshop.jammachine.libraries.TileEntityTypeLibrary;

import java.util.Arrays;

//one block space can hold a cable on each of its six faces
public class CableTileEntity extends BlockEntity {
    public static final byte NONE = -1;
    //undyed cable, 0-15 are the dye colours
    public static final byte PLAIN = 16;

    private final byte[] colours = new byte[6];
    //one bit per channel for each face, a dyed cable only ever has its own
    private final int[] powered = new int[6];
    //set for a moment by a sequencer step, never saved
    private final int[] pulsed = new int[6];

    public CableTileEntity(BlockPos pos, BlockState state) {
        super(TileEntityTypeLibrary.cable_tile, pos, state);
        Arrays.fill(colours, NONE);
    }

    public int getColour(Direction face) {
        return colours[face.get3DDataValue()];
    }

    public boolean hasCable(Direction face) {
        return colours[face.get3DDataValue()] != NONE;
    }

    public boolean isEmpty() {
        for (byte colour : colours) {
            if (colour != NONE) return false;
        }
        return true;
    }

    public int count() {
        int count = 0;
        for (byte colour : colours) {
            if (colour != NONE) count++;
        }
        return count;
    }

    public void setColour(Direction face, int colour) {
        colours[face.get3DDataValue()] = (byte) colour;
        powered[face.get3DDataValue()] = 0;
        pulsed[face.get3DDataValue()] = 0;
        sendUpdates();
    }

    public boolean isPowered(Direction face) {
        return powered[face.get3DDataValue()] != 0;
    }

    public boolean isPulsed(Direction face) {
        return pulsed[face.get3DDataValue()] != 0;
    }

    //both return true when the face as a whole turned on or off
    public boolean setPowered(Direction face, int channel, boolean isPowered) {
        final int i = face.get3DDataValue();
        final int old = powered[i];
        powered[i] = isPowered ? old | (1 << channel) : old & ~(1 << channel);
        if (old != powered[i]) setChanged();
        return (old == 0) != (powered[i] == 0);
    }

    public boolean setPulsed(Direction face, int channel, boolean isPulsed) {
        final int i = face.get3DDataValue();
        final int old = pulsed[i];
        pulsed[i] = isPulsed ? old | (1 << channel) : old & ~(1 << channel);
        return (old == 0) != (pulsed[i] == 0);
    }

    public void clear() {
        Arrays.fill(colours, NONE);
        Arrays.fill(powered, 0);
        Arrays.fill(pulsed, 0);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level == null || level.isClientSide()) return;
        for (final Direction face : Direction.values()) {
            if (hasCable(face)) {
                Block.popResource(level, pos, new ItemStack(CableBlock.itemFor(getColour(face))));
            }
        }
        clear();
    }

    private void sendUpdates() {
        setChanged();
        if (level != null) {
            final BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (final Direction face : Direction.values()) {
            colours[face.get3DDataValue()] = input.getByteOr(face.getName(), NONE);
        }
        final int[] saved = input.getIntArray("powered").orElse(new int[0]);
        for (int i = 0; i < 6; i++) powered[i] = i < saved.length ? saved[i] : 0;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (final Direction face : Direction.values()) {
            if (hasCable(face)) {
                output.putByte(face.getName(), colours[face.get3DDataValue()]);
            }
        }
        output.putIntArray("powered", powered);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
