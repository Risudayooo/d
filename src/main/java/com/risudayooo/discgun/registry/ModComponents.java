package com.risudayooo.discgun.registry;

import com.risudayooo.discgun.DiscGunMod;
import com.risudayooo.discgun.disc.DiscData;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/**
 * Data component types. The loaded disc lives on the gun item stack as
 * {@link #DISC_DATA} so it travels with the item and syncs to the client.
 */
public final class ModComponents {
	public static final ComponentType<DiscData> DISC_DATA = Registry.register(
			Registries.DATA_COMPONENT_TYPE,
			DiscGunMod.id("disc_data"),
			ComponentType.<DiscData>builder()
					.codec(DiscData.CODEC)
					.packetCodec(DiscData.PACKET_CODEC)
					.build()
	);

	private ModComponents() {
	}

	public static void initialize() {
		// Triggers class load + registration.
	}
}
