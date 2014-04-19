package micdoodle8.mods.galacticraft.mars.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import micdoodle8.mods.galacticraft.core.entities.player.GCEntityPlayerMP;
import micdoodle8.mods.galacticraft.core.entities.player.GCEntityClientPlayerMP;
import micdoodle8.mods.galacticraft.core.network.IPacket;
import micdoodle8.mods.galacticraft.core.network.NetworkUtil;
import micdoodle8.mods.galacticraft.core.util.GCLog;
import micdoodle8.mods.galacticraft.core.util.PlayerUtil;
import micdoodle8.mods.galacticraft.mars.client.gui.GCMarsGuiCargoRocket;
import micdoodle8.mods.galacticraft.mars.client.gui.GCMarsGuiSlimelingInventory;
import micdoodle8.mods.galacticraft.mars.entities.EntityCargoRocket;
import micdoodle8.mods.galacticraft.mars.entities.EntitySlimeling;
import micdoodle8.mods.galacticraft.mars.tile.GCMarsTileEntityCryogenicChamber;
import micdoodle8.mods.galacticraft.mars.tile.GCMarsTileEntityLaunchController;
import micdoodle8.mods.galacticraft.mars.util.GCMarsUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.tileentity.TileEntity;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class PacketSimpleMars implements IPacket
{
	public static enum EnumSimplePacketMars
	{
		// SERVER
		S_UPDATE_SLIMELING_DATA(Side.SERVER, Integer.class, Integer.class, String.class),
		S_WAKE_PLAYER(Side.SERVER),
		S_UPDATE_ADVANCED_GUI(Side.SERVER, Integer.class, Integer.class, Integer.class, Integer.class, Integer.class),
		S_UPDATE_CARGO_ROCKET_STATUS(Side.SERVER, Integer.class, Integer.class),
		// CLIENT
		C_OPEN_CUSTOM_GUI(Side.CLIENT, Integer.class, Integer.class, Integer.class),
		C_BEGIN_CRYOGENIC_SLEEP(Side.CLIENT, Integer.class, Integer.class, Integer.class);

		private Side targetSide;
		private Class<?>[] decodeAs;

		private EnumSimplePacketMars(Side targetSide, Class<?>... decodeAs)
		{
			this.targetSide = targetSide;
			this.decodeAs = decodeAs;
		}

		public Side getTargetSide()
		{
			return this.targetSide;
		}

		public Class<?>[] getDecodeClasses()
		{
			return this.decodeAs;
		}
	}

	private EnumSimplePacketMars type;
	private List<Object> data;

	public PacketSimpleMars()
	{

	}

	public PacketSimpleMars(EnumSimplePacketMars packetType, Object[] data)
	{
		this(packetType, Arrays.asList(data));
	}

	public PacketSimpleMars(EnumSimplePacketMars packetType, List<Object> data)
	{
		if (packetType.getDecodeClasses().length != data.size())
		{
			GCLog.info("Simple Packet found data length different than packet type");
		}

		this.type = packetType;
		this.data = data;
	}

	@Override
	public void encodeInto(ChannelHandlerContext context, ByteBuf buffer)
	{
		buffer.writeInt(this.type.ordinal());

		try
		{
			NetworkUtil.encodeData(buffer, this.data);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void decodeInto(ChannelHandlerContext context, ByteBuf buffer)
	{
		this.type = EnumSimplePacketMars.values()[buffer.readInt()];

		if (this.type.getDecodeClasses().length > 0)
		{
			this.data = NetworkUtil.decodeData(this.type.getDecodeClasses(), buffer);
		}
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void handleClientSide(EntityPlayer player)
	{
		GCEntityClientPlayerMP playerBaseClient = null;

		if (player instanceof GCEntityClientPlayerMP)
		{
			playerBaseClient = (GCEntityClientPlayerMP) player;
		}

		switch (this.type)
		{
		case C_OPEN_CUSTOM_GUI:
			int entityID = 0;
			Entity entity = null;
			FMLLog.info("done c " + (Integer) this.data.get(1));

			switch ((Integer) this.data.get(1))
			{
			case 0:
				entityID = (Integer) this.data.get(2);
				entity = player.worldObj.getEntityByID(entityID);

				if (entity != null && entity instanceof EntitySlimeling)
				{
					FMLClientHandler.instance().getClient().displayGuiScreen(new GCMarsGuiSlimelingInventory(player, (EntitySlimeling) entity));
				}

				player.openContainer.windowId = (Integer) this.data.get(0);
				break;
			case 1:
				entityID = (Integer) this.data.get(2);
				entity = player.worldObj.getEntityByID(entityID);

				if (entity != null && entity instanceof EntityCargoRocket)
				{
					FMLClientHandler.instance().getClient().displayGuiScreen(new GCMarsGuiCargoRocket(player.inventory, (EntityCargoRocket) entity));
				}

				player.openContainer.windowId = (Integer) this.data.get(0);
				break;
			}
		case C_BEGIN_CRYOGENIC_SLEEP:
			TileEntity tile = player.worldObj.getTileEntity((Integer)this.data.get(0), (Integer)this.data.get(1), (Integer)this.data.get(2));
			
			if (tile instanceof GCMarsTileEntityCryogenicChamber)
			{
				((GCMarsTileEntityCryogenicChamber) tile).sleepInBedAt(player, (Integer)this.data.get(0), (Integer)this.data.get(1), (Integer)this.data.get(2));
			}
		default:
			break;
		}
	}

	@Override
	public void handleServerSide(EntityPlayer player)
	{
		GCEntityPlayerMP playerBase = PlayerUtil.getPlayerBaseServerFromPlayer(player, false);

		switch (this.type)
		{
		case S_UPDATE_SLIMELING_DATA:
			Entity entity = player.worldObj.getEntityByID((Integer) this.data.get(0));

			if (entity instanceof EntitySlimeling)
			{
				EntitySlimeling slimeling = (EntitySlimeling) entity;

				int subType = (Integer) this.data.get(1);

				switch (subType)
				{
				case 0:
					if (player.getCommandSenderName().equalsIgnoreCase(slimeling.getOwnerName()) && !slimeling.worldObj.isRemote)
					{
						slimeling.getAiSit().setSitting(!slimeling.isSitting());
						slimeling.setJumping(false);
						slimeling.setPathToEntity((PathEntity) null);
						slimeling.setTarget((Entity) null);
						slimeling.setAttackTarget((EntityLivingBase) null);
					}
					break;
				case 1:
					if (player.getCommandSenderName().equalsIgnoreCase(slimeling.getOwnerName()) && !slimeling.worldObj.isRemote)
					{
						slimeling.slimelingName = (String) this.data.get(2);
					}
					break;
				case 2:
					if (player.getCommandSenderName().equalsIgnoreCase(slimeling.getOwnerName()) && !slimeling.worldObj.isRemote)
					{
						slimeling.age += 5000;
					}
					break;
				case 3:
					if (!slimeling.isInLove() && player.getCommandSenderName().equalsIgnoreCase(slimeling.getOwnerName()) && !slimeling.worldObj.isRemote)
					{
						slimeling.func_146082_f(playerBase);
					}
					break;
				case 4:
					if (player.getCommandSenderName().equalsIgnoreCase(slimeling.getOwnerName()) && !slimeling.worldObj.isRemote)
					{
						slimeling.attackDamage = Math.min(slimeling.attackDamage + 0.1F, 1.0F);
					}
					break;
				case 5:
					if (player.getCommandSenderName().equalsIgnoreCase(slimeling.getOwnerName()) && !slimeling.worldObj.isRemote)
					{
						slimeling.setHealth(slimeling.getHealth() + 5.0F);
					}
					break;
				case 6:
					if (player.getCommandSenderName().equalsIgnoreCase(slimeling.getOwnerName()) && !slimeling.worldObj.isRemote)
					{
						GCMarsUtil.openSlimelingInventory(playerBase, slimeling);
					}
					break;
				}
			}
			break;
		case S_WAKE_PLAYER:
			playerBase.wakeUpPlayer(false, true, true, true);
			break;
		case S_UPDATE_ADVANCED_GUI:
			TileEntity tile = player.worldObj.getTileEntity((Integer) this.data.get(1), (Integer) this.data.get(2), (Integer) this.data.get(3));

			switch ((Integer) this.data.get(0))
			{
			case 0:
				if (tile instanceof GCMarsTileEntityLaunchController)
				{
					GCMarsTileEntityLaunchController launchController = (GCMarsTileEntityLaunchController) tile;
					launchController.setFrequency((Integer) this.data.get(4));
				}
				break;
			case 1:
				if (tile instanceof GCMarsTileEntityLaunchController)
				{
					GCMarsTileEntityLaunchController launchController = (GCMarsTileEntityLaunchController) tile;
					launchController.launchDropdownSelection = (Integer) this.data.get(4);
				}
				break;
			case 2:
				if (tile instanceof GCMarsTileEntityLaunchController)
				{
					GCMarsTileEntityLaunchController launchController = (GCMarsTileEntityLaunchController) tile;
					launchController.setDestinationFrequency((Integer) this.data.get(4));
				}
				break;
			case 3:
				if (tile instanceof GCMarsTileEntityLaunchController)
				{
					GCMarsTileEntityLaunchController launchController = (GCMarsTileEntityLaunchController) tile;
					launchController.launchPadRemovalDisabled = (Integer) this.data.get(4) == 1 ? true : false;
				}
				break;
			case 4:
				if (tile instanceof GCMarsTileEntityLaunchController)
				{
					GCMarsTileEntityLaunchController launchController = (GCMarsTileEntityLaunchController) tile;
					launchController.launchSchedulingEnabled = (Integer) this.data.get(4) == 1 ? true : false;
				}
				break;
			case 5:
				if (tile instanceof GCMarsTileEntityLaunchController)
				{
					GCMarsTileEntityLaunchController launchController = (GCMarsTileEntityLaunchController) tile;
					launchController.requiresClientUpdate = true;
				}
				break;
			default:
				break;
			}
			break;
		case S_UPDATE_CARGO_ROCKET_STATUS:
			Entity entity2 = player.worldObj.getEntityByID((Integer) this.data.get(0));

			if (entity2 instanceof EntityCargoRocket)
			{
				EntityCargoRocket rocket = (EntityCargoRocket) entity2;

				int subType = (Integer) this.data.get(1);

				switch (subType)
				{
				default:
					rocket.statusValid = rocket.checkLaunchValidity();
					break;
				}
			}
			break;
		default:
			break;
		}
	}
}
