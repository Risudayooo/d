package com.risudayooo.discgun.registry;

import com.mojang.serialization.Codec;
import com.risudayooo.discgun.DiscGunMod;
import com.risudayooo.discgun.disc.DiscData;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/**
 * Data component types. The loaded disc and the current ammo live on the gun
 * item stack so they travel with it and sync to the client (the HUD reads both).
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

	public static final ComponentType<Integer> AMMO = Registry.register(
			Registries.DATA_COMPONENT_TYPE,
			DiscGunMod.id("ammo"),
			ComponentType.<Integer>builder()
					.codec(Codec.INT)
					.packetCodec(PacketCodecs.INTEGER)
					.build()
	);

	private ModComponents() {
	}

	public static void initialize() {
		// Triggers class load + registration.
	}
}
