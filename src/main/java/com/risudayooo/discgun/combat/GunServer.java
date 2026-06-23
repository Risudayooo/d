package com.risudayooo.discgun.combat;

import com.risudayooo.discgun.DiscGunMod;
import com.risudayooo.discgun.disc.DiscAbility;
import com.risudayooo.discgun.item.GunCDPlayerItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-tick server logic for the gun: a passive movement-speed buff while a gun is
 * held (so just running feels fast — 2章 の高速感), and reload completion.
 */
public final class GunServer {
	private static final Identifier SPEED_ID = DiscGunMod.id("gun_speed");
	private static final double SPEED_BONUS = 0.20; // +20% move speed while armed

	private static final Map<UUID, Long> RELOAD_END = new HashMap<>();

	private GunServer() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(GunServer::tick);
	}

	private static void tick(MinecraftServer server) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			boolean holding = player.getMainHandStack().getItem() instanceof GunCDPlayerItem;
			updateSpeed(player, holding);

			Long end = RELOAD_END.get(player.getUuid());
			if (end != null && player.getWorld().getTime() >= end) {
				RELOAD_END.remove(player.getUuid());
				ItemStack gun = player.getMainHandStack();
				if (gun.getItem() instanceof GunCDPlayerItem) {
					GunCDPlayerItem.setAmmo(gun, GunCDPlayerItem.getDisc(gun).ability().magazineSize());
					player.getServerWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
							SoundEvents.ITEM_CROSSBOW_LOADING_END, SoundCategory.PLAYERS, 0.8f, 1.2f);
				}
			}
		}
	}

	private static void updateSpeed(ServerPlayerEntity player, boolean holding) {
		EntityAttributeInstance inst = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
		if (inst == null) {
			return;
		}
		boolean has = inst.getModifier(SPEED_ID) != null;
		if (holding && !has) {
			inst.addTemporaryModifier(new EntityAttributeModifier(
					SPEED_ID, SPEED_BONUS, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		} else if (!holding && has) {
			inst.removeModifier(SPEED_ID);
		}
	}

	public static boolean isReloading(ServerPlayerEntity player) {
		Long end = RELOAD_END.get(player.getUuid());
		return end != null && player.getWorld().getTime() < end;
	}

	public static void startReload(ServerPlayerEntity player, ItemStack gun, DiscAbility ability) {
		if (isReloading(player) || GunCDPlayerItem.getAmmo(gun) >= ability.magazineSize()) {
			return;
		}
		RELOAD_END.put(player.getUuid(), player.getWorld().getTime() + ability.reloadTimeTicks());
		player.getServerWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ITEM_CROSSBOW_LOADING_START, SoundCategory.PLAYERS, 0.8f, 1.0f);
	}
}
