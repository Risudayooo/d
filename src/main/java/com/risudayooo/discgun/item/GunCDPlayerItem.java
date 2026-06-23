package com.risudayooo.discgun.item;

import com.risudayooo.discgun.disc.DiscData;
import com.risudayooo.discgun.disc.DiscType;
import com.risudayooo.discgun.registry.ModComponents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * The gun-shaped CD player (3章). Firing / action logic lives in the loaded
 * disc's {@link com.risudayooo.discgun.disc.DiscAbility}; this item carries the
 * disc + the current ammo as data components.
 *
 * <p>Left/right click are driven client-side (mixin + input handler), so vanilla
 * interactions are suppressed while the gun is held.
 */
public class GunCDPlayerItem extends Item {
	public GunCDPlayerItem(Settings settings) {
		super(settings);
	}

	public static DiscType getDisc(ItemStack stack) {
		DiscData data = stack.get(ModComponents.DISC_DATA);
		return data != null ? data.type() : DiscType.HOUSE;
	}

	/** Loading a disc also refills the magazine to that disc's capacity. */
	public static void setDisc(ItemStack stack, DiscType type) {
		stack.set(ModComponents.DISC_DATA, DiscData.of(type));
		setAmmo(stack, type.ability().magazineSize());
	}

	/** Current ammo; a fresh stack (no component yet) reports a full magazine. */
	public static int getAmmo(ItemStack stack) {
		Integer ammo = stack.get(ModComponents.AMMO);
		return ammo != null ? ammo : getDisc(stack).ability().magazineSize();
	}

	public static void setAmmo(ItemStack stack, int ammo) {
		stack.set(ModComponents.AMMO, Math.max(0, ammo));
	}

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
		DiscType disc = getDisc(stack);
		tooltip.add(Text.translatable("tooltip.discgun.loaded_disc")
				.append(Text.translatable(disc.translationKey()).formatted(disc.color())));
		tooltip.add(Text.translatable("tooltip.discgun.ammo",
				getAmmo(stack), disc.ability().magazineSize()).formatted(Formatting.GRAY));
		tooltip.add(Text.translatable("tooltip.discgun.controls").formatted(Formatting.DARK_GRAY));
	}
}
