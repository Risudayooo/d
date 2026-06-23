package com.risudayooo.discgun.disc.ability;

import com.risudayooo.discgun.combat.GunMechanics;
import com.risudayooo.discgun.disc.DiscAbility;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;

/**
 * Dubstep — heavy "ため→ドロップ" hits (4章). Phase 1 models the drop as a single
 * high-damage hitscan on a long cadence; the explicit charge-up windup (and a
 * radial drop) is a Phase 2 refinement. Right click raises a temporary shield.
 */
public class DubstepAbility implements DiscAbility {
	@Override
	public int fireCooldownTicks() {
		return 18; // slow, punchy
	}

	@Override
	public int actionCooldownTicks() {
		return 140;
	}

	@Override
	public void onFire(ServerPlayerEntity player) {
		GunMechanics.hitscan(player, 9.0f, 24.0, SoundEvents.ENTITY_BLAZE_SHOOT);
	}

	@Override
	public void onAction(ServerPlayerEntity player) {
		GunMechanics.shield(player, 3);
	}
}
