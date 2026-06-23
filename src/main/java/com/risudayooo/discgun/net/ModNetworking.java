package com.risudayooo.discgun.net;

import com.risudayooo.discgun.combat.Cooldowns;
import com.risudayooo.discgun.combat.GunMechanics;
import com.risudayooo.discgun.combat.GunServer;
import com.risudayooo.discgun.combat.ParryState;
import com.risudayooo.discgun.disc.DiscAbility;
import com.risudayooo.discgun.disc.DiscType;
import com.risudayooo.discgun.item.GunCDPlayerItem;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

/**
 * Registers the C2S payload types and their server-side handlers. The server is
 * the single source of truth: it re-derives the disc, checks ammo + cooldowns,
 * and runs the ability.
 */
public final class ModNetworking {
	/** Blink cooldown, ticks — short so it chains into movement. */
	private static final int BLINK_COOLDOWN = 18;
	private static final double BLINK_DISTANCE = 7.0;
	private static final int PARRY_COOLDOWN = 20;

	private ModNetworking() {
	}

	public static void registerPayloads() {
		PayloadTypeRegistry.playC2S().register(Payloads.Fire.ID, Payloads.Fire.CODEC);
		PayloadTypeRegistry.playC2S().register(Payloads.Action.ID, Payloads.Action.CODEC);
		PayloadTypeRegistry.playC2S().register(Payloads.Blink.ID, Payloads.Blink.CODEC);
		PayloadTypeRegistry.playC2S().register(Payloads.Parry.ID, Payloads.Parry.CODEC);
		PayloadTypeRegistry.playC2S().register(Payloads.Reload.ID, Payloads.Reload.CODEC);
	}

	public static void registerReceivers() {
		ServerPlayNetworking.registerGlobalReceiver(Payloads.Fire.ID,
				(payload, context) -> runOnMain(context.player(), ModNetworking::handleFire));
		ServerPlayNetworking.registerGlobalReceiver(Payloads.Action.ID,
				(payload, context) -> runOnMain(context.player(), ModNetworking::handleAction));
		ServerPlayNetworking.registerGlobalReceiver(Payloads.Blink.ID,
				(payload, context) -> runOnMain(context.player(), ModNetworking::handleBlink));
		ServerPlayNetworking.registerGlobalReceiver(Payloads.Parry.ID,
				(payload, context) -> runOnMain(context.player(), ModNetworking::handleParry));
		ServerPlayNetworking.registerGlobalReceiver(Payloads.Reload.ID,
				(payload, context) -> runOnMain(context.player(), ModNetworking::handleReload));
	}

	private static void runOnMain(ServerPlayerEntity player, java.util.function.Consumer<ServerPlayerEntity> action) {
		player.getServer().execute(() -> action.accept(player));
	}

	private static ItemStack heldGun(ServerPlayerEntity player) {
		ItemStack stack = player.getMainHandStack();
		return stack.getItem() instanceof GunCDPlayerItem ? stack : ItemStack.EMPTY;
	}

	private static void handleFire(ServerPlayerEntity player) {
		ItemStack gun = heldGun(player);
		if (gun.isEmpty() || GunServer.isReloading(player)) {
			return;
		}
		DiscAbility ability = GunCDPlayerItem.getDisc(gun).ability();
		if (!Cooldowns.ready(player, "fire")) {
			return;
		}
		int ammo = GunCDPlayerItem.getAmmo(gun);
		if (ammo <= 0) {
			player.getServerWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.PLAYERS, 0.6f, 1.6f);
			GunServer.startReload(player, gun, ability);
			return;
		}
		Cooldowns.set(player, "fire", ability.fireCooldownTicks());
		GunCDPlayerItem.setAmmo(gun, ammo - 1);
		GunMechanics.fireShot(player, ability.damage(), ability.range(), ability.spreadDegrees(), ability.fireSound());
		if (ammo - 1 <= 0) {
			GunServer.startReload(player, gun, ability);
		}
	}

	private static void handleAction(ServerPlayerEntity player) {
		ItemStack gun = heldGun(player);
		if (gun.isEmpty()) {
			return;
		}
		DiscType disc = GunCDPlayerItem.getDisc(gun);
		if (Cooldowns.tryUse(player, "action", disc.ability().actionCooldownTicks())) {
			disc.ability().onAction(player);
		}
	}

	private static void handleBlink(ServerPlayerEntity player) {
		if (heldGun(player).isEmpty()) {
			return;
		}
		if (Cooldowns.tryUse(player, "blink", BLINK_COOLDOWN)) {
			GunMechanics.blink(player, BLINK_DISTANCE);
		}
	}

	private static void handleParry(ServerPlayerEntity player) {
		if (heldGun(player).isEmpty()) {
			return;
		}
		if (Cooldowns.tryUse(player, "parry", PARRY_COOLDOWN)) {
			ParryState.begin(player);
		}
	}

	private static void handleReload(ServerPlayerEntity player) {
		ItemStack gun = heldGun(player);
		if (gun.isEmpty()) {
			return;
		}
		GunServer.startReload(player, gun, GunCDPlayerItem.getDisc(gun).ability());
	}
}
