package org.atomicworkshop.jammachine.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import org.atomicworkshop.jammachine.libraries.BlockLibrary;
import org.atomicworkshop.jammachine.libraries.ContainerTypeLibrary;
import org.atomicworkshop.jammachine.tiles.ControllerTileEntity;

public class ControllerContainer extends AbstractContainerMenu {
    private final ContainerLevelAccess position;

    public ControllerContainer(int windowId, Inventory playerInventory, BlockPos pos) {
        this(windowId, playerInventory, ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ControllerContainer(int windowId, Inventory playerInventory, ContainerLevelAccess position) {
        super(ContainerTypeLibrary.controller_container, windowId);
        this.position = position;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(position, player, BlockLibrary.controller);
    }

    public ControllerTileEntity getTileEntity() {
        return position.evaluate((world, pos) -> world.getBlockEntity(pos) instanceof ControllerTileEntity te ? te : null).orElse(null);
    }
}
