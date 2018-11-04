package me.fril.regeneration.common.items;

import me.fril.regeneration.common.capability.CapabilityRegeneration;
import me.fril.regeneration.common.capability.IRegeneration;
import me.fril.regeneration.util.PlayerUtil;
import me.fril.regeneration.util.RegenConfig;
import me.fril.regeneration.util.RegenObjects;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

/**
 * Created by Sub
 * on 16/09/2018.
 */
public class ItemFobWatch extends Item {
	
	public ItemFobWatch() {
		setMaxDamage(RegenConfig.regenCapacity);
		setCreativeTab(CreativeTabs.MISC);
		setMaxStackSize(1);
	}
	
	@Override
	public void onCreated(ItemStack stack, World worldIn, EntityPlayer playerIn) {
		super.onCreated(stack, worldIn, playerIn);
		stack.setItemDamage(RegenConfig.regenCapacity);
	}
	
	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
		IRegeneration cap = CapabilityRegeneration.getForPlayer(player);
		ItemStack stack = player.getHeldItem(hand);
		
		if (!player.isSneaking()) { //transferring watch->player
			if (stack.getItemDamage() == RegenConfig.regenCapacity) {
				return usageFailed(player, "regeneration.messages.transfer.empty_watch", stack);
			} else if (cap.getRegenerationsLeft() == RegenConfig.regenCapacity) {
				return usageFailed(player, "regeneration.messages.transfer.max_regens", stack);
			}
			
			int supply = RegenConfig.regenCapacity - stack.getItemDamage(),
				needed = RegenConfig.regenCapacity - cap.getRegenerationsLeft(),
				used = Math.min(supply, needed);
			
			if (cap.canRegenerate())
				PlayerUtil.sendMessage(player, new TextComponentTranslation("regeneration.messages.gained_regens", used), true);
			else
				PlayerUtil.sendMessage(player, new TextComponentTranslation("regeneration.messages.now_timelord"), true);
			
			cap.receiveRegenerations(used);
			
			if (!cap.getPlayer().isCreative())
				stack.setItemDamage(stack.getItemDamage() + used);
			
			world.playSound(null, player.posX, player.posY, player.posZ, RegenObjects.Sounds.FOB_WATCH, SoundCategory.PLAYERS, 0.5F, 1.0F);
		} else { //transferring player->watch
			if (stack.getItemDamage() == 0) {
				return usageFailed(player, cap.canRegenerate() ? "regeneration.messages.transfer.full_watch" : "regeneration.messages.transfer.no_regens", stack);
			}
			
			stack.setItemDamage(stack.getItemDamage() - 1);
			cap.extractRegeneration(1);
			PlayerUtil.sendMessage(player, "regeneration.messages.transfer.success", true);
			return new ActionResult<>(EnumActionResult.PASS, stack);
		}
		
		return super.onItemRightClick(world, player, hand);
	}
	
	private ActionResult<ItemStack> usageFailed(EntityPlayer player, String message, ItemStack stack) {
		PlayerUtil.sendMessage(player, message, true);
		return ActionResult.newResult(EnumActionResult.FAIL, stack);
	}
	
}
