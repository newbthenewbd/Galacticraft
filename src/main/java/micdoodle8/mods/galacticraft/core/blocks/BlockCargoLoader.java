package micdoodle8.mods.galacticraft.core.blocks;

import micdoodle8.mods.galacticraft.core.GCBlocks;
import micdoodle8.mods.galacticraft.core.GalacticraftCore;
import micdoodle8.mods.galacticraft.core.energy.tile.TileBaseUniversalElectrical;
import micdoodle8.mods.galacticraft.core.items.IShiftDescription;
import micdoodle8.mods.galacticraft.core.tile.TileEntityCargoLoader;
import micdoodle8.mods.galacticraft.core.tile.TileEntityCargoUnloader;
import micdoodle8.mods.galacticraft.core.util.EnumSortCategoryBlock;
import micdoodle8.mods.galacticraft.core.util.GCCoreUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public class BlockCargoLoader extends BlockAdvancedTile implements IShiftDescription, ISortableBlock
{
    private enum EnumLoaderType implements IStringSerializable
    {
        CARGO_LOADER(0, "cargo_loader"),
        CARGO_UNLOADER(1, "cargo_unloader");

        private final int meta;
        private final String name;

        EnumLoaderType(int meta, String name)
        {
            this.meta = meta;
            this.name = name;
        }

        public int getMeta()
        {
            return this.meta;
        }

        public static EnumLoaderType byMetadata(int meta)
        {
            return values()[meta];
        }

        public static EnumLoaderType byIndex(int index)
        {
            return values()[index];
        }

        @Override
        public String getName()
        {
            return this.name;
        }
    }

    public static final PropertyEnum TYPE = PropertyEnum.create("type", EnumLoaderType.class);
    public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);

    public static final int METADATA_CARGO_LOADER = 0;
    public static final int METADATA_CARGO_UNLOADER = 4;

    public BlockCargoLoader(String assetName)
    {
        super(Material.rock);
        this.setHardness(1.0F);
        this.setStepSound(Block.soundTypeMetal);
        this.setUnlocalizedName(assetName);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void getSubBlocks(Item par1, CreativeTabs par2CreativeTabs, List<ItemStack> par3List)
    {
        par3List.add(new ItemStack(par1, 1, BlockCargoLoader.METADATA_CARGO_LOADER));
        par3List.add(new ItemStack(par1, 1, BlockCargoLoader.METADATA_CARGO_UNLOADER));
    }

    @Override
    public CreativeTabs getCreativeTabToDisplayOn()
    {
        return GalacticraftCore.galacticraftBlocksTab;
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state)
    {
        super.onBlockAdded(worldIn, pos, state);

        TileEntity tileEntity = worldIn.getTileEntity(pos);

        if (tileEntity != null)
        {
            if (tileEntity instanceof TileEntityCargoLoader)
            {
                ((TileEntityCargoLoader) tileEntity).checkForCargoEntity();
            }
            else if (tileEntity instanceof TileEntityCargoUnloader)
            {
                ((TileEntityCargoUnloader) tileEntity).checkForCargoEntity();
            }
        }
    }

    @Override
    public boolean onMachineActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumFacing side, float hitX, float hitY, float hitZ)
    {
        playerIn.openGui(GalacticraftCore.instance, -1, worldIn, pos.getX(), pos.getY(), pos.getZ());
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state)
    {
        if (getMetaFromState(state) < BlockCargoLoader.METADATA_CARGO_UNLOADER)
        {
            return new TileEntityCargoLoader();
        }
        else
        {
            return new TileEntityCargoUnloader();
        }
    }

    @Override
    public boolean onUseWrench(World world, BlockPos pos, EntityPlayer entityPlayer, EnumFacing side, float hitX, float hitY, float hitZ)
    {
        int metadata = getMetaFromState(world.getBlockState(pos));
        int change = world.getBlockState(pos).getValue(FACING).rotateY().getHorizontalIndex();

        world.setBlockState(pos, this.getStateFromMeta(metadata - (metadata % 4) + change), 3);

        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileBaseUniversalElectrical)
        {
            ((TileBaseUniversalElectrical) te).updateFacing();
        }

        return true;
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
        final int angle = MathHelper.floor_double(placer.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
        int change = EnumFacing.getHorizontal(angle).getOpposite().getHorizontalIndex();

        if (stack.getItemDamage() >= METADATA_CARGO_UNLOADER)
        {
            change += METADATA_CARGO_UNLOADER;
        }
        else if (stack.getItemDamage() >= METADATA_CARGO_LOADER)
        {
            change += METADATA_CARGO_LOADER;
        }

        worldIn.setBlockState(pos, getStateFromMeta(change), 3);

        for (int dX = -2; dX < 3; dX++)
        {
            for (int dZ = -2; dZ < 3; dZ++)
            {
                final Block block = worldIn.getBlockState(pos.add(dX, 0, dZ)).getBlock();

                if (block == GCBlocks.landingPadFull)
                {
                    worldIn.markBlockForUpdate(pos.add(dX, 0, dZ));
                }
            }
        }
    }

    @Override
    public void onBlockDestroyedByPlayer(World worldIn, BlockPos pos, IBlockState state)
    {
        super.onBlockDestroyedByPlayer(worldIn, pos, state);

        for (int dX = -2; dX < 3; dX++)
        {
            for (int dZ = -2; dZ < 3; dZ++)
            {
                BlockPos newPos = new BlockPos(pos.getX() + dX, pos.getY(), pos.getZ() + dZ);
                final Block block = worldIn.getBlockState(newPos).getBlock();

                if (block == GCBlocks.landingPadFull)
                {
                    worldIn.markBlockForUpdate(newPos);
                }
            }
        }
    }

    @Override
    public int damageDropped(IBlockState state)
    {
        return getMetaFromState(state);
    }

    @Override
    public String getShiftDescription(int meta)
    {
        switch (meta)
        {
        case METADATA_CARGO_LOADER:
            return GCCoreUtil.translate("tile.cargo_loader.description");
        case METADATA_CARGO_UNLOADER:
            return GCCoreUtil.translate("tile.cargo_unloader.description");
        }
        return "";
    }

    @Override
    public boolean showDescription(int meta)
    {
        return true;
    }

    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        EnumFacing enumfacing = EnumFacing.getHorizontal(meta % 4);
        EnumLoaderType type = EnumLoaderType.byMetadata((int) Math.floor(meta / 4.0));

        return this.getDefaultState().withProperty(FACING, enumfacing).withProperty(TYPE, type);
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return ((EnumFacing) state.getValue(FACING)).getHorizontalIndex() + ((EnumLoaderType) state.getValue(TYPE)).getMeta() * 4;
    }

    @Override
    protected BlockState createBlockState()
    {
        return new BlockState(this, FACING, TYPE);
    }

    @Override
    public EnumSortCategoryBlock getCategory(int meta)
    {
        return EnumSortCategoryBlock.MACHINE;
    }
}
