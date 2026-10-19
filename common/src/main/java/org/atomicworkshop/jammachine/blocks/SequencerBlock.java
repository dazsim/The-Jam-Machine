package org.atomicworkshop.jammachine.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.atomicworkshop.jammachine.ClientConfig;
import org.atomicworkshop.jammachine.JamMachineMod;
import org.atomicworkshop.jammachine.items.ItemPunchCardBlank;
import org.atomicworkshop.jammachine.items.ItemPunchCardWritten;
import org.atomicworkshop.jammachine.libraries.TileEntityTypeLibrary;
import org.atomicworkshop.jammachine.tiles.SequencerTileEntity;
import org.atomicworkshop.jammachine.util.CollisionMaths;
import org.jspecify.annotations.Nullable;

public class SequencerBlock extends BaseEntityBlock {
    public static final MapCodec<SequencerBlock> CODEC = simpleCodec(SequencerBlock::new);
    private static final VoxelShape SHAPE = Shapes.box(0, 0, 0, 1, 0.5, 1);

    public SequencerBlock(Properties properties) {
        super(properties);

        final BlockState defaultState = this.stateDefinition.any()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        registerDefaultState(defaultState);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter reader, BlockPos pos, CollisionContext selectionContext) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext useContext) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, useContext.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(BlockStateProperties.HORIZONTAL_FACING, rotation.rotate(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SequencerTileEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, TileEntityTypeLibrary.sequencer_tile, SequencerTileEntity::tick);
    }

    @Override
    protected void neighborChanged(BlockState blockState, Level worldIn, BlockPos pos, Block blockIn, @Nullable Orientation orientation, boolean movedByPiston) {
        if (worldIn.isClientSide()) return;
        final boolean isPowered = CableBlock.withoutPulses(() -> {
            if (worldIn.hasNeighborSignal(pos)) return true;
            for (final Direction value : Direction.values()) {
                if (worldIn.hasNeighborSignal(pos.relative(value))) return true;
            }
            return false;
        });

        final SequencerTileEntity tileEntity = getTileEntity(worldIn, pos);
        if (tileEntity == null) return;

        tileEntity.notifyPowered(isPowered);
    }

    @Override
    protected boolean triggerEvent(BlockState blockState, Level world, BlockPos pos, int id, int param) {
        final SequencerTileEntity tileEntity = getTileEntity(world, pos);
        if (tileEntity == null) return false;

        return tileEntity.triggerEvent(id, param);
    }

    private SequencerTileEntity getTileEntity(Level world, BlockPos pos) {
        final BlockEntity tileEntity = world.getBlockEntity(pos);
        if (tileEntity instanceof SequencerTileEntity) {
            return (SequencerTileEntity)tileEntity;
        }
        JamMachineMod.LOGGER.error("No Tile entity found at block location? {}", pos);
        return null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level worldIn, BlockPos pos, Player playerIn, BlockHitResult rayTraceResult) {
        final SequencerTileEntity teSequencer = getTileEntity(worldIn, pos);
        if (teSequencer == null) {
            return InteractionResult.PASS;
        }
        if (!playerIn.getOffhandItem().isEmpty()) {
            return InteractionResult.PASS;
        }

        if (ClientConfig.USE_GUI) {
            if (!worldIn.isClientSide()) {
                JamMachineMod.PLATFORM.openMenu((ServerPlayer) playerIn, teSequencer, pos);
            }
        } else {
            final Vec3 hitVec = CollisionMaths.calculateSlopeHit(pos, state.getValue(BlockStateProperties.HORIZONTAL_FACING), playerIn);
            if (hitVec == null) return InteractionResult.PASS;
            return teSequencer.checkPlayerInteraction(hitVec.x, hitVec.z, playerIn) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack heldItemStack, BlockState state, Level worldIn, BlockPos pos, Player playerIn, InteractionHand hand, BlockHitResult rayTraceResult) {
        /*
         * This is where we handle interaction with the Sequencer
         * Insert card on right click with card in hand
         * right click with an empty hand opens the GUI
         */
        if (heldItemStack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        final SequencerTileEntity teSequencer = getTileEntity(worldIn, pos);
        if (teSequencer == null) {
            return InteractionResult.PASS;
        }
        final Item heldItem = heldItemStack.getItem();

        if (heldItem instanceof ItemPunchCardBlank) {
            //you just inserted a blank card into sequencer. load the sequence onto it?
            if (teSequencer.getHasCard()) {
                JamMachineMod.LOGGER.debug("Sequencer already has Card");
                return InteractionResult.PASS;
            } else {
                if (!worldIn.isClientSide()) {
                    teSequencer.setHasCard(true);
                    heldItemStack.consume(1, playerIn);
                }
                return InteractionResult.SUCCESS;
            }
        }

        if (heldItem instanceof ItemPunchCardWritten) {
            //Attempting to insert a written card.
            if (teSequencer.getHasCard()) {
                JamMachineMod.LOGGER.debug("Sequencer already has Card");
                return InteractionResult.PASS;
            } else {
                if (!worldIn.isClientSide()) {
                    teSequencer.loadFromCard(heldItemStack);
                    heldItemStack.consume(1, playerIn);
                    teSequencer.setHasCard(true);
                }
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }
}
