package com.risudayooo.discgun.disc.ability;

import com.risudayooo.discgun.combat.GunMechanics;
import com.risudayooo.discgun.disc.DiscAbility;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;

/**
 * DnB — fast, low-damage chatter (4章). Very short cadence to evoke the chopped
 * breakbeat; right click is a snappy radial burst ("周囲をかき乱す").
 */
public class DnbAbility implements DiscAbility {
	@Override
	public int fireCooldownTicks() {
		return 2; // rapid fire
	}

	@Override
	public int actionCooldownTicks() {
		return 90;
	}

	@Override
	public void onFire(ServerPlayerEntity player) {
		GunMechanics.hitscan(player, 1.5f, 20.0, SoundEvents.ENTITY_ARROW_SHOOT);
	}

	@Override
	public void onAction(ServerPlayerEntity player) {
		GunMechanics.radialBurst(player, 4.0, 3.0f);
	}
}
