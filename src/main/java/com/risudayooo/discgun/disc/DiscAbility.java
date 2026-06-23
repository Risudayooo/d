package com.risudayooo.discgun.disc;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;

/**
 * The single "ability interface" (4章). A disc fully describes its weapon feel
 * through tunable parameters (fire cadence, magazine, damage, spread, recoil...)
 * plus its signature right-click action. Firing itself is centralised in
 * {@link com.risudayooo.discgun.combat.GunMechanics}, so a new disc is just new
 * numbers + an action.
 */
public interface DiscAbility {
	/** Minimum ticks between consecutive fires (left click). */
	int fireCooldownTicks();

	/** Rounds per magazine. */
	int magazineSize();

	/** Reload duration in ticks. */
	int reloadTimeTicks();

	/** Damage per hit. */
	float damage();

	/** Effective hitscan range in blocks. */
	double range();

	/** Random spread half-angle in degrees (0 = laser accurate). */
	float spreadDegrees();

	/** Upward view kick per shot in degrees (visual recoil). */
	float recoilDegrees();

	/** Muzzle sound. */
	SoundEvent fireSound();

	/** Cooldown for the right-click action, in ticks. */
	int actionCooldownTicks();

	/** Right click: the disc's signature action. */
	void onAction(ServerPlayerEntity player);
}
