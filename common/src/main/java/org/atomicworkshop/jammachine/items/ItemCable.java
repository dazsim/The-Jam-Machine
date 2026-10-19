package org.atomicworkshop.jammachine.items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.atomicworkshop.jammachine.blocks.CableBlock;
import org.atomicworkshop.jammachine.tiles.CableTileEntity;

public class ItemCable extends Item {
	private final int colour;

	public ItemCable(Properties properties, int colour)
	{
		super(properties);
		this.colour = colour;
	}

	public int getColour() {
		return colour;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		final Level world = context.getLevel();
		final BlockPos pos = context.getClickedPos();
		final Direction side = context.getClickedFace();

		BlockPos target = null;
		Direction face = null;

		if (world.getBlockEntity(pos) instanceof CableTileEntity cable)
		{
			//we are clicking an existing cable, lay a new one on the free face nearest to where we clicked
			final Vec3 local = context.getClickLocation().subtract(Vec3.atCenterOf(pos));
			double best = -Double.MAX_VALUE;
			for (final Direction d : Direction.values()) {
				if (cable.hasCable(d) || !CableBlock.canSitOn(world, pos, d)) continue;
				final double towards = local.dot(Vec3.atLowerCornerOf(d.getUnitVec3i()));
				if (towards > best) {
					best = towards;
					face = d;
				}
			}
			target = pos;
		}

		if (face == null)
		{
			target = pos.relative(side);
			face = side.getOpposite();
			final BlockState there = world.getBlockState(target);
			if (!CableBlock.canSitOn(world, target, face)) return InteractionResult.FAIL;
			if (there.getBlock() instanceof CableBlock) {
				if (world.getBlockEntity(target) instanceof CableTileEntity cable && cable.hasCable(face)) return InteractionResult.FAIL;
			} else if (!there.canBeReplaced()) {
				return InteractionResult.FAIL;
			}
		}

		if (!world.isClientSide()) {
			CableBlock.addCable(world, target, face, colour);
			context.getItemInHand().consume(1, context.getPlayer());
		}
		return InteractionResult.SUCCESS;
	}
}
