package com.risudayooo.discgun.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.List;

/**
 * Shared building blocks used by the disc abilities: hitscan fire, blink, shield
 * and a radial burst. Reusing these keeps each {@link com.risudayooo.discgun.disc.DiscAbility}
 * tiny so adding discs stays cheap (the "1つ確立すれば横展開しやすい" principle).
 */
public final class GunMechanics {
	private GunMechanics() {
	}

	/** Instant hitscan from the player's eyes. Returns true if a living entity was hit. */
	public static boolean hitscan(ServerPlayerEntity player, float damage, double range, SoundEvent fireSound) {
		ServerWorld world = player.getServerWorld();
		Vec3d start = player.getEyePos();
		Vec3d dir = player.getRotationVec(1.0f);
		Vec3d end = start.add(dir.multiply(range));

		// Clip the ray at the first solid block so we cannot shoot through walls.
		BlockHitResult blockHit = world.raycast(new RaycastContext(start, end,
				RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
		if (blockHit.getType() == HitResult.Type.BLOCK) {
			end = blockHit.getPos();
		}

		world.playSound(null, player.getX(), player.getY(), player.getZ(),
				fireSound, SoundCategory.PLAYERS, 0.8f, 1.0f);

		Box searchBox = player.getBoundingBox().stretch(dir.multiply(range)).expand(1.0);
		double maxDistSq = start.squaredDistanceTo(end);
		EntityHitResult entityHit = ProjectileUtil.raycast(player, start, end, searchBox,
				e -> e instanceof LivingEntity && e.isAlive() && e != player, maxDistSq);

		if (entityHit != null && entityHit.getEntity() instanceof LivingEntity target) {
			target.damage(player.getDamageSources().playerAttack(player), damage);
			Vec3d hitPos = entityHit.getPos();
			world.spawnParticles(ParticleTypes.CRIT, hitPos.x, hitPos.y, hitPos.z, 6, 0.1, 0.1, 0.1, 0.05);
			world.playSound(null, hitPos.x, hitPos.y, hitPos.z,
					SoundEvents.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 0.5f, 1.4f);
			return true;
		}
		return false;
	}

	/** Short-range teleport that stops short of the first wall (2章 ブリンク). */
	public static void blink(ServerPlayerEntity player, double distance) {
		Vec3d look = player.getRotationVec(1.0f);
		// Dampen the vertical component so looking up doesn't fling the player.
		Vec3d horiz = new Vec3d(look.x, 0.0, look.z);
		if (horiz.lengthSquared() < 1.0e-4) {
			return;
		}
		horiz = horiz.normalize();

		Vec3d start = player.getEyePos();
		Vec3d end = start.add(horiz.multiply(distance));
		BlockHitResult hit = player.getServerWorld().raycast(new RaycastContext(start, end,
				RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));

		double travel = distance;
		if (hit.getType() == HitResult.Type.BLOCK) {
			travel = Math.max(0.0, start.distanceTo(hit.getPos()) - 0.6);
		}

		Vec3d feet = player.getPos();
		Vec3d dest = feet.add(horiz.multiply(travel));
		player.requestTeleport(dest.x, dest.y, dest.z);
		player.fallDistance = 0.0f;

		ServerWorld world = player.getServerWorld();
		world.spawnParticles(ParticleTypes.PORTAL, feet.x, feet.y + 1.0, feet.z, 20, 0.3, 0.5, 0.3, 0.1);
		world.playSound(null, dest.x, dest.y, dest.z,
				SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.6f, 1.4f);
	}

	/** Temporary defensive layer: absorption + resistance for a few seconds. */
	public static void shield(ServerPlayerEntity player, int seconds) {
		int ticks = seconds * 20;
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, ticks, 1, false, true));
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, ticks, 1, false, true));
		player.getServerWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 0.5f, 1.8f);
	}

	/** Radial burst around the player: damage + knockback to nearby enemies. */
	public static void radialBurst(ServerPlayerEntity player, double radius, float damage) {
		ServerWorld world = player.getServerWorld();
		Box area = Box.of(player.getPos(), radius * 2, radius * 2, radius * 2);
		List<Entity> targets = world.getOtherEntities(player, area,
				e -> e instanceof LivingEntity && e.isAlive());

		for (Entity entity : targets) {
			LivingEntity target = (LivingEntity) entity;
			target.damage(player.getDamageSources().playerAttack(player), damage);
			Vec3d push = target.getPos().subtract(player.getPos());
			target.takeKnockback(0.6, -push.x, -push.z);
		}

		world.spawnParticles(ParticleTypes.SONIC_BOOM, player.getX(), player.getY() + 1.0, player.getZ(), 1, 0, 0, 0, 0);
		world.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.6f, 1.6f);
	}
}
