package org.atomicworkshop.jammachine.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CableBlock extends Block
{
	/* cable placed on floor */
	public static final BooleanProperty FLOOR = BooleanProperty.create("floor");
	/* cable placed on ceiling */
	public static final BooleanProperty CEILING = BooleanProperty.create("ceiling");
	/* cable placed on a wall */
	public static final BooleanProperty NORTH = BooleanProperty.create("north");
	public static final BooleanProperty SOUTH = BooleanProperty.create("south");
	public static final BooleanProperty EAST = BooleanProperty.create("east");
	public static final BooleanProperty WEST = BooleanProperty.create("west");

	private static final VoxelShape boundingBox = Shapes.box(0, 0, 0, 1, 0.2, 1);

	public CableBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any()
				.setValue(FLOOR, false).setValue(CEILING, false)
				.setValue(NORTH, false).setValue(SOUTH, false)
				.setValue(EAST, false).setValue(WEST, false));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		return boundingBox;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		builder.add(FLOOR, CEILING, NORTH, SOUTH, EAST, WEST);
	}

	public static BooleanProperty propertyFor(Direction facing)
	{
		return switch (facing) {
			case DOWN -> FLOOR;
			case UP -> CEILING;
			case NORTH -> NORTH;
			case SOUTH -> SOUTH;
			case EAST -> EAST;
			case WEST -> WEST;
		};
	}

	public boolean hasCable(BlockState state, Direction facing)
	{
		return state.getValue(propertyFor(facing));
	}

	/* sets cable state in blockstate */
	public BlockState setCableProperty(Direction facing, BlockState state, boolean value)
	{
		return state.setValue(propertyFor(facing), value);
	}
}
