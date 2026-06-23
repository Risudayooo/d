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
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

/**
 * Client entrypoint. Owns the FPS feel: reads input every tick, sends combat
 * intent packets, and handles movement locally (player velocity is
 * client-authoritative). Vanilla attack/use are suppressed by the mixin while a
 * gun is held, so this handler owns left/right click.
 */
public class DiscGunClient implements ClientModInitializer {
	private static final double DOUBLE_JUMP_UP = 0.62;
	private static final double DOUBLE_JUMP_FORWARD = 0.18;
	private static final double SLIDE_IMPULSE = 0.85;
	private static final double AIR_ACCEL = 0.022;
	private static final double AIR_SPEED_CAP = 0.42;
	private static final float RECOIL_RECOVER_PER_TICK = 1.2f;

	// Client-side fire gate (matches the disc cadence to avoid spamming packets).
	private long nextFireTick = 0;
	private long nextActionTick = 0;

	// Edge-detection state.
	private boolean prevBlink;
	private boolean prevParry;
	private boolean prevReload;
	private boolean prevJump;
	private boolean prevSneak;

	private int airJumpsLeft = 1;

	// Visual recoil bookkeeping (accumulated kick we ease back down).
	private float pendingRecoil = 0f;

	// Client mirror of the reload window so the HUD can show "RELOADING".
	private static long reloadUntilTick = 0;

	public static boolean isReloading(long now) {
		return now < reloadUntilTick;
	}

	@Override
	public void onInitializeClient() {
		ModKeybinds.register();
		GunHud.register();
		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
	}

	private void onClientTick(MinecraftClient client) {
		ClientPlayerEntity player = client.player;
		if (player == null) {
			return;
		}

		ItemStack main = player.getMainHandStack();
		boolean holdingGun = main.getItem() instanceof GunCDPlayerItem;
		long now = player.getWorld().getTime();

		handleDoubleJump(client, player, holdingGun);
		handleSlide(client, player, holdingGun);
		handleAirControl(player, holdingGun);

		if (!holdingGun) {
			prevBlink = prevParry = prevReload = false;
			recoverRecoil(player, false);
			return;
		}

		DiscType disc = GunCDPlayerItem.getDisc(main);
		int ammo = GunCDPlayerItem.getAmmo(main);
		boolean reloading = isReloading(now);

		// Fire — held, gated to the disc cadence, blocked while empty/reloading.
		boolean firingThisTick = false;
		if (client.options.attackKey.isPressed() && !reloading && ammo > 0 && now >= nextFireTick) {
			nextFireTick = now + disc.ability().fireCooldownTicks();
			ClientPlayNetworking.send(new Payloads.Fire());
			applyRecoil(player, disc.ability().recoilDegrees());
			firingThisTick = true;
			// Mirror auto-reload so the HUD reacts the instant the mag empties.
			if (ammo - 1 <= 0) {
				reloadUntilTick = now + disc.ability().reloadTimeTicks();
			}
		}
		recoverRecoil(player, firingThisTick);

		// Right-click disc action.
		if (client.options.useKey.isPressed() && now >= nextActionTick) {
			nextActionTick = now + 5;
			ClientPlayNetworking.send(new Payloads.Action());
		}

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
		if (reload && !prevReload && ammo < disc.ability().magazineSize()) {
			ClientPlayNetworking.send(new Payloads.Reload());
			reloadUntilTick = now + disc.ability().reloadTimeTicks();
		}
		prevReload = reload;
	}

	private void applyRecoil(ClientPlayerEntity player, float degrees) {
		player.setPitch(player.getPitch() - degrees);
		player.setYaw(player.getYaw() + (player.getRandom().nextFloat() - 0.5f) * degrees * 0.5f);
		pendingRecoil += degrees;
	}

	/** Ease the view back down after the burst, only reclaiming what recoil added. */
	private void recoverRecoil(ClientPlayerEntity player, boolean firingThisTick) {
		if (firingThisTick || pendingRecoil <= 0f) {
			return;
		}
		float recover = Math.min(pendingRecoil, RECOIL_RECOVER_PER_TICK);
		player.setPitch(player.getPitch() + recover);
		pendingRecoil -= recover;
	}

	private void handleDoubleJump(MinecraftClient client, ClientPlayerEntity player, boolean holdingGun) {
		if (player.isOnGround()) {
			airJumpsLeft = 1;
		}
		boolean jump = client.options.jumpKey.isPressed();
		if (holdingGun && jump && !prevJump && !player.isOnGround() && airJumpsLeft > 0) {
			Vec3d look = player.getRotationVec(1.0f);
			Vec3d horiz = new Vec3d(look.x, 0.0, look.z);
			horiz = horiz.lengthSquared() > 1.0e-4 ? horiz.normalize() : Vec3d.ZERO;
			Vec3d v = player.getVelocity();
			player.setVelocity(v.x + horiz.x * DOUBLE_JUMP_FORWARD, DOUBLE_JUMP_UP, v.z + horiz.z * DOUBLE_JUMP_FORWARD);
			player.velocityModified = true;
			airJumpsLeft--;
			player.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 0.4f, 1.7f);
		}
		prevJump = jump;
	}

	private void handleSlide(MinecraftClient client, ClientPlayerEntity player, boolean holdingGun) {
		boolean sneak = client.options.sneakKey.isPressed();
		// Phase 1 lite: a strong forward burst on the sneak press while grounded +
		// moving (no hitbox-height change yet — that needs a mixin, Phase 2).
		if (holdingGun && sneak && !prevSneak && player.isOnGround()) {
			Vec3d v = player.getVelocity();
			if (Math.hypot(v.x, v.z) > 0.05) {
				Vec3d look = player.getRotationVec(1.0f);
				Vec3d horiz = new Vec3d(look.x, 0.0, look.z);
				if (horiz.lengthSquared() > 1.0e-4) {
					horiz = horiz.normalize().multiply(SLIDE_IMPULSE);
					player.addVelocity(horiz.x, 0.0, horiz.z);
					player.velocityModified = true;
					player.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.1f);
				}
			}
		}
		prevSneak = sneak;
	}

	/** Subtle air strafe so jumps + blinks keep their flow. */
	private void handleAirControl(ClientPlayerEntity player, boolean holdingGun) {
		if (!holdingGun || player.isOnGround() || player.input == null) {
			return;
		}
		float fwd = player.input.movementForward;
		float side = player.input.movementSideways;
		if (fwd == 0f && side == 0f) {
			return;
		}
		float yawRad = (float) Math.toRadians(player.getYaw());
		Vec3d forward = new Vec3d(-Math.sin(yawRad), 0.0, Math.cos(yawRad));
		Vec3d strafe = new Vec3d(Math.cos(yawRad), 0.0, Math.sin(yawRad));
		Vec3d wish = forward.multiply(fwd).add(strafe.multiply(side));
		if (wish.lengthSquared() < 1.0e-4) {
			return;
		}
		wish = wish.normalize().multiply(AIR_ACCEL);
		Vec3d v = player.getVelocity();
		double nx = v.x + wish.x;
		double nz = v.z + wish.z;
		double h = Math.hypot(nx, nz);
		if (h > AIR_SPEED_CAP) {
			nx = nx / h * AIR_SPEED_CAP;
			nz = nz / h * AIR_SPEED_CAP;
		}
		player.setVelocity(nx, v.y, nz);
		player.velocityModified = true;
	}
}
