package micdoodle8.mods.galacticraft.core.items;

import micdoodle8.mods.galacticraft.core.GalacticraftCore;
import micdoodle8.mods.galacticraft.core.proxy.ClientProxyCore;
import micdoodle8.mods.galacticraft.core.util.EnumSortCategoryItem;
import micdoodle8.mods.galacticraft.core.Annotations.RuntimeInterface;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemUniversalWrench extends Item implements ISortableItem
{
    public ItemUniversalWrench(String assetName)
    {
        super();
        this.setUnlocalizedName(assetName);
        this.setMaxStackSize(1);
        this.setMaxDamage(256);
        //this.setTextureName(Constants.TEXTURE_PREFIX + assetName);
    }

    @Override
    public CreativeTabs getCreativeTab()
    {
        return GalacticraftCore.galacticraftItemsTab;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public EnumRarity getRarity(ItemStack par1ItemStack)
    {
        return ClientProxyCore.galacticraftItem;
    }

    @RuntimeInterface(clazz = "buildcraft.api.tools.IToolWrench", modID = "BuildCraft|Core")
    public boolean canWrench(EntityPlayer entityPlayer, BlockPos pos)
    {
        return true;
    }

    @RuntimeInterface(clazz = "buildcraft.api.tools.IToolWrench", modID = "BuildCraft|Core")
    public void wrenchUsed(EntityPlayer entityPlayer, BlockPos pos)
    {
        ItemStack stack = entityPlayer.inventory.getCurrentItem();

        if (stack != null)
        {
            stack.damageItem(1, entityPlayer);

            if (stack.getItemDamage() >= stack.getMaxDamage())
            {
                stack.stackSize--;
            }

            if (stack.stackSize <= 0)
            {
                entityPlayer.inventory.setInventorySlotContents(entityPlayer.inventory.currentItem, null);
            }
        }
    }

    @Override
    public EnumActionResult onItemUse(ItemStack stack, EntityPlayer playerIn, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
        return EnumActionResult.PASS;
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, IBlockAccess world, BlockPos pos, EntityPlayer player)
    {
        return true;
    }

    @Override
    public void onCreated(ItemStack stack, World world, EntityPlayer player)
    {
        if (world.isRemote && player instanceof EntityPlayerSP)
        {
            ClientProxyCore.playerClientHandler.onBuild(3, (EntityPlayerSP) player);
        }
    }

    @Override
    public EnumActionResult onItemUseFirst(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand)
    {
        if (world.isRemote)
        {
            return EnumActionResult.FAIL;
        }
        IBlockState state = world.getBlockState(pos);
        Block blockID = state.getBlock();

        if (blockID == Blocks.FURNACE || blockID == Blocks.LIT_FURNACE || blockID == Blocks.DROPPER || blockID == Blocks.HOPPER || blockID == Blocks.DISPENSER || blockID == Blocks.PISTON || blockID == Blocks.STICKY_PISTON)
        {
            int metadata = blockID.getMetaFromState(state);

            world.setBlockState(pos, blockID.getStateFromMeta(EnumFacing.getHorizontal((metadata + 1) % 4).ordinal()), 3);
            this.wrenchUsed(player, pos);

            return EnumActionResult.PASS;
        }

        return EnumActionResult.FAIL;
    }

    @Override
    public EnumSortCategoryItem getCategory(int meta)
    {
        return EnumSortCategoryItem.TOOLS;
    }
}
