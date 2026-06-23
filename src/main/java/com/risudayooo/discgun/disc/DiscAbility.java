package com.risudayooo.discgun.disc;

import net.minecraft.server.network.ServerPlayerEntity;

/**
 * The single "ability interface" from the design doc (4章). Each disc implements
 * its left-click (fire) behaviour, its right-click action, and the cadences that
 * gate them. BGM hook will be added in Phase 2 when the music subsystem lands.
 *
 * <p>All methods run server-side; the client only sends intent packets.
 */
public interface DiscAbility {
	/** Minimum ticks between consecutive fires (left click). */
	int fireCooldownTicks();

	/** Cooldown for the right-click action, in ticks. */
	int actionCooldownTicks();

	/** Left click: the disc's "通常攻撃". */
	void onFire(ServerPlayerEntity player);

	/** Right click: the disc's signature action. */
	void onAction(ServerPlayerEntity player);
}
