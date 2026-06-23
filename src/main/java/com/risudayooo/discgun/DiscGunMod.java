package com.risudayooo.discgun;

import com.risudayooo.discgun.combat.GunServer;
import com.risudayooo.discgun.combat.ParryState;
import com.risudayooo.discgun.net.ModNetworking;
import com.risudayooo.discgun.registry.ModComponents;
import com.risudayooo.discgun.registry.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entrypoint. See docs/design.md for the full design.
 *
 * <p>Working title mod id: {@code discgun}. Phase 1 (MVP) scope only.
 */
public class DiscGunMod implements ModInitializer {
	public static final String MOD_ID = "discgun";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		// Order matters: components are referenced by item logic at runtime.
		ModComponents.initialize();
		ModItems.initialize();
		ModNetworking.registerPayloads();
		ModNetworking.registerReceivers();
		GunServer.register();

		// Parry (Phase 1: simple cancel). While the parry window is active, the
		// player ignores all incoming damage. Phase 2 will branch on attack type.
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayerEntity player && ParryState.isActive(player)) {
				ParryState.consume(player);
				return false; // cancel the damage
			}
			return true;
		});

		LOGGER.info("[{}] initialized (Phase 1 MVP scaffold)", MOD_ID);
	}
}
