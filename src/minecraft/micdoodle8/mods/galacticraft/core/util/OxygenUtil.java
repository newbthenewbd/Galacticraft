package micdoodle8.mods.galacticraft.core.util;

import java.util.ArrayList;
import java.util.List;

import micdoodle8.mods.galacticraft.API.EnumGearType;
import micdoodle8.mods.galacticraft.API.IBreathableArmor;
import micdoodle8.mods.galacticraft.core.blocks.GCCoreBlockBreathableAir;
import micdoodle8.mods.galacticraft.core.entities.GCCorePlayerMP;
import micdoodle8.mods.galacticraft.core.inventory.GCCoreInventoryPlayer;
import micdoodle8.mods.galacticraft.core.items.GCCoreItemOxygenGear;
import micdoodle8.mods.galacticraft.core.items.GCCoreItemOxygenMask;
import micdoodle8.mods.galacticraft.core.items.GCCoreItemOxygenTank;
import micdoodle8.mods.galacticraft.core.tile.GCCoreTileEntityOxygenDistributor;
import net.minecraft.block.Block;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import universalelectricity.core.vector.Vector3;
import cpw.mods.fml.client.FMLClientHandler;

public class OxygenUtil
{
	public static boolean shouldDisplayTankGui(GuiScreen gui)
	{
		if (FMLClientHandler.instance().getClient().gameSettings.hideGUI)
		{
			return false;
		}

		if (gui == null)
		{
			return true;
		}

		if (gui instanceof GuiInventory)
		{
			return false;
		}

		if (gui instanceof GuiChat)
		{
			return true;
		}

		return false;
	}

    public static boolean isAABBInBreathableAirBlock(Entity entity)
    {
    	return isAABBInBreathableAirBlock(entity.worldObj, new Vector3(entity.boundingBox.minX, entity.boundingBox.minY, entity.boundingBox.minZ), new Vector3(entity.boundingBox.maxX + 1, entity.boundingBox.maxY + 1, entity.boundingBox.maxZ + 1), false);
    }

    public static boolean isAABBInBreathableAirBlock(World world, Vector3 minVec, Vector3 maxVec, boolean testAllPoints)
    {
        final int var3 = MathHelper.floor_double(minVec.x);
        final int var4 = MathHelper.floor_double(maxVec.x);
        final int var5 = MathHelper.floor_double(minVec.y);
        final int var6 = MathHelper.floor_double(maxVec.y);
        final int var7 = MathHelper.floor_double(minVec.z);
        final int var8 = MathHelper.floor_double(maxVec.z);

        final double avgX = (minVec.x + maxVec.x) / 2.0D;
        final double avgY = (minVec.y + maxVec.y) / 2.0D;
        final double avgZ = (minVec.z + maxVec.z) / 2.0D;

        final AxisAlignedBB box = AxisAlignedBB.getBoundingBox(minVec.x - 40, minVec.y - 40, minVec.z - 40, maxVec.x + 40, maxVec.y + 40, maxVec.z + 40);

        final List l = world.loadedTileEntityList;

        for (final Object o : l)
        {
        	if (o instanceof GCCoreTileEntityOxygenDistributor)
        	{
        		final GCCoreTileEntityOxygenDistributor distributor = (GCCoreTileEntityOxygenDistributor) o;

        		if (!distributor.worldObj.isRemote)
        		{
        			if (testAllPoints)
        			{
        				ArrayList<Vector3> vecs = new ArrayList<Vector3>();
        				vecs.add(minVec);
        				vecs.add(new Vector3(maxVec.x, minVec.y, minVec.z));
        				vecs.add(new Vector3(minVec.x, maxVec.y, minVec.z));
        				vecs.add(new Vector3(maxVec.x, maxVec.y, minVec.z));
        				vecs.add(new Vector3(minVec.x, maxVec.y, maxVec.z));
        				vecs.add(new Vector3(maxVec.x, minVec.y, maxVec.z));
        				vecs.add(new Vector3(maxVec.x, minVec.y, minVec.z));
        				vecs.add(minVec);
        				
        				for (Vector3 vec : vecs)
        				{
                			final double dist = distributor.getDistanceFromServer(vec.x, vec.y, vec.z);

                			if (Math.sqrt(dist) < distributor.storedOxygen / 600.0D)
                			{
                				return true;
                			}
        				}
        			}
        			else
        			{
            			final double dist = distributor.getDistanceFromServer(avgX, avgY, avgZ);

            			if (Math.sqrt(dist) < distributor.storedOxygen / 600.0D)
            			{
            				return true;
            			}
        			}
        		}
        	}
        }

        for (int var9 = var3; var9 < var4; ++var9)
        {
            for (int var10 = var5; var10 < var6; ++var10)
            {
                for (int var11 = var7; var11 < var8; ++var11)
                {
                    final Block var12 = Block.blocksList[world.getBlockId(var9, var10, var11)];

                    if (var12 != null && var12 instanceof GCCoreBlockBreathableAir)
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static int getDrainSpacing(ItemStack tank)
    {
		if (tank == null)
		{
			return 0;
		}

		if (tank.getItem() instanceof GCCoreItemOxygenTank)
		{
			return 360;
		}

		return 0;
    }

	public static boolean hasValidOxygenSetup(GCCorePlayerMP player)
	{
		boolean missingComponent = false;

		if (((GCCoreInventoryPlayer)player.inventory).tankItemInSlot(0) == null || !OxygenUtil.isItemValidForPlayerTankInv(0, ((GCCoreInventoryPlayer)player.inventory).tankItemInSlot(0)))
		{
			boolean handled = false;
			
			for (final ItemStack armorStack : player.inventory.armorInventory)
			{
				if (armorStack != null && armorStack.getItem() instanceof IBreathableArmor)
				{
					final IBreathableArmor breathableArmor = (IBreathableArmor) armorStack.getItem();

					if (breathableArmor.handleGearType(EnumGearType.HELMET))
					{
						if (breathableArmor.canBreathe(armorStack, player, EnumGearType.HELMET))
						{
							handled = true;
						}
					}
				}
			}
			
			if (!handled)
			{
				missingComponent = true;
			}
		}

		if (((GCCoreInventoryPlayer)player.inventory).tankItemInSlot(1) == null || !OxygenUtil.isItemValidForPlayerTankInv(1, ((GCCoreInventoryPlayer)player.inventory).tankItemInSlot(1)))
		{
			boolean handled = false;
			
			for (final ItemStack armorStack : player.inventory.armorInventory)
			{
				if (armorStack != null && armorStack.getItem() instanceof IBreathableArmor)
				{
					final IBreathableArmor breathableArmor = (IBreathableArmor) armorStack.getItem();
	
					if (breathableArmor.handleGearType(EnumGearType.GEAR))
					{
						if (breathableArmor.canBreathe(armorStack, player, EnumGearType.GEAR))
						{
							handled = true;
						}
					}
				}
			}
			
			if (!handled)
			{
				missingComponent = true;
			}
		}

		if ((((GCCoreInventoryPlayer)player.inventory).tankItemInSlot(2) == null || !OxygenUtil.isItemValidForPlayerTankInv(2, ((GCCoreInventoryPlayer)player.inventory).tankItemInSlot(2))) && (((GCCoreInventoryPlayer)player.inventory).tankItemInSlot(3) == null || !OxygenUtil.isItemValidForPlayerTankInv(3, ((GCCoreInventoryPlayer)player.inventory).tankItemInSlot(3))))
		{
			boolean handled = false;
			
			for (final ItemStack armorStack : player.inventory.armorInventory)
			{
				if (armorStack != null && armorStack.getItem() instanceof IBreathableArmor)
				{
					final IBreathableArmor breathableArmor = (IBreathableArmor) armorStack.getItem();

					if (breathableArmor.handleGearType(EnumGearType.TANK1))
					{
						if (breathableArmor.canBreathe(armorStack, player, EnumGearType.TANK1))
						{
							handled = true;
						}
					}

					if (breathableArmor.handleGearType(EnumGearType.TANK2))
					{
						if (breathableArmor.canBreathe(armorStack, player, EnumGearType.TANK2))
						{
							handled = true;
						}
					}
				}
			}
			
			if (!handled)
			{
				missingComponent = true;
			}
		}

		return !missingComponent;
	}

	public static boolean isItemValidForPlayerTankInv(int slotIndex, ItemStack stack)
	{
		switch (slotIndex)
		{
		case 0:
			return stack.getItem() instanceof GCCoreItemOxygenMask;
		case 1:
			return stack.getItem() instanceof GCCoreItemOxygenGear;
		case 2:
			return OxygenUtil.getDrainSpacing(stack) > 0;
		case 3:
			return OxygenUtil.getDrainSpacing(stack) > 0;
		}

		return false;
	}

    public static boolean isBlockGettingOxygen(World world, int par1, int par2, int par3)
    {
    	return true; // TODO
//        return isBlockProvidingOxygenTo(world, par1, par2 - 1, par3, 0) ? true : isBlockProvidingOxygenTo(world, par1, par2 + 1, par3, 1) ? true : isBlockProvidingOxygenTo(world, par1, par2, par3 - 1, 2) ? true : isBlockProvidingOxygenTo(world, par1, par2, par3 + 1, 3) ? true : isBlockProvidingOxygenTo(world, par1 - 1, par2, par3, 4) ? true : isBlockProvidingOxygenTo(world, par1 + 1, par2, par3, 5);
    }

//    public static boolean isBlockProvidingOxygenTo(World world, int par1, int par2, int par3, int par4)
//    {
//    	final TileEntity te = world.getBlockTileEntity(par1, par2, par3);
//
//    	if (te != null && te instanceof TileEntityOxygenTransmitter)
//    	{
//    		final TileEntityOxygenTransmitter teot = (TileEntityOxygenTransmitter) te;
//
//    		if (teot.getOxygenInTransmitter() > 1.0D)
//    		{
//    			return true;
//    		}
//    	}
//
//    	return false;
//    }
}
