package com.risudayooo.discgun.disc.ability;

import com.risudayooo.discgun.combat.GunMechanics;
import com.risudayooo.discgun.disc.DiscAbility;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

/**
 * Dubstep — heavy, slow, punchy (4章). Small magazine, big damage, strong recoil
 * so each shot lands like a "drop". Right click raises a temporary shield.
 */
public class DubstepAbility implements DiscAbility {
	@Override public int fireCooldownTicks() { return 18; }
	@Override public int magazineSize() { return 6; }
	@Override public int reloadTimeTicks() { return 46; }
	@Override public float damage() { return 9.0f; }
	@Override public double range() { return 28.0; }
	@Override public float spreadDegrees() { return 0.6f; }
	@Override public float recoilDegrees() { return 3.2f; }
	@Override public SoundEvent fireSound() { return SoundEvents.ENTITY_BLAZE_SHOOT; }
	@Override public int actionCooldownTicks() { return 140; }

	@Override
	public void onAction(ServerPlayerEntity player) {
		GunMechanics.shield(player, 3);
	}
}
