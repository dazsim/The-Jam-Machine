package org.atomicworkshop.jammachine.libraries;

import net.minecraft.world.level.block.entity.BlockEntityType;
import org.atomicworkshop.jammachine.tiles.CableTileEntity;
import org.atomicworkshop.jammachine.tiles.ControllerTileEntity;
import org.atomicworkshop.jammachine.tiles.SequencerTileEntity;

public class TileEntityTypeLibrary {
    public static BlockEntityType<SequencerTileEntity> sequencer_tile;
    public static BlockEntityType<CableTileEntity> cable_tile;
    public static BlockEntityType<ControllerTileEntity> controller_tile;
}
