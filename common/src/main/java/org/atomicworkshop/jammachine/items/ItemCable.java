package org.atomicworkshop.jammachine.items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.atomicworkshop.jammachine.blocks.CableBlock;
import org.atomicworkshop.jammachine.libraries.BlockLibrary;

public class ItemCable extends Item {

	public ItemCable(Properties properties)
	{
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		final Level world = context.getLevel();
		final BlockPos pos = context.getClickedPos();
		final Direction side = context.getClickedFace();
		final BlockState clicked = world.getBlockState(pos);

		if (clicked.getBlock() instanceof CableBlock cable)
		{
			//we are clicking an existing cable, add one against the block behind it
			final BlockPos behind = pos.relative(side.getOpposite());
			if (world.getBlockState(behind).isFaceSturdy(world, behind, side) && !cable.hasCable(clicked, side.getOpposite()))
			{
				if (!world.isClientSide()) {
					world.setBlock(pos, cable.setCableProperty(side.getOpposite(), clicked, true), 3);
					context.getItemInHand().consume(1, context.getPlayer());
				}
				return InteractionResult.SUCCESS;
			}
		}

		final BlockPos target = pos.relative(side);
		if (world.getBlockState(target).isAir() && clicked.isFaceSturdy(world, pos, side))
		{
			//this should create a new cable at coordinates
			if (!world.isClientSide()) {
				world.setBlock(target, BlockLibrary.cable.setCableProperty(side.getOpposite(), BlockLibrary.cable.defaultBlockState(), true), 3);
				context.getItemInHand().consume(1, context.getPlayer());
			}
			return InteractionResult.SUCCESS;
		}

		return InteractionResult.FAIL;
	}
}
