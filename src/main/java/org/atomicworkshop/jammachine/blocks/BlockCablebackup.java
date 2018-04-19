package org.atomicworkshop.jammachine.blocks;

import org.atomicworkshop.jammachine.JamMachineMod;
import org.atomicworkshop.jammachine.tiles.TileEntityCable;
import org.atomicworkshop.jammachine.tiles.TileEntitySequencer;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

@SuppressWarnings("deprecation")
public class BlockCablebackup extends Block implements ITileEntityProvider
{
	
	/* cable placed on floor */
	 public static final PropertyBool FLOOR = PropertyBool.create("floor");
	 /* cable placed on floor */
	 public static final PropertyBool CEILING = PropertyBool.create("ceiling");
	 /* cable placed on floor */
	 
	 public static final PropertyBool NORTH = PropertyBool.create("north");
	 /* cable placed on floor */
	 public static final PropertyBool SOUTH = PropertyBool.create("south");
	 /* cable placed on floor */
	 public static final PropertyBool EAST = PropertyBool.create("east");
	 /* cable placed on floor */
	 public static final PropertyBool WEST = PropertyBool.create("west");
	 
	 
	 
	private final AxisAlignedBB boundingBox = new AxisAlignedBB(0, 0, 0, 1, 0.2, 1);

	public BlockCablebackup() {
		super(new MachineMaterial());
		//blockState.validateProperty(block, property)
		//final IBlockState defaultState = blockState.getBaseState().withProperty(NORTH, Boolean.FALSE).withProperty(EAST, Boolean.FALSE).withProperty(SOUTH, Boolean.FALSE).withProperty(WEST, Boolean.FALSE).withProperty(FLOOR, Boolean.FALSE).withProperty(CEILING, Boolean.FALSE);
		
	}

	@Override
	public boolean shouldCheckWeakPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side)
	{
		//Disabling weak power checks here prevents Note Blocks from being fired when the sequencer receives a redstone
		//signal.
		return false;
	}

	@Override
	@Deprecated
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
	{
		return boundingBox;
	}

	@Override
	protected BlockStateContainer createBlockState()
	{
		return new BlockStateContainer(this,FLOOR, CEILING,NORTH,SOUTH,EAST,WEST);
		
	}

	@Override
	public IBlockState getActualState(IBlockState state,IBlockAccess worldIn,BlockPos pos)
	{
		if (worldIn.getTileEntity(pos) instanceof TileEntityCable)
		{
			TileEntityCable te = (TileEntityCable)worldIn.getTileEntity(pos);
			JamMachineMod.logger.info("*********");
			JamMachineMod.logger.info(te.hasCable(EnumFacing.DOWN).toString());
			JamMachineMod.logger.info("*********");
			return this.getDefaultState()
					.withProperty(FLOOR, te.hasCable(EnumFacing.DOWN))
					.withProperty(CEILING, te.hasCable(EnumFacing.UP))
					.withProperty(NORTH, te.hasCable(EnumFacing.NORTH))
					.withProperty(SOUTH, te.hasCable(EnumFacing.SOUTH))
					.withProperty(EAST, te.hasCable(EnumFacing.EAST))
					.withProperty(WEST, te.hasCable(EnumFacing.WEST))
					;	
			
		}
		return this.getDefaultState();
				
	}
	@Override
	public int getMetaFromState(IBlockState state)
	{
		return 0; //TODO : this will change as i add support for cables connecting to each other. or not
	}

	@Override
	@Deprecated
	public IBlockState getStateFromMeta(int meta)
	{
		return getDefaultState();
	
	}
	/* sets cable state in blockstate */
	public IBlockState setCableProperty(EnumFacing facing, IBlockState state, Boolean value)
	{
		if (facing.equals(EnumFacing.UP))
    	{
				return (this.createBlockState().getBaseState())
					
					.withProperty(this.FLOOR, value.booleanValue()).withProperty(this.CEILING, false)
					.withProperty(this.NORTH, false).withProperty(this.SOUTH, false)
					.withProperty(this.EAST, false).withProperty(this.WEST, false);
    	}
    	if (facing.equals(EnumFacing.DOWN))
    	{
    			return (this.createBlockState().getBaseState())
					
					.withProperty(this.FLOOR, value.booleanValue()).withProperty(this.CEILING, false)
					.withProperty(this.NORTH, false).withProperty(this.SOUTH, false)
					.withProperty(this.EAST, false).withProperty(this.WEST, false);
    	}
    	if (facing.equals(EnumFacing.NORTH))
    	{
    			return (this.createBlockState().getBaseState())
					
					.withProperty(this.FLOOR, value.booleanValue()).withProperty(this.CEILING, false)
					.withProperty(this.NORTH, false).withProperty(this.SOUTH, false)
					.withProperty(this.EAST, false).withProperty(this.WEST, false);
    	}
    	if (facing.equals(EnumFacing.SOUTH))
    	{
    		return (this.createBlockState().getBaseState())
					
					.withProperty(this.FLOOR, value.booleanValue()).withProperty(this.CEILING, false)
					.withProperty(this.NORTH, false).withProperty(this.SOUTH, false)
					.withProperty(this.EAST, false).withProperty(this.WEST, false);
    	}
    	if (facing.equals(EnumFacing.EAST))
    	{
    		return (this.createBlockState().getBaseState())
					
					.withProperty(this.FLOOR, value.booleanValue()).withProperty(this.CEILING, false)
					.withProperty(this.NORTH, false).withProperty(this.SOUTH, false)
					.withProperty(this.EAST, false).withProperty(this.WEST, false);
    	}
    	if (facing.equals(EnumFacing.WEST))
    	{
    		return (this.createBlockState().getBaseState())
					
					.withProperty(this.FLOOR, value.booleanValue()).withProperty(this.CEILING, false)
					.withProperty(this.NORTH, false).withProperty(this.SOUTH, false)
					.withProperty(this.EAST, false).withProperty(this.WEST, false);
    	}
    	return state;
	}
	
	/*sets cable state in TE */
	public void setCableState(World world, BlockPos pos, EnumFacing facing, boolean state)
	{
		if (world.getTileEntity(pos) instanceof TileEntityCable)
		{
			TileEntityCable te = (TileEntityCable)world.getTileEntity(pos);
			te.setCable(facing, Boolean.valueOf(state));
			te.markDirty();
			
		}
	}
	
	@Override
	public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
	                                        float hitX, float hitY, float hitZ, int meta,
	                                        EntityLivingBase placer)
	{
		/*check if there is already a cable in this block*/
		
		JamMachineMod.logger.info(facing.toString());
		//System.out.println(world.getBlockState(pos).getBlock().toString());
		JamMachineMod.logger.info(world.getBlockState(pos).getBlock().toString());
		if (world.getBlockState(pos).getBlock().toString().equals("Block{minecraft:air}"))
		{
			JamMachineMod.logger.info("IS AIR");
				
			setCableState(world,pos,facing,true);
			
			return setCableProperty(facing,getActualState(this.getDefaultState(), world, pos),Boolean.TRUE); 
			//return (this.createBlockState().getBaseState().withProperty(this.FLOOR, true).withProperty(this.CEILING, false).withProperty(this.NORTH, false).withProperty(this.SOUTH, false).withProperty(this.EAST, false).withProperty(this.WEST, false));
			
			
		}
		if (world.getBlockState(pos).getBlock() instanceof BlockCablebackup)
		{
			/*check if there is already a cable on this surface*/
			System.out.println(facing.toString());
			if (facing.equals(facing.UP))
			{
				
				if (world.getBlockState(pos).getProperties().containsKey(FLOOR))
				{
					return (world.getBlockState(pos));
				}  else
				{
					return (world.getBlockState(pos).withProperty(this.FLOOR, true));
				}
			}
			if (facing.equals(facing.DOWN))
			{
				if (world.getBlockState(pos).getProperties().containsKey(CEILING))
				{
					return (world.getBlockState(pos));
				} else
				{
					/*valid placement. make sure there is a valid block above this one*/
					if (world.getBlockState(pos).getBlock().isFullBlock(world.getBlockState(pos)))
					{
						return (world.getBlockState(pos).withProperty(this.CEILING, true));
					}
				}
			}
		}
		if (facing.DOWN.equals(facing))
		{
			return getDefaultState().withProperty(this.FLOOR, true);
		}
		return getDefaultState(); //TODO this will change as i add support for cables connecting when you place them
		
	}
	
	
/*
	@Nullable
	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta)
	{
		return new TileEntitySequencer();
	}*/

	@Override
	public void breakBlock(World worldIn, BlockPos pos, IBlockState state)
	{
		
		super.breakBlock(worldIn, pos, state);
	}

	@Override
	@Deprecated
	public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos)
	{
		boolean isPowered = false;
		for (final EnumFacing value : EnumFacing.VALUES)
		{
			isPowered = worldIn.isBlockPowered(pos.offset(value));
			if (isPowered) break;
		}

		final TileEntityCable tileEntity = getTileEntity(worldIn, pos);
		if (tileEntity == null) return;

		//tileEntity.notifyPowered(isPowered);
	}

	@Override
	@Deprecated
	public boolean isOpaqueCube(IBlockState state)
	{
		return false; 
	}
	
	
	
	
	@Override
	@Deprecated
	public boolean eventReceived(IBlockState state, World world, BlockPos pos, int id, int param)
	{
		final TileEntityCable tileEntity = getTileEntity(world, pos);
		if (tileEntity == null) return false;

		return tileEntity.receiveClientEvent(id, param);
	}

	public TileEntityCable getTileEntity(IBlockAccess world, BlockPos pos) {
		final TileEntity tileEntity = world.getTileEntity(pos);
		if (tileEntity instanceof TileEntityCable) {
			return (TileEntityCable)tileEntity;
		}
		JamMachineMod.logger.error("No Tile entity found at block location? {}", pos);
		return null;
	}

	@SuppressWarnings("ChainOfInstanceofChecks")
	@Override
	public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
		/*
		 * This is where we handle interaction with the cable and adjcent cables
		 * 
		 */
	    

	    return false;
    }
	@Override
	public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
		JamMachineMod.logger.error("block placed. lets see whats here.");
		JamMachineMod.logger.error(state.toString());
		if (state.getProperties().get(CEILING).equals(Boolean.TRUE))
		{
			JamMachineMod.logger.error("CEILING SET TRUE");
			if (worldIn.getTileEntity(pos) instanceof TileEntityCable)
			{
				TileEntityCable te =(TileEntityCable)worldIn.getTileEntity(pos);
				te.setCable(EnumFacing.DOWN, true);
				
				JamMachineMod.logger.error("setCable(EnumFacing.DOWN,true)");
			}
		}
    }
	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta) {
		// TODO Auto-generated method stub
		
		return new TileEntityCable();
	}


}
