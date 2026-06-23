package com.risudayooo.discgun.combat;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks the active parry window per player (8章). When {@link #begin} is called
 * the player ignores damage until the window expires; {@link #consume} fires on a
 * successful parry to give feedback and end the window early.
 */
public final class ParryState {
	/** Window length in ticks (~0.25s at 20 tps). */
	public static final int WINDOW_TICKS = 5;

	private static final Map<UUID, Long> EXPIRY = new HashMap<>();

	private ParryState() {
	}

	public static void begin(ServerPlayerEntity player) {
		long expiry = player.getWorld().getTime() + WINDOW_TICKS;
		EXPIRY.put(player.getUuid(), expiry);
		player.getServerWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 0.6f, 1.6f);
	}

	public static boolean isActive(ServerPlayerEntity player) {
		Long expiry = EXPIRY.get(player.getUuid());
		return expiry != null && player.getWorld().getTime() <= expiry;
	}

	/** Called on a successful parry: feedback + close the window. */
	public static void consume(ServerPlayerEntity player) {
		EXPIRY.remove(player.getUuid());
		player.getServerWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.5f, 1.8f);
	}
}
