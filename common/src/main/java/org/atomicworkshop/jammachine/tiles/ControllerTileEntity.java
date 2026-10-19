package org.atomicworkshop.jammachine.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.atomicworkshop.jammachine.cables.CableNetwork;
import org.atomicworkshop.jammachine.gui.ControllerContainer;
import org.atomicworkshop.jammachine.libraries.TileEntityTypeLibrary;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//keeps every sequencer it is cabled to playing together, at one tempo
public class ControllerTileEntity extends BlockEntity implements MenuProvider {
    public static final int MAX_SEQUENCERS = 8;

    private UUID songId = UUID.randomUUID();
    private int beatsPerMinute = 120;
    private boolean isRunning;
    private boolean isPowered;
    private final List<BlockPos> sequencers = new ArrayList<>();

    public ControllerTileEntity(BlockPos pos, BlockState state) {
        super(TileEntityTypeLibrary.controller_tile, pos, state);
    }

    public int getBeatsPerMinute() {
        return beatsPerMinute;
    }

    public boolean isPlaying() {
        return isRunning || isPowered;
    }

    public List<BlockPos> getSequencers() {
        return sequencers;
    }

    public void setBeatsPerMinute(int bpm) {
        beatsPerMinute = Math.max(SequencerTileEntity.MIN_BPM, Math.min(SequencerTileEntity.MAX_BPM, bpm));
        for (final SequencerTileEntity sequencer : connected()) {
            sequencer.setBeatsPerMinute(beatsPerMinute);
        }
        sendUpdates();
    }

    public void togglePlaying() {
        isRunning = !isRunning;
        updateSequencers();
        sendUpdates();
    }

    public void notifyPowered(boolean powered) {
        if (isPowered == powered) return;
        isPowered = powered;
        updateSequencers();
        sendUpdates();
    }

    public void selectPattern(int slot, int pattern) {
        if (slot < 0 || slot >= sequencers.size()) return;
        if (level.getBlockEntity(sequencers.get(slot)) instanceof SequencerTileEntity sequencer) {
            sequencer.selectPattern(pattern);
        }
    }

    private List<SequencerTileEntity> connected() {
        final List<SequencerTileEntity> out = new ArrayList<>();
        if (level == null) return out;
        for (final BlockPos pos : sequencers) {
            if (level.getBlockEntity(pos) instanceof SequencerTileEntity sequencer) out.add(sequencer);
        }
        return out;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ControllerTileEntity controller) {
        if (level.isClientSide() || (level.getGameTime() + pos.hashCode()) % 20 != 0) return;
        controller.findSequencers();
    }

    private void findSequencers() {
        final List<BlockPos> found = new ArrayList<>();
        for (final Direction side : Direction.values()) {
            final BlockPos next = worldPosition.relative(side);
            if (level.getBlockEntity(next) instanceof SequencerTileEntity) found.add(next);
        }
        for (final BlockPos device : CableNetwork.devicesFrom(level, worldPosition)) {
            if (found.size() >= MAX_SEQUENCERS) break;
            if (!found.contains(device) && level.getBlockEntity(device) instanceof SequencerTileEntity) found.add(device);
        }
        while (found.size() > MAX_SEQUENCERS) found.removeLast();

        if (!found.equals(sequencers)) {
            //let go of any that are no longer connected
            for (final SequencerTileEntity sequencer : connected()) {
                if (!found.contains(sequencer.getBlockPos()) && sequencer.isControlledBy(songId)) {
                    sequencer.setControlled(null, false);
                }
            }
            sequencers.clear();
            sequencers.addAll(found);
            sendUpdates();
        }
        updateSequencers();
    }

    private void updateSequencers() {
        if (level == null || level.isClientSide()) return;
        for (final SequencerTileEntity sequencer : connected()) {
            sequencer.setBeatsPerMinute(beatsPerMinute);
            sequencer.setControlled(songId, isPlaying());
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && !level.isClientSide()) {
            for (final SequencerTileEntity sequencer : connected()) {
                if (sequencer.isControlledBy(songId)) sequencer.setControlled(null, false);
            }
        }
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
        songId = input.read("songId", UUIDUtil.CODEC).orElseGet(UUID::randomUUID);
        beatsPerMinute = input.getIntOr("bpm", 120);
        isRunning = input.getBooleanOr("running", false);
        isPowered = input.getBooleanOr("powered", false);
        sequencers.clear();
        input.read("sequencers", BlockPos.CODEC.listOf()).ifPresent(sequencers::addAll);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("songId", UUIDUtil.CODEC, songId);
        output.putInt("bpm", beatsPerMinute);
        output.putBoolean("running", isRunning);
        output.putBoolean("powered", isPowered);
        output.store("sequencers", BlockPos.CODEC.listOf(), sequencers);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.jammachine.controller");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ControllerContainer(id, inventory, ContainerLevelAccess.create(level, worldPosition));
    }
}
