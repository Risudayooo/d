package com.risudayooo.discgun.client;

import com.risudayooo.discgun.disc.DiscType;
import com.risudayooo.discgun.item.GunCDPlayerItem;
import com.risudayooo.discgun.net.Payloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

/**
 * Client entrypoint. Drives the FPS feel: it reads input every client tick and
 * sends intent packets for combat actions, while handling double-jump and slide
 * locally (player velocity is client-authoritative, so no packet needed).
 *
 * <p>Vanilla attack/use are suppressed by {@code MinecraftClientMixin} while the
 * gun is held, so this handler owns left/right click behaviour.
 */
public class DiscGunClient implements ClientModInitializer {
	/** Upward velocity granted by the air jump. */
	private static final double DOUBLE_JUMP_VELOCITY = 0.5;
	/** Forward impulse of the slide burst. */
	private static final double SLIDE_IMPULSE = 0.55;

	// Client-side gate so we don't spam fire packets faster than the disc cadence.
	private long nextFireTick = 0;
	private long nextActionTick = 0;

	// Edge-detection state for tap actions.
	private boolean prevBlink;
	private boolean prevParry;
	private boolean prevReload;
	private boolean prevJump;
	private boolean prevSneak;

	// Double jump bookkeeping.
	private int airJumpsLeft = 1;

	@Override
	public void onInitializeClient() {
		ModKeybinds.register();
		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
	}

	private void onClientTick(MinecraftClient client) {
		ClientPlayerEntity player = client.player;
		if (player == null) {
			return;
		}

		boolean holdingGun = player.getMainHandStack().getItem() instanceof GunCDPlayerItem;

		handleDoubleJump(client, player, holdingGun);
		handleSlide(client, player, holdingGun);

		if (!holdingGun) {
			// Reset edges so the first press after re-equipping isn't swallowed.
			prevBlink = prevParry = prevReload = false;
			return;
		}

		long now = player.getWorld().getTime();

		// Fire — held, rate-limited to the loaded disc's cadence.
		if (client.options.attackKey.isPressed() && now >= nextFireTick) {
			DiscType disc = GunCDPlayerItem.getDisc(player.getMainHandStack());
			nextFireTick = now + disc.ability().fireCooldownTicks();
			ClientPlayNetworking.send(new Payloads.Fire());
		}

		// Right-click disc action — held, lightly gated client-side.
		if (client.options.useKey.isPressed() && now >= nextActionTick) {
			nextActionTick = now + 5;
			ClientPlayNetworking.send(new Payloads.Action());
		}

		// Tap actions (edge-triggered).
		boolean blink = ModKeybinds.blink.isPressed();
		if (blink && !prevBlink) {
			ClientPlayNetworking.send(new Payloads.Blink());
		}
		prevBlink = blink;

		boolean parry = ModKeybinds.parry.isPressed();
		if (parry && !prevParry) {
			ClientPlayNetworking.send(new Payloads.Parry());
		}
		prevParry = parry;

		boolean reload = ModKeybinds.reload.isPressed();
		if (reload && !prevReload) {
			ClientPlayNetworking.send(new Payloads.Reload());
		}
		prevReload = reload;
	}

	private void handleDoubleJump(MinecraftClient client, ClientPlayerEntity player, boolean holdingGun) {
		if (player.isOnGround()) {
			airJumpsLeft = 1;
		}
		boolean jump = client.options.jumpKey.isPressed();
		if (holdingGun && jump && !prevJump && !player.isOnGround() && airJumpsLeft > 0) {
			Vec3d v = player.getVelocity();
			player.setVelocity(v.x, DOUBLE_JUMP_VELOCITY, v.z);
			player.velocityModified = true;
			airJumpsLeft--;
		}
		prevJump = jump;
	}

	private void handleSlide(MinecraftClient client, ClientPlayerEntity player, boolean holdingGun) {
		boolean sneak = client.options.sneakKey.isPressed();
		// Phase 1 lite: a forward burst on the sneak press while grounded + moving.
		// (No hitbox-height change yet — that needs a mixin, tracked for Phase 2.)
		if (holdingGun && sneak && !prevSneak && player.isOnGround()) {
			Vec3d look = player.getRotationVec(1.0f);
			Vec3d horiz = new Vec3d(look.x, 0.0, look.z);
			if (horiz.lengthSquared() > 1.0e-4) {
				horiz = horiz.normalize().multiply(SLIDE_IMPULSE);
				player.addVelocity(horiz.x, 0.0, horiz.z);
				player.velocityModified = true;
			}
		}
		prevSneak = sneak;
	}
}
