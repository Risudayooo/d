package com.risudayooo.discgun.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * Momentum-based parkour movement (Parcool! / Redliner flavour). A small
 * client-side state machine layered on top of vanilla movement: slide,
 * wall-run, wall-jump, auto-vault, double jump and air strafe — all by nudging
 * the player's (client-authoritative) velocity.
 *
 * <p>Only active while a gun is held, matching the override design. Tuning knobs
 * are the constants up top.
 */
public final class ParkourController {
	// --- tuning ---------------------------------------------------------------
	private static final double AIR_ACCEL = 0.025;
	private static final double AIR_SPEED_CAP = 0.56;          // lets momentum build
	private static final double DOUBLE_JUMP_UP = 0.62;
	private static final double DOUBLE_JUMP_FORWARD = 0.20;

	private static final double SLIDE_TRIGGER_SPEED = 0.16;
	private static final double SLIDE_START_SPEED = 0.46;
	private static final double SLIDE_FRICTION = 0.985;        // closer to 1 = keeps speed
	private static final double SLIDE_MIN_SPEED = 0.13;
	private static final int SLIDE_MAX_TICKS = 36;
	private static final double SLIDE_HOP_UP = 0.42;

	private static final double WALL_CHECK_DIST = 0.85;
	private static final double WALLRUN_ENTER_SPEED = 0.12;
	private static final int WALLRUN_MAX_TICKS = 55;
	private static final double WALLRUN_SPEED = 0.34;
	private static final double WALLRUN_UP_START = 0.12;
	private static final double WALLRUN_DOWN_END = -0.06;
	private static final double WALLJUMP_UP = 0.54;
	private static final double WALLJUMP_OUT = 0.42;
	private static final double WALLJUMP_FORWARD = 0.24;

	private static final double VAULT_UP = 0.46;
	private static final double VAULT_FORWARD = 0.18;

	// --- state ----------------------------------------------------------------
	private enum State { NONE, SLIDING, WALLRUN }

	private State state = State.NONE;
	private int stateTimer = 0;
	private int airJumpsLeft = 1;
	private int wallCooldown = 0;
	private int vaultCooldown = 0;
	private Vec3d wallNormal = Vec3d.ZERO;
	private double slideSpeed = 0;

	private boolean prevJump;
	private boolean prevSneak;

	public void tick(MinecraftClient client, ClientPlayerEntity player, boolean holdingGun) {
		if (!holdingGun || player.input == null) {
			state = State.NONE;
			prevJump = client.options.jumpKey.isPressed();
			prevSneak = client.options.sneakKey.isPressed();
			return;
		}

		boolean ground = player.isOnGround();
		boolean jump = client.options.jumpKey.isPressed();
		boolean sneak = client.options.sneakKey.isPressed();
		boolean jumpEdge = jump && !prevJump;
		boolean sneakEdge = sneak && !prevSneak;
		float forwardIn = player.input.movementForward;

		if (wallCooldown > 0) wallCooldown--;
		if (vaultCooldown > 0) vaultCooldown--;
		if (ground) airJumpsLeft = 1;

		switch (state) {
			case SLIDING -> tickSlide(player, sneak, ground, jumpEdge);
			case WALLRUN -> tickWallRun(player, jumpEdge);
			case NONE -> {
				if (ground) {
					if (sneakEdge && (player.isSprinting() || horizSpeed(player) > SLIDE_TRIGGER_SPEED)) {
						enterSlide(player);
					} else {
						tryVault(player, forwardIn);
					}
				} else {
					if (jumpEdge) {
						BlockHitResult wall = nearestWall(player);
						if (wall != null) {
							wallJump(player, wallNormalFrom(player, wall));
						} else if (airJumpsLeft > 0) {
							doubleJump(player);
						}
					} else if (wallCooldown == 0 && forwardIn > 0 && player.getVelocity().y < 0.1
							&& horizSpeed(player) > WALLRUN_ENTER_SPEED) {
						tryEnterWallRun(player);
					}
				}
			}
		}

		if (!ground && state != State.WALLRUN) {
			airStrafe(player);
		}

		prevJump = jump;
		prevSneak = sneak;
	}

	// --- slide ----------------------------------------------------------------
	private void enterSlide(ClientPlayerEntity player) {
		Vec3d dir = horizDir(player);
		slideSpeed = Math.min(0.5, Math.max(horizSpeed(player) * 1.3, SLIDE_START_SPEED));
		setHoriz(player, dir.multiply(slideSpeed));
		state = State.SLIDING;
		stateTimer = SLIDE_MAX_TICKS;
		player.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 0.9f);
		feetParticles(player, ParticleTypes.CLOUD, 6);
	}

	private void tickSlide(ClientPlayerEntity player, boolean sneak, boolean ground, boolean jumpEdge) {
		if (jumpEdge) {           // slide-hop: keep the speed, pop up
			Vec3d dir = horizDir(player);
			player.setVelocity(dir.x * slideSpeed, SLIDE_HOP_UP, dir.z * slideSpeed);
			player.velocityModified = true;
			player.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.3f);
			state = State.NONE;
			return;
		}
		if (!sneak || !ground || stateTimer-- <= 0 || slideSpeed < SLIDE_MIN_SPEED) {
			state = State.NONE;
			return;
		}
		slideSpeed *= SLIDE_FRICTION;
		// allow gentle steering toward where you look
		Vec3d dir = horizDir(player).multiply(0.15).add(currentDir(player).multiply(0.85));
		if (dir.lengthSquared() > 1.0e-4) dir = dir.normalize();
		setHoriz(player, dir.multiply(slideSpeed));
		if (stateTimer % 3 == 0) feetParticles(player, ParticleTypes.CLOUD, 1);
	}

	// --- wall-run -------------------------------------------------------------
	private void tryEnterWallRun(ClientPlayerEntity player) {
		BlockHitResult wall = nearestWall(player);
		if (wall == null) return;
		Vec3d normal = wallNormalFrom(player, wall);
		Vec3d tangent = tangentAlongWall(player, normal);
		if (tangent == null) return;     // looking into/away from the wall
		wallNormal = normal;
		state = State.WALLRUN;
		stateTimer = WALLRUN_MAX_TICKS;
		Vec3d v = player.getVelocity();
		player.setVelocity(v.x, Math.max(v.y, WALLRUN_UP_START), v.z);
		player.velocityModified = true;
		player.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 0.4f, 1.5f);
	}

	private void tickWallRun(ClientPlayerEntity player, boolean jumpEdge) {
		if (jumpEdge) {
			wallJump(player, wallNormal);
			return;
		}
		BlockHitResult wall = nearestWall(player);
		if (wall == null || player.isOnGround() || stateTimer-- <= 0) {
			state = State.NONE;
			wallCooldown = 7;
			return;
		}
		wallNormal = wallNormalFrom(player, wall);
		Vec3d tangent = tangentAlongWall(player, wallNormal);
		if (tangent == null) {
			state = State.NONE;
			wallCooldown = 7;
			return;
		}
		double t = 1.0 - (double) stateTimer / WALLRUN_MAX_TICKS;
		double vy = WALLRUN_UP_START + (WALLRUN_DOWN_END - WALLRUN_UP_START) * t;
		// move along the wall, glued slightly toward it
		Vec3d move = tangent.multiply(WALLRUN_SPEED).subtract(wallNormal.multiply(0.03));
		player.setVelocity(move.x, vy, move.z);
		player.velocityModified = true;
		if (stateTimer % 4 == 0) {
			player.getWorld().addParticle(ParticleTypes.CRIT,
					player.getX(), player.getY() + 1.0, player.getZ(), 0, 0, 0);
			player.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 0.25f, 1.9f);
		}
	}

	private void wallJump(ClientPlayerEntity player, Vec3d normal) {
		Vec3d look = horizDir(player);
		double vx = normal.x * WALLJUMP_OUT + look.x * WALLJUMP_FORWARD;
		double vz = normal.z * WALLJUMP_OUT + look.z * WALLJUMP_FORWARD;
		player.setVelocity(vx, WALLJUMP_UP, vz);
		player.velocityModified = true;
		airJumpsLeft = 1;          // refresh so you can chain off the next wall/air
		state = State.NONE;
		wallCooldown = 5;
		player.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.6f);
		player.getWorld().addParticle(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.5, player.getZ(), 0, 0, 0);
	}

	// --- double jump + vault + air strafe ------------------------------------
	private void doubleJump(ClientPlayerEntity player) {
		Vec3d dir = horizDir(player);
		Vec3d v = player.getVelocity();
		player.setVelocity(v.x + dir.x * DOUBLE_JUMP_FORWARD, DOUBLE_JUMP_UP, v.z + dir.z * DOUBLE_JUMP_FORWARD);
		player.velocityModified = true;
		airJumpsLeft--;
		player.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 0.4f, 1.8f);
		feetParticles(player, ParticleTypes.CLOUD, 4);
	}

	/** Auto-mantle a ~1 block ledge you run into, so flow isn't broken. */
	private void tryVault(ClientPlayerEntity player, float forwardIn) {
		if (vaultCooldown > 0 || forwardIn <= 0 || horizSpeed(player) < 0.12) return;
		Vec3d look = horizDir(player);
		Vec3d ahead = player.getPos().add(look.multiply(0.6));
		// wall in front at body height?
		BlockHitResult front = ray(player, player.getPos().add(0, 0.6, 0), look, 0.9);
		if (front.getType() != HitResult.Type.BLOCK) return;
		// clear space above the ledge to stand on?
		BlockHitResult above = ray(player, ahead.add(0, 1.2, 0), look, 0.9);
		if (above.getType() == HitResult.Type.BLOCK) return;
		Vec3d v = player.getVelocity();
		player.setVelocity(v.x + look.x * VAULT_FORWARD, VAULT_UP, v.z + look.z * VAULT_FORWARD);
		player.velocityModified = true;
		vaultCooldown = 10;
		player.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 0.45f, 1.4f);
	}

	private void airStrafe(ClientPlayerEntity player) {
		float fwd = player.input.movementForward;
		float side = player.input.movementSideways;
		if (fwd == 0f && side == 0f) return;
		float yawRad = (float) Math.toRadians(player.getYaw());
		Vec3d forward = new Vec3d(-Math.sin(yawRad), 0.0, Math.cos(yawRad));
		Vec3d strafe = new Vec3d(Math.cos(yawRad), 0.0, Math.sin(yawRad));
		Vec3d wish = forward.multiply(fwd).add(strafe.multiply(side));
		if (wish.lengthSquared() < 1.0e-4) return;
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

	// --- helpers --------------------------------------------------------------
	private static double horizSpeed(ClientPlayerEntity p) {
		Vec3d v = p.getVelocity();
		return Math.hypot(v.x, v.z);
	}

	/** Horizontal facing direction (unit). */
	private static Vec3d horizDir(ClientPlayerEntity p) {
		Vec3d look = p.getRotationVec(1.0f);
		Vec3d h = new Vec3d(look.x, 0.0, look.z);
		return h.lengthSquared() > 1.0e-4 ? h.normalize() : new Vec3d(0, 0, 1);
	}

	/** Current horizontal travel direction (unit), falling back to facing. */
	private static Vec3d currentDir(ClientPlayerEntity p) {
		Vec3d v = p.getVelocity();
		Vec3d h = new Vec3d(v.x, 0.0, v.z);
		return h.lengthSquared() > 1.0e-4 ? h.normalize() : horizDir(p);
	}

	private static void setHoriz(ClientPlayerEntity p, Vec3d horiz) {
		p.setVelocity(horiz.x, p.getVelocity().y, horiz.z);
		p.velocityModified = true;
	}

	private static void feetParticles(ClientPlayerEntity p, net.minecraft.particle.ParticleEffect fx, int n) {
		for (int i = 0; i < n; i++) {
			p.getWorld().addParticle(fx, p.getX(), p.getY() + 0.1, p.getZ(),
					(p.getRandom().nextDouble() - 0.5) * 0.2, 0.02, (p.getRandom().nextDouble() - 0.5) * 0.2);
		}
	}

	private static BlockHitResult ray(ClientPlayerEntity p, Vec3d start, Vec3d dir, double dist) {
		Vec3d end = start.add(dir.multiply(dist));
		return p.getWorld().raycast(new RaycastContext(start, end,
				RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
	}

	/** Find a wall beside the player (left or right), or null. */
	private static BlockHitResult nearestWall(ClientPlayerEntity p) {
		Vec3d look = horizDir(p);
		Vec3d perp = new Vec3d(look.z, 0.0, -look.x);
		Vec3d chest = p.getPos().add(0, 0.9, 0);
		BlockHitResult right = ray(p, chest, perp, WALL_CHECK_DIST);
		if (right.getType() == HitResult.Type.BLOCK) return right;
		BlockHitResult left = ray(p, chest, perp.multiply(-1), WALL_CHECK_DIST);
		if (left.getType() == HitResult.Type.BLOCK) return left;
		return null;
	}

	/** Unit horizontal normal pointing from the wall toward the player. */
	private static Vec3d wallNormalFrom(ClientPlayerEntity p, BlockHitResult wall) {
		Vec3d n = p.getPos().add(0, 0.9, 0).subtract(wall.getPos());
		n = new Vec3d(n.x, 0.0, n.z);
		return n.lengthSquared() > 1.0e-4 ? n.normalize() : horizDir(p).multiply(-1);
	}

	/** Direction along the wall in the player's forward sense, or null if facing the wall. */
	private static Vec3d tangentAlongWall(ClientPlayerEntity p, Vec3d normal) {
		Vec3d look = horizDir(p);
		Vec3d t = look.subtract(normal.multiply(look.dotProduct(normal)));
		if (t.lengthSquared() < 0.04) return null;   // not enough "along wall" component
		return t.normalize();
	}
}
