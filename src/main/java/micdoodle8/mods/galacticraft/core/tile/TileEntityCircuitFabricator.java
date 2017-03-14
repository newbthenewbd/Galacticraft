package micdoodle8.mods.galacticraft.core.tile;

import micdoodle8.mods.galacticraft.api.recipe.CircuitFabricatorRecipes;
import micdoodle8.mods.galacticraft.core.GCItems;
import micdoodle8.mods.galacticraft.core.energy.item.ItemElectricBase;
import micdoodle8.mods.galacticraft.core.energy.tile.TileBaseElectricBlockWithInventory;
import micdoodle8.mods.galacticraft.core.items.ItemBasic;
import micdoodle8.mods.galacticraft.core.network.IPacketReceiver;
import micdoodle8.mods.galacticraft.core.util.ConfigManagerCore;
import micdoodle8.mods.galacticraft.core.util.GCCoreUtil;
import micdoodle8.mods.galacticraft.planets.mars.blocks.BlockMachineMars;
import micdoodle8.mods.galacticraft.core.Annotations.NetworkedField;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayList;
import java.util.Arrays;

public class TileEntityCircuitFabricator extends TileBaseElectricBlockWithInventory implements ISidedInventory, IPacketReceiver
{
    public static final int PROCESS_TIME_REQUIRED = 300;
    @NetworkedField(targetSide = Side.CLIENT)
    public int processTicks = 0;
    private ItemStack producingStack = null;
    private long ticks;

    private ItemStack[] containingItems = new ItemStack[7];

    public TileEntityCircuitFabricator()
    {
        this.storage.setMaxExtract(ConfigManagerCore.hardMode ? 40 : 20);
    }

    @Override
    public void update()
    {
        super.update();

        this.updateInput();

        if (!this.worldObj.isRemote)
        {
            boolean updateInv = false;

            if (this.hasEnoughEnergyToRun)
            {
                if (this.canCompress())
                {
                    ++this.processTicks;

                    if (this.processTicks == TileEntityCircuitFabricator.PROCESS_TIME_REQUIRED)
                    {
                        this.worldObj.playSound(null, this.getPos(), SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 0.3F, this.worldObj.rand.nextFloat() * 0.1F + 0.9F);
                        this.processTicks = 0;
                        this.compressItems();
                        updateInv = true;
                    }
                }
                else
                {
                    this.processTicks = 0;
                }
            }
            else
            {
                this.processTicks = 0;
            }

            if (updateInv)
            {
                this.markDirty();
            }
        }

        if (this.ticks >= Long.MAX_VALUE)
        {
            this.ticks = 0;
        }

        this.ticks++;
    }

    public void updateInput()
    {
        this.producingStack = CircuitFabricatorRecipes.getOutputForInput(Arrays.copyOfRange(this.containingItems, 1, 6));
    }

    private boolean canCompress()
    {
        ItemStack itemstack = this.producingStack;
        if (itemstack == null)
        {
            return false;
        }
        if (this.containingItems[6] == null)
        {
            return true;
        }
        if (this.containingItems[6] != null && !this.containingItems[6].isItemEqual(itemstack))
        {
            return false;
        }
        int result = this.containingItems[6] == null ? 0 : this.containingItems[6].stackSize + itemstack.stackSize;
        return result <= this.getInventoryStackLimit() && result <= itemstack.getMaxStackSize();
    }

    public void compressItems()
    {
        if (this.canCompress())
        {
            ItemStack resultItemStack = this.producingStack.copy();
            if (ConfigManagerCore.quickMode)
            {
                if (resultItemStack.getItem() == GCItems.basicItem)
                {
                    if (resultItemStack.getItemDamage() == ItemBasic.WAFER_BASIC)
                    {
                        resultItemStack.stackSize = 5;
                    }
                    else if (resultItemStack.getItemDamage() == ItemBasic.WAFER_ADVANCED)
                    {
                        resultItemStack.stackSize = 2;
                    }
                }
            }

            if (this.containingItems[6] == null)
            {
                this.containingItems[6] = resultItemStack;
            }
            else if (this.containingItems[6].isItemEqual(resultItemStack))
            {
                if (this.containingItems[6].stackSize + resultItemStack.stackSize > 64)
                {
                    for (int i = 0; i < this.containingItems[6].stackSize + resultItemStack.stackSize - 64; i++)
                    {
                        float var = 0.7F;
                        double dx = this.worldObj.rand.nextFloat() * var + (1.0F - var) * 0.5D;
                        double dy = this.worldObj.rand.nextFloat() * var + (1.0F - var) * 0.5D;
                        double dz = this.worldObj.rand.nextFloat() * var + (1.0F - var) * 0.5D;
                        EntityItem entityitem = new EntityItem(this.worldObj, this.getPos().getX() + dx, this.getPos().getY() + dy, this.getPos().getZ() + dz, new ItemStack(resultItemStack.getItem(), 1, resultItemStack.getItemDamage()));

                        entityitem.setPickupDelay(10);

                        this.worldObj.spawnEntityInWorld(entityitem);
                    }
                    this.containingItems[6].stackSize = 64;
                }
                else
                {
                    this.containingItems[6].stackSize += resultItemStack.stackSize;
                }
            }
        }

        for (int i = 1; i < 6; i++)
        {
            this.decrStackSize(i, 1);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt)
    {
        super.readFromNBT(nbt);
        this.processTicks = nbt.getInteger("smeltingTicks");
        this.containingItems = this.readStandardItemsFromNBT(nbt);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);
        nbt.setInteger("smeltingTicks", this.processTicks);
        this.writeStandardItemsToNBT(nbt);
        return nbt;
    }

    @Override
    protected ItemStack[] getContainingItems()
    {
        return this.containingItems;
    }

    @Override
    public String getName()
    {
        return GCCoreUtil.translate("tile.machine2.5.name");
    }

    @Override
    public boolean hasCustomName()
    {
        return false;
    }

    @Override
    public ITextComponent getDisplayName()
    {
        return null;
    }

    @Override
    public boolean isItemValidForSlot(int slotID, ItemStack itemStack)
    {
        if (slotID == 0)
        {
            return itemStack != null && ItemElectricBase.isElectricItem(itemStack.getItem());
        }

        if (slotID > 5)
        {
            return false;
        }

        ArrayList<ItemStack> list = CircuitFabricatorRecipes.slotValidItems.get(slotID - 1);

        for (ItemStack test : list)
        {
            if (test.isItemEqual(itemStack))
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public int[] getSlotsForFace(EnumFacing side)
    {
        if (side == EnumFacing.UP)
        {
            return new int[] { 6 };
        }

        //Offer whichever silicon slot has less silicon
        boolean siliconFlag = this.containingItems[2] != null && (this.containingItems[3] == null || this.containingItems[3].stackSize < this.containingItems[2].stackSize);
        return siliconFlag ? new int[] { 0, 1, 3, 4, 5 } : new int[] { 0, 1, 2, 4, 5 };
    }

    @Override
    public boolean canInsertItem(int slotID, ItemStack par2ItemStack, EnumFacing par3)
    {
        return slotID < 6 && this.isItemValidForSlot(slotID, par2ItemStack);
    }

    @Override
    public boolean canExtractItem(int slotID, ItemStack par2ItemStack, EnumFacing par3)
    {
        return slotID == 6;
    }

    @Override
    public boolean shouldUseEnergy()
    {
        return this.processTicks > 0;
    }

    @Override
    public EnumFacing getFront()
    {
        return this.worldObj.getBlockState(getPos()).getValue(BlockMachineMars.FACING);
    }

    @Override
    public EnumFacing getElectricInputDirection()
    {
        return getFront().rotateY();
    }
}
