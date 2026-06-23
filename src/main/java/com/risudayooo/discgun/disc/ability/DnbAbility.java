package com.risudayooo.discgun.disc.ability;

import com.risudayooo.discgun.combat.GunMechanics;
import com.risudayooo.discgun.disc.DiscAbility;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

/**
 * DnB — fast, low-damage chatter (4章). Tiny cadence + huge magazine make it a
 * stream of bullets; wider spread, very light per-shot recoil that stacks while
 * held. Right click is a snappy radial burst.
 */
public class DnbAbility implements DiscAbility {
	@Override public int fireCooldownTicks() { return 2; }
	@Override public int magazineSize() { return 40; }
	@Override public int reloadTimeTicks() { return 34; }
	@Override public float damage() { return 1.6f; }
	@Override public double range() { return 22.0; }
	@Override public float spreadDegrees() { return 2.6f; }
	@Override public float recoilDegrees() { return 0.7f; }
	@Override public SoundEvent fireSound() { return SoundEvents.ENTITY_ARROW_SHOOT; }
	@Override public int actionCooldownTicks() { return 90; }

	@Override
	public void onAction(ServerPlayerEntity player) {
		GunMechanics.radialBurst(player, 4.0, 3.0f);
	}
}
