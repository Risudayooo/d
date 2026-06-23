package com.risudayooo.discgun.disc.ability;

import com.risudayooo.discgun.combat.GunMechanics;
import com.risudayooo.discgun.disc.DiscAbility;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;

/**
 * House — the neutral baseline (4章). Steady mid-tempo auto fire, and a short
 * teleport on the right click that leans into its "軽快な移動" feel.
 */
public class HouseAbility implements DiscAbility {
	@Override
	public int fireCooldownTicks() {
		return 5; // steady, readable cadence
	}

	@Override
	public int actionCooldownTicks() {
		return 50;
	}

	@Override
	public void onFire(ServerPlayerEntity player) {
		GunMechanics.hitscan(player, 4.0f, 24.0, SoundEvents.ENTITY_ARROW_SHOOT);
	}

	@Override
	public void onAction(ServerPlayerEntity player) {
		GunMechanics.blink(player, 6.0);
	}
}
