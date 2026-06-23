package com.risudayooo.discgun.item;

import com.risudayooo.discgun.disc.DiscData;
import com.risudayooo.discgun.disc.DiscType;
import com.risudayooo.discgun.registry.ModComponents;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * The gun-shaped CD player (3章). The actual firing / action logic lives in the
 * loaded disc's {@link com.risudayooo.discgun.disc.DiscAbility}; this item is just
 * the carrier + the data component holder.
 *
 * <p>Left/right click are driven client-side (see the client mixin + input
 * handler) rather than vanilla {@code use}, so vanilla interactions are suppressed
 * while the gun is held.
 */
public class GunCDPlayerItem extends Item {
	public GunCDPlayerItem(Settings settings) {
		super(settings);
	}

	/** Resolve the currently loaded disc, defaulting to House when empty. */
	public static DiscType getDisc(ItemStack stack) {
		DiscData data = stack.get(ModComponents.DISC_DATA);
		return data != null ? data.type() : DiscType.HOUSE;
	}

	public static void setDisc(ItemStack stack, DiscType type) {
		stack.set(ModComponents.DISC_DATA, DiscData.of(type));
	}

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
		DiscType disc = getDisc(stack);
		tooltip.add(Text.translatable("tooltip.discgun.loaded_disc")
				.append(Text.translatable(disc.translationKey()).formatted(disc.color())));
		tooltip.add(Text.translatable("tooltip.discgun.controls").formatted(Formatting.DARK_GRAY));
	}
}
