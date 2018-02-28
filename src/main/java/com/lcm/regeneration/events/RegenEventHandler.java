package com.lcm.regeneration.events;

import com.lcm.regeneration.RegenConfig;
import com.lcm.regeneration.init.RegenItems;
import com.lcm.regeneration.init.RegenSounds;
import com.lcm.regeneration.superpower.TimelordSuperpower;
import com.lcm.regeneration.superpower.TimelordSuperpowerHandler;
import com.lcm.regeneration.util.ExplosionUtil;

import lucraft.mods.lucraftcore.superpowers.SuperpowerHandler;
import lucraft.mods.lucraftcore.superpowers.capabilities.CapabilitySuperpower;
import lucraft.mods.lucraftcore.util.helper.StringHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber
public class RegenEventHandler {
	
	@SubscribeEvent
	public static void onWorldLoaded(WorldEvent.Load e) {
		if (!e.getWorld().isRemote && RegenConfig.disableTraits) for (EntityPlayer p : e.getWorld().playerEntities)
			if (SuperpowerHandler.hasSuperpower(p, TimelordSuperpower.INSTANCE)) SuperpowerHandler.getSuperpowerPlayerHandler(p).getAbilities().forEach(ability -> ability.setUnlocked(false));
	}
	
	@SubscribeEvent
	public static void onAttacked(LivingAttackEvent e) {
		if (!(e.getEntity() instanceof EntityPlayer) || !SuperpowerHandler.hasSuperpower((EntityPlayer) e.getEntity(), TimelordSuperpower.INSTANCE)) return;
		EntityPlayer player = (EntityPlayer) e.getEntity();
		if (player.getHealth() - e.getAmount() < 0 && SuperpowerHandler.getSpecificSuperpowerPlayerHandler(player, TimelordSuperpowerHandler.class).regenerating && player.world.isRemote && Minecraft.getMinecraft().player.getUniqueID() == player.getUniqueID()) Minecraft.getMinecraft().gameSettings.thirdPersonView = 0;
	}
	
	@SubscribeEvent
	public static void onPlayerInteract(PlayerInteractEvent e) {
		if (e.getSide() == Side.CLIENT || !SuperpowerHandler.hasSuperpower(e.getEntityPlayer(), TimelordSuperpower.INSTANCE)) return;
		if (SuperpowerHandler.getSpecificSuperpowerPlayerHandler(e.getEntityPlayer(), TimelordSuperpowerHandler.class).regenerating) e.setCanceled(true);
	}
	
	@SubscribeEvent
	public static void onKnockback(LivingKnockBackEvent e) {
		if (!(e.getEntity() instanceof EntityPlayer)) return;
		EntityPlayer player = ((EntityPlayer) e.getEntity());
		if (!SuperpowerHandler.hasSuperpower(player, TimelordSuperpower.INSTANCE)) return;
		if (SuperpowerHandler.getSpecificSuperpowerPlayerHandler(player, TimelordSuperpowerHandler.class).regenerating) e.setCanceled(true);
	}
	
	@SubscribeEvent
	public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
		if (!RegenConfig.startAsTimelord || !e.player.world.isRemote) return;
		
		NBTTagCompound nbt = e.player.getEntityData();
		boolean loggedInBefore = nbt.getBoolean("loggedInBefore");
		if (!loggedInBefore) {
			e.player.inventory.addItemStackToInventory(new ItemStack(RegenItems.chameleonArch));
			nbt.setBoolean("loggedInBefore", true);
		}
	}
	
	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onHurt(LivingHurtEvent e) {
		if (!(e.getEntity() instanceof EntityPlayer)) return;
		
		EntityPlayer player = (EntityPlayer) e.getEntity();
		if (player.getHealth() + player.getAbsorptionAmount() - e.getAmount() > 0 || !SuperpowerHandler.hasSuperpower(player, TimelordSuperpower.INSTANCE)) return;
		
		TimelordSuperpowerHandler handler = SuperpowerHandler.getSpecificSuperpowerPlayerHandler(player, TimelordSuperpowerHandler.class);
		
		if ((handler.regenerating || player.posY < 0 || handler.regenerationsLeft == 0) && !RegenConfig.dontLoseUponDeath) {
			SuperpowerHandler.removeSuperpower(player);
			((CapabilitySuperpower) player.getCapability(CapabilitySuperpower.SUPERPOWER_CAP, null)).superpowerData.removeTag(TimelordSuperpower.INSTANCE.getRegistryName().toString());
		} else if (handler.regenerationsLeft > 0 || handler.regenerationsLeft == -1) { // initiate regeneration
			e.setCanceled(true);
			handler.regenerating = true;
			SuperpowerHandler.syncToAll(player);
			
			player.setHealth(.5f);
			player.setAbsorptionAmount(RegenConfig.absorbtionLevel);
			if (RegenConfig.resetOxygen) player.setAir(300);
			if (RegenConfig.resetHunger) player.getFoodStats().setFoodLevel(20);
			player.clearActivePotions();
			player.addPotionEffect(new PotionEffect(Potion.getPotionById(10), 10 * 20, RegenConfig.regenerationLevel, false, false)); // 10 seconds of 20 ticks of Regeneration 2
			player.extinguish();
			
			String time = "" + (handler.timesRegenerated + 1);
			int lastDigit = handler.timesRegenerated;
			if (lastDigit > 20) while (lastDigit > 10)
				lastDigit -= 10;
			
			if (lastDigit < 3)
				time = time + StringHelper.translateToLocal("lcm-regen.messages.numsuffix." + lastDigit);
			else
				time = time + StringHelper.translateToLocal("lcm-regen.messages.numsuffix.ext");
			
			if (handler.regenerationsLeft != -1) player.sendStatusMessage(new TextComponentString(StringHelper.translateToLocal("lcm-regen.messages.regenLeftExt", time, (handler.regenerationsLeft - 1))), true);
			player.world.playSound(null, player.posX, player.posY, player.posZ, RegenSounds.REGENERATION, SoundCategory.PLAYERS, 1.0F, 1.0F);
			ExplosionUtil.regenerationExplosion(player);
		}
	}
}
