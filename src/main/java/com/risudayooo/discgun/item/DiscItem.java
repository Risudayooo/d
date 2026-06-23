package com.risudayooo.discgun.item;

import com.risudayooo.discgun.disc.DiscType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * A physical disc the player collects during a run (4章). The real exchange flow
 * is the jukebox station (Phase 2, open-questions #3); for Phase 1 testing,
 * right-clicking a disc loads it into the first gun found in the player's
 * inventory. This is a temporary dev affordance.
 */
public class DiscItem extends Item {
	private final DiscType type;

	public DiscItem(Settings settings, DiscType type) {
		super(settings);
		this.type = type;
	}

	public DiscType discType() {
		return type;
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
		if (!world.isClient) {
			ItemStack gun = findGun(player);
			if (gun != null) {
				GunCDPlayerItem.setDisc(gun, type);
				player.sendMessage(Text.translatable("message.discgun.disc_loaded",
						Text.translatable(type.translationKey()).formatted(type.color())), true);
			} else {
				player.sendMessage(Text.translatable("message.discgun.no_gun"), true);
			}
		}
		return TypedActionResult.success(player.getStackInHand(hand), world.isClient);
	}

	private static ItemStack findGun(PlayerEntity player) {
		for (int i = 0; i < player.getInventory().size(); i++) {
			ItemStack stack = player.getInventory().getStack(i);
			if (stack.getItem() instanceof GunCDPlayerItem) {
				return stack;
			}
		}
		return null;
	}
}
