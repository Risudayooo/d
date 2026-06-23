package com.risudayooo.discgun.registry;

import com.risudayooo.discgun.DiscGunMod;
import com.risudayooo.discgun.disc.DiscType;
import com.risudayooo.discgun.item.DiscItem;
import com.risudayooo.discgun.item.GunCDPlayerItem;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;

/**
 * Item + creative-tab registration.
 */
public final class ModItems {
	public static final Item GUN_CD_PLAYER = register("gun_cd_player",
			new GunCDPlayerItem(new Item.Settings().maxCount(1)));

	public static final Item DISC_HOUSE = register("disc_house",
			new DiscItem(new Item.Settings().maxCount(16), DiscType.HOUSE));
	public static final Item DISC_DUBSTEP = register("disc_dubstep",
			new DiscItem(new Item.Settings().maxCount(16), DiscType.DUBSTEP));
	public static final Item DISC_DNB = register("disc_dnb",
			new DiscItem(new Item.Settings().maxCount(16), DiscType.DNB));

	public static final ItemGroup GROUP = Registry.register(Registries.ITEM_GROUP,
			DiscGunMod.id("general"),
			FabricItemGroup.builder()
					.icon(() -> new ItemStack(GUN_CD_PLAYER))
					.displayName(Text.translatable("itemGroup.discgun.general"))
					.entries((context, entries) -> {
						entries.add(GUN_CD_PLAYER);
						entries.add(DISC_HOUSE);
						entries.add(DISC_DUBSTEP);
						entries.add(DISC_DNB);
					})
					.build());

	private ModItems() {
	}

	private static Item register(String path, Item item) {
		return Registry.register(Registries.ITEM, DiscGunMod.id(path), item);
	}

	public static void initialize() {
		// Triggers class load + registration.
	}
}
