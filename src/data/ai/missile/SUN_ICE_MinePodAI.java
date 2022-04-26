package data.ai.missile;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import data.scripts.tools.SUN_ICE_IceUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SUN_ICE_MinePodAI extends SUN_ICE_BaseMissileAI {
	private static final Color PING_COLOR = new Color(0, 250, 220, 255);
	private static final Color EXPLOSION_COLOR = new Color(100, 125, 105, 155);
	private static final float DEPLOY_DELEY_TIME = 3f;
	private static final float DEPLOY_DISTANCE = 40f;
	private static final float AVOID_DISTANCE = 150f;

	private CombatEntityAPI entityToAvoid = null;
	private float deployDelay = DEPLOY_DELEY_TIME;
	private Vector2f deployPoint;
	private final CombatEngineAPI engine;

	public SUN_ICE_MinePodAI(MissileAPI missile) {
		this.missile = missile;
		engine = Global.getCombatEngine();
		deployPoint = new Vector2f(missile.getSource().getMouseTarget());
		List<BattleObjectiveAPI> objectives = Global.getCombatEngine().getObjectives();

		if (missile.getWeapon() == null) {
			SUN_ICE_IceUtils.destroy(missile);
		} else if (missile.getSource().getShipAI() == null) {
			Global.getCombatEngine().addHitParticle(deployPoint, new Vector2f(), 60f, 1f, 0.7f, PING_COLOR);
			Global.getSoundPlayer().playUISound("sun_ice_deploy_mine_pod", 1f, 1f);
		} else if (!objectives.isEmpty()) {
			float bestRecord = missile.getWeapon().getRange();

			for (BattleObjectiveAPI objective : objectives) {
				float distance = MathUtils.getDistance(objective, missile);

				if (distance < bestRecord) {
					bestRecord = distance;
					deployPoint = objective.getLocation();
				}
			}
		}
	}

	private void deployNewMines() {
		float angle = 360f * (float) Math.random();

		for (int i = 0; i < 24; ++i) {
			angle += 60f + Math.random() * 60f;

			MissileAPI mine = (MissileAPI) engine.spawnProjectile(missile.getSource(), missile.getWeapon(), "sun_ice_mine", missile.getLocation(), angle, null);

			mine.getVelocity().scale((float)Math.random() * 1.2f + 0.4f);
			mine.setFlightTime(mine.getMaxFlightTime() * (float)Math.random() * 0.25f);
		}
	}

	@Override
	public CombatEntityAPI findFlareTarget(float range) {
		return null;
	}

	@Override
	public void evaluateCircumstances() {
		float dist, bestDist = Float.MAX_VALUE;
		CombatEntityAPI winner = null;

		float angle = (float) Math.toRadians(missile.getFacing());
		Vector2f leadPoint = new Vector2f((float) Math.cos(angle) * AVOID_DISTANCE + missile.getLocation().x, (float) Math.sin(angle) * AVOID_DISTANCE + missile.getLocation().y);

		List<CombatEntityAPI> obstacles = new ArrayList<>();
		obstacles.addAll(engine.getShips());
		obstacles.addAll(engine.getAsteroids());

		for (CombatEntityAPI entity : obstacles) {
			// Skip living allies since the pod will pass over them anyway.
			if (entity instanceof ShipAPI && entity.getOwner() == missile.getOwner() && ((ShipAPI) entity).isAlive()) {
				continue;
			}

			dist = MathUtils.getDistanceSquared(entity, leadPoint);

			if (dist < bestDist) {
				bestDist = dist;
				winner = entity;
			}
		}

		entityToAvoid = (Math.sqrt(bestDist) < AVOID_DISTANCE) ? winner : null;
	}

	@Override
	public void advance(float amount) {
		super.advance(amount);

		if (entityToAvoid == null) {
			if (Math.abs(getAngleTo(deployPoint)) < 5f) {
				accelerate();
			} else {
				decelerate();
			}

			turnToward(deployPoint);
			strafeToward(deployPoint);
		} else if (Math.abs(getAngleTo(entityToAvoid)) > 75f) {
			turnToward(deployPoint);
			strafeToward(deployPoint);
			accelerate();
		} else {
			turnAway(entityToAvoid);
			strafeAway(entityToAvoid);
			accelerate();
		}

		if ((deployDelay -= amount) < 0f && (MathUtils.getDistance(missile, deployPoint) <= DEPLOY_DISTANCE || missile.isFizzling() || missile.getHullLevel() < 0.5f)) {
			deployNewMines();

			DamagingExplosionSpec spec = new DamagingExplosionSpec(0.1f, 50f, 20f, missile.getDamageAmount() * 0.5f, missile.getDamageAmount() * 0.25f, CollisionClass.PROJECTILE_FF, CollisionClass.PROJECTILE_FIGHTER, 3f, 3f, 2f, 10, PING_COLOR, EXPLOSION_COLOR);
			spec.setDamageType(missile.getDamageType());
			spec.setSoundSetId("explosion_missile");
			spec.setUseDetailedExplosion(false);

			Global.getCombatEngine().removeEntity(missile);
		}
	}
}