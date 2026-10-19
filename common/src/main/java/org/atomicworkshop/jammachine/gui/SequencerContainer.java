package org.atomicworkshop.jammachine.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.atomicworkshop.jammachine.libraries.BlockLibrary;
import org.atomicworkshop.jammachine.libraries.ContainerTypeLibrary;
import org.atomicworkshop.jammachine.tiles.SequencerTileEntity;

public class SequencerContainer extends AbstractContainerMenu {
    private final ContainerLevelAccess position;

    private final int[] patternState = new int[16 * 25];

    public SequencerContainer(int windowId, Inventory playerInventory, BlockPos pos) {
        this(windowId, playerInventory, ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public SequencerContainer(int windowId, Inventory playerInventory, ContainerLevelAccess position) {
        super(ContainerTypeLibrary.sequencer_container, windowId);
        this.position = position;

        for (int interval=0;interval<16;interval++) {
            for (int pitch=0;pitch<25;pitch++) {
                addDataSlot(DataSlot.shared(patternState, interval * 25 + pitch));
            }
        }
    }

    public boolean set(int pitch, int interval, boolean isSet) {
        final int index = interval * 25 + pitch;
        patternState[index] = isSet ? 1 : 0;
        return patternState[index] == 1;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player playerIn) {
        return stillValid(this.position, playerIn, BlockLibrary.sequencer);
    }

    public SequencerTileEntity getTileEntity() {
        return this.position.evaluate((world, position) -> {
            BlockEntity te = world.getBlockEntity(position);
            if (te instanceof SequencerTileEntity) {
                return (SequencerTileEntity)te;
            }
            return null;
        }).orElse(null);
    }
}
