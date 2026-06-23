package com.risudayooo.discgun.disc.ability;

import com.risudayooo.discgun.combat.GunMechanics;
import com.risudayooo.discgun.disc.DiscAbility;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

/**
 * House — the neutral baseline (4章). Steady, readable auto fire with a roomy
 * magazine, tight spread and light recoil. Right click is a short blink.
 */
public class HouseAbility implements DiscAbility {
	@Override public int fireCooldownTicks() { return 5; }
	@Override public int magazineSize() { return 24; }
	@Override public int reloadTimeTicks() { return 28; }
	@Override public float damage() { return 4.0f; }
	@Override public double range() { return 28.0; }
	@Override public float spreadDegrees() { return 1.2f; }
	@Override public float recoilDegrees() { return 1.0f; }
	@Override public SoundEvent fireSound() { return SoundEvents.ENTITY_ARROW_SHOOT; }
	@Override public int actionCooldownTicks() { return 18; }

	@Override
	public void onAction(ServerPlayerEntity player) {
		GunMechanics.blink(player, 7.0);
	}
}
