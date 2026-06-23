package com.risudayooo.discgun.combat;

import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tiny server-side cooldown ledger keyed by player + action name, measured in
 * world ticks. Intentionally in-memory: cooldowns reset on server restart, which
 * is fine for a roguelike run. Authoritative gate for fire / blink / action / parry.
 */
public final class Cooldowns {
	private static final Map<UUID, Map<String, Long>> NEXT_READY = new HashMap<>();

	private Cooldowns() {
	}

	public static boolean ready(PlayerEntity player, String key) {
		long now = player.getWorld().getTime();
		Map<String, Long> perPlayer = NEXT_READY.get(player.getUuid());
		if (perPlayer == null) {
			return true;
		}
		return now >= perPlayer.getOrDefault(key, 0L);
	}

	public static void set(PlayerEntity player, String key, int ticks) {
		long ready = player.getWorld().getTime() + ticks;
		NEXT_READY.computeIfAbsent(player.getUuid(), k -> new HashMap<>()).put(key, ready);
	}

	/** Convenience: if ready, arm the cooldown and return true. */
	public static boolean tryUse(PlayerEntity player, String key, int ticks) {
		if (!ready(player, key)) {
			return false;
		}
		set(player, key, ticks);
		return true;
	}
}
