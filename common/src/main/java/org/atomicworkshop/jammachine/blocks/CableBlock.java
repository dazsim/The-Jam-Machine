package org.atomicworkshop.jammachine.blocks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.atomicworkshop.jammachine.cables.CableNetwork;
import org.atomicworkshop.jammachine.libraries.BlockLibrary;
import org.atomicworkshop.jammachine.libraries.ItemLibrary;
import org.atomicworkshop.jammachine.tiles.CableTileEntity;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

public class CableBlock extends BaseEntityBlock
{
	public static final MapCodec<CableBlock> CODEC = simpleCodec(CableBlock::new);
	private static final double THICKNESS = 2 / 16.0;
	private static final double INSET = 3 / 16.0;
	private static final VoxelShape[] SHAPES = new VoxelShape[64];

	static {
		for (int mask = 0; mask < 64; mask++) {
			VoxelShape shape = Shapes.empty();
			for (final Direction face : Direction.values()) {
				if ((mask & (1 << face.get3DDataValue())) != 0) {
					shape = Shapes.or(shape, plate(face));
				}
			}
			SHAPES[mask] = shape.optimize();
		}
	}

	public CableBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	private static VoxelShape plate(Direction face) {
		final double lo = INSET, hi = 1 - INSET, t = THICKNESS;
		return switch (face) {
			case DOWN -> Shapes.box(lo, 0, lo, hi, t, hi);
			case UP -> Shapes.box(lo, 1 - t, lo, hi, 1, hi);
			case NORTH -> Shapes.box(lo, lo, 0, hi, hi, t);
			case SOUTH -> Shapes.box(lo, lo, 1 - t, hi, hi, 1);
			case WEST -> Shapes.box(0, lo, lo, t, hi, hi);
			case EAST -> Shapes.box(1 - t, lo, lo, 1, hi, hi);
		};
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		if (!(level.getBlockEntity(pos) instanceof CableTileEntity cable)) return SHAPES[1];
		int mask = 0;
		for (final Direction face : Direction.values()) {
			if (cable.hasCable(face)) mask |= 1 << face.get3DDataValue();
		}
		return mask == 0 ? SHAPES[1] : SHAPES[mask];
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CableTileEntity(pos, state);
	}

	@Override
	protected boolean isSignalSource(BlockState state) {
		return true;
	}

	//off while a network works out its own input, so it cannot hear itself
	private static boolean shouldSignal = true;
	//off while a sequencer or controller checks its power, so steps do not start and stop machines
	private static boolean pulsesVisible = true;
	//on while a step pulse is going out, cables next to it have nothing to recalculate
	private static boolean pulsing;

	public static <T> T withoutSignal(Supplier<T> check) {
		final boolean old = shouldSignal;
		shouldSignal = false;
		try {
			return check.get();
		} finally {
			shouldSignal = old;
		}
	}

	public static <T> T withoutPulses(Supplier<T> check) {
		final boolean old = pulsesVisible;
		pulsesVisible = false;
		try {
			return check.get();
		} finally {
			pulsesVisible = old;
		}
	}

	public static void pulsing(Runnable action) {
		final boolean old = pulsing;
		pulsing = true;
		try {
			action.run();
		} finally {
			pulsing = old;
		}
	}

	@Override
	protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		if (!shouldSignal || !(level.getBlockEntity(pos) instanceof CableTileEntity cable)) return 0;
		//the block asking is on this side of us
		final Direction towards = direction.getOpposite();
		//note blocks get their notes from the sequencer, a step pulse would play them twice
		final boolean pulses = pulsesVisible && !(level.getBlockState(pos.relative(towards)).getBlock() instanceof NoteBlock);
		for (final Direction face : Direction.values()) {
			if (towards == face.getOpposite()) continue;
			if (cable.isPowered(face) || (pulses && cable.isPulsed(face))) return 15;
		}
		return 0;
	}

	//like dust, a powered cable strongly powers the block it sits on
	@Override
	protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
		if (!shouldSignal || !(level.getBlockEntity(pos) instanceof CableTileEntity cable)) return 0;
		return cable.isPowered(direction.getOpposite()) ? 15 : 0;
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
		if (level.isClientSide() || pulsing || !(level.getBlockEntity(pos) instanceof CableTileEntity cable)) return;
		//cables fall off when the block they sit on goes
		for (final Direction face : Direction.values()) {
			if (cable.hasCable(face) && !canSitOn(level, pos, face)) {
				removeCable(level, pos, face, true);
				if (level.getBlockState(pos).getBlock() != this) return;
			}
		}
		CableNetwork.refreshAround(level, pos);
	}

	public static boolean canSitOn(LevelReader level, BlockPos pos, Direction face) {
		final BlockPos support = pos.relative(face);
		return level.getBlockState(support).isFaceSturdy(level, support, face.getOpposite());
	}

	public static Item itemFor(int colour) {
		if (colour >= 0 && colour < 16) return ItemLibrary.dyedCables[colour];
		return ItemLibrary.cable;
	}

	public static void addCable(Level level, BlockPos pos, Direction face, int colour) {
		if (!(level.getBlockState(pos).getBlock() instanceof CableBlock)) {
			level.setBlock(pos, BlockLibrary.cable.defaultBlockState(), 3);
		}
		if (level.getBlockEntity(pos) instanceof CableTileEntity cable) {
			cable.setColour(face, colour);
			level.updateNeighborsAt(pos, BlockLibrary.cable);
			CableNetwork.refreshAround(level, pos);
		}
	}

	public static void removeCable(Level level, BlockPos pos, Direction face, boolean drop) {
		if (!(level.getBlockEntity(pos) instanceof CableTileEntity cable) || !cable.hasCable(face)) return;
		final int colour = cable.getColour(face);
		if (drop) {
			popResource(level, pos, new ItemStack(itemFor(colour)));
		}
		if (cable.count() == 1) {
			cable.clear();
			level.removeBlock(pos, false);
		} else {
			cable.setColour(face, CableTileEntity.NONE);
			level.updateNeighborsAt(pos, BlockLibrary.cable);
		}
		CableNetwork.refreshAround(level, pos);
	}

	//which cable in the space a point (inside the space) is closest to
	public static @Nullable Direction faceAt(CableTileEntity cable, Vec3 local) {
		Direction best = null;
		double bestDistance = Double.MAX_VALUE;
		for (final Direction face : Direction.values()) {
			if (!cable.hasCable(face)) continue;
			final double along = local.get(face.getAxis());
			final double distance = face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 - along : along;
			if (distance < bestDistance) {
				bestDistance = distance;
				best = face;
			}
		}
		return best;
	}

	/*
	 * Breaking takes out the one cable being looked at. Returns true when that was handled here and
	 * the block should stay, false when it was the last cable and the block can break as normal.
	 */
	public static boolean breakOne(Level level, Player player, BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof CableTileEntity cable) || cable.count() <= 1) return false;
		final HitResult hit = player.pick(player.blockInteractionRange() + 1, 1.0F, false);
		Direction face = null;
		if (hit instanceof BlockHitResult blockHit && blockHit.getBlockPos().equals(pos)) {
			face = faceAt(cable, blockHit.getLocation().subtract(Vec3.atLowerCornerOf(pos)));
		}
		if (face == null) return false;
		removeCable(level, pos, face, !player.isCreative());
		return true;
	}

	//neoforge calls this to remove the block, returning false keeps it
	public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, ItemStack tool, boolean willHarvest, FluidState fluid) {
		if (level.getBlockEntity(pos) instanceof CableTileEntity cable && cable.count() > 1) {
			if (!level.isClientSide()) breakOne(level, player, pos);
			return false;
		}
		if (level.isClientSide()) return level.setBlock(pos, fluid.createLegacyBlock(), 11);
		return level.removeBlock(pos, false);
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (player.isCreative() && level.getBlockEntity(pos) instanceof CableTileEntity cable && cable.count() <= 1) {
			cable.clear();
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		CableNetwork.refreshAround(level, pos);
	}
}
