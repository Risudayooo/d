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
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;

import java.util.List;

/**
 * Shared combat building blocks: a hitscan shot with spread + a visible tracer,
 * a momentum-preserving blink, a shield and a radial burst. Centralising these
 * keeps each {@link com.risudayooo.discgun.disc.DiscAbility} to pure numbers.
 */
public final class GunMechanics {
	private GunMechanics() {
	}

	/** Fire one hitscan round with spread and a tracer trail. */
	public static void fireShot(ServerPlayerEntity player, float damage, double range,
								float spreadDegrees, SoundEvent fireSound) {
		ServerWorld world = player.getServerWorld();
		Random rng = player.getRandom();

		// Apply random spread around the aim direction.
		float yaw = player.getYaw() + (rng.nextFloat() - 0.5f) * 2.0f * spreadDegrees;
		float pitch = player.getPitch() + (rng.nextFloat() - 0.5f) * 2.0f * spreadDegrees;
		Vec3d dir = Vec3d.fromPolar(pitch, yaw);

		Vec3d start = player.getEyePos();
		Vec3d end = start.add(dir.multiply(range));

		// Clip the ray at the first solid block.
		BlockHitResult blockHit = world.raycast(new RaycastContext(start, end,
				RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
		if (blockHit.getType() == HitResult.Type.BLOCK) {
			end = blockHit.getPos();
		}

		world.playSound(null, player.getX(), player.getY(), player.getZ(),
				fireSound, SoundCategory.PLAYERS, 0.8f, 0.95f + rng.nextFloat() * 0.1f);

		Box searchBox = player.getBoundingBox().stretch(dir.multiply(range)).expand(1.0);
		EntityHitResult entityHit = ProjectileUtil.raycast(player, start, end, searchBox,
				e -> e instanceof LivingEntity && e.isAlive() && e != player, range * range);

		Vec3d impact = end;
		if (entityHit != null && entityHit.getEntity() instanceof LivingEntity target) {
			impact = entityHit.getPos();
			target.damage(player.getDamageSources().playerAttack(player), damage);
			world.spawnParticles(ParticleTypes.CRIT, impact.x, impact.y, impact.z, 8, 0.1, 0.1, 0.1, 0.06);
			world.playSound(null, impact.x, impact.y, impact.z,
					SoundEvents.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 0.6f, 1.4f);
		} else {
			world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, impact.x, impact.y, impact.z, 3, 0.05, 0.05, 0.05, 0.02);
		}

		drawTracer(world, player, dir, impact);
	}

	/** A sparse particle line from the muzzle to the impact point. */
	private static void drawTracer(ServerWorld world, ServerPlayerEntity player, Vec3d dir, Vec3d impact) {
		Vec3d muzzle = player.getEyePos().add(dir.multiply(0.7)).add(0.0, -0.12, 0.0);
		Vec3d delta = impact.subtract(muzzle);
		double dist = delta.length();
		int steps = Math.max(1, (int) (dist / 1.2));
		for (int i = 0; i <= steps; i++) {
			double t = (double) i / steps;
			Vec3d p = muzzle.add(delta.multiply(t));
			world.spawnParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
		// Muzzle flash.
		world.spawnParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 2, 0.02, 0.02, 0.02, 0.0);
	}

	/** Short-range teleport that keeps your momentum (2章 ブリンク). */
	public static void blink(ServerPlayerEntity player, double distance) {
		Vec3d look = player.getRotationVec(1.0f);
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
		// Preserve forward momentum so blink chains into running.
		player.setVelocity(horiz.x * 0.6, player.getVelocity().y, horiz.z * 0.6);
		player.velocityModified = true;

		ServerWorld world = player.getServerWorld();
		world.spawnParticles(ParticleTypes.PORTAL, feet.x, feet.y + 1.0, feet.z, 24, 0.3, 0.5, 0.3, 0.1);
		world.playSound(null, dest.x, dest.y, dest.z,
				SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.6f, 1.5f);
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
