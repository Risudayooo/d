package com.risudayooo.discgun.disc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

/**
 * The data component stored on a gun item describing which disc is loaded.
 *
 * <p>Kept intentionally small for Phase 1: just the disc id. Ammo / charge state
 * for Dubstep-style charge weapons can be added here later.
 */
public record DiscData(String discId) {
	public static final Codec<DiscData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.STRING.fieldOf("disc").forGetter(DiscData::discId)
	).apply(instance, DiscData::new));

	// Required so the component syncs to the client (the HUD / firing cadence reads it).
	public static final PacketCodec<RegistryByteBuf, DiscData> PACKET_CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, DiscData::discId,
			DiscData::new
	);

	public DiscType type() {
		return DiscType.byId(discId);
	}

	public static DiscData of(DiscType type) {
		return new DiscData(type.id());
	}
}
