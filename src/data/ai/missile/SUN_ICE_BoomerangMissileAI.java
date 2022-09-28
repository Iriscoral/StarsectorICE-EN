package data.ai.missile;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

public class SUN_ICE_BoomerangMissileAI extends SUN_ICE_BaseMissileAI {
	private static final Color PARTICLE_COLOR = new Color(175, 240, 165, 255);
	private static final Color EXPLOSION_COLOR = new Color(125, 95, 100, 155);
	private static final float AVOID_RANGE = 400f;

	public SUN_ICE_BoomerangMissileAI(MissileAPI missile) {
		this.missile = missile;
	}

	@Override
	protected CombatEntityAPI findTarget() {
		ShipAPI closest = null;
		float distance, closestDistance = Float.MAX_VALUE;

		for (ShipAPI tmp : AIUtils.getEnemiesOnMap(missile)) {
			distance = MathUtils.getDistance(tmp, missile);
			if (tmp.isDrone() || tmp.isFighter()) {
				distance += 500f;
			}
			if (tmp.isPhased()) {
				distance += 1000f;
			}
			if (tmp == missile.getSource().getShipTarget()) {
				distance -= 500f;
			}
			if (distance < closestDistance) {
				closest = tmp;
				closestDistance = distance;
			}
		}

		target = closest;

		return target;
	}

	@Override
	public void advance(float amount) {
		super.advance(amount);
		accelerate();

		if (target != null) {
			if (MathUtils.getDistance(target, missile) <= AVOID_RANGE) {
				turnAway(target);
			} else {
				float angle = VectorUtils.getAngle(missile.getLocation(), target.getLocation());
				float dif = Math.abs(MathUtils.getShortestRotation(missile.getFacing(), angle));
				if (dif > 120f) {
					turnToward(target);
				}
			}
		}

		if (missile.isFizzling()) {
			if (target == null) {
				return;
			}

			Global.getCombatEngine().spawnProjectile(missile.getSource(), missile.getWeapon(), "sun_ice_boomeranghack", missile.getLocation(), VectorUtils.getAngle(missile.getLocation(), target.getLocation()), (Vector2f) missile.getVelocity().scale(0.25f));

			DamagingExplosionSpec spec = new DamagingExplosionSpec(0.1f, 50f, 20f, missile.getDamageAmount() * 0.5f, missile.getDamageAmount() * 0.25f, CollisionClass.PROJECTILE_FF, CollisionClass.PROJECTILE_FIGHTER, 3f, 3f, 2f, 5, PARTICLE_COLOR, EXPLOSION_COLOR);
			spec.setDamageType(missile.getDamageType());
			spec.setSoundSetId("explosion_missile");
			spec.setUseDetailedExplosion(false);

			Global.getCombatEngine().removeEntity(missile);
		}
	}
}