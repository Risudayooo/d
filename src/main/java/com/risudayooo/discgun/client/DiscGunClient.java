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

/**
 * Client entrypoint. Owns the FPS feel: reads input every tick, sends combat
 * intent packets, applies visual recoil, and delegates movement tech to
 * {@link ParkourController}. Vanilla attack/use are suppressed by the mixin while
 * a gun is held, so this handler owns left/right click.
 */
public class DiscGunClient implements ClientModInitializer {
	private static final float RECOIL_RECOVER_PER_TICK = 1.2f;

	private final ParkourController parkour = new ParkourController();

	// Client-side fire gate (matches the disc cadence to avoid spamming packets).
	private long nextFireTick = 0;
	private long nextActionTick = 0;

	// Edge-detection state for combat keys.
	private boolean prevBlink;
	private boolean prevParry;
	private boolean prevReload;

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

		// Movement tech (slide / wall-run / wall-jump / vault / air strafe).
		parkour.tick(client, player, holdingGun);

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
}
