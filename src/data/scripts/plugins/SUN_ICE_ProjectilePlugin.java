package data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

public class SUN_ICE_ProjectilePlugin extends BaseEveryFrameCombatPlugin {
	private static final String DATA_KEY = "SUN_ICE_ProjectilePlugin";
	private static final Vector2f ZERO = new Vector2f();
	private CombatEngineAPI engine;

	@Override
	public void advance(float amount, List<InputEventAPI> events) {
		if (engine == null || engine.isPaused()) {
			return;
		}
		final LocalData localData = (LocalData) engine.getCustomData().get(DATA_KEY);
		final Map<DamagingProjectileAPI, SineData> sineProjs = localData.sineProjs;
		final Map<DamagingProjectileAPI, BladeData> bladeProjs = localData.bladeProjs;
		List<DamagingProjectileAPI> toRemove = new ArrayList<>();
		for (DamagingProjectileAPI projectile : engine.getProjectiles()) {
			if (!engine.isEntityInPlay(projectile)) {
				continue;
			}

			String projectileID = projectile.getProjectileSpecId();
			ShipAPI ship = projectile.getSource();
			Vector2f location = projectile.getLocation();
			if (projectileID == null) {
				continue;
			}

			if (projectile.getElapsed() > 0f) {
				continue;
			}

			switch (projectileID) {
				case "sun_ice_ricochet_shot":
					sineProjs.put(projectile, new SineData(projectile));
					break;
				case "sun_ice_bladeofalpha_hack":
					bladeProjs.put(projectile, new BladeData(projectile));
					break;
			}
		}

		if (!sineProjs.isEmpty()) {
			Iterator<Map.Entry<DamagingProjectileAPI, SineData>> iter = sineProjs.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<DamagingProjectileAPI, SineData> entry = iter.next();
				DamagingProjectileAPI projectile = entry.getKey();
				SineData data = entry.getValue();
				if (projectile == null || projectile.didDamage() || !engine.isEntityInPlay(projectile)) {
					engine.removeEntity(data.projHack);
					iter.remove();
					continue;
				}

				data.advance(amount);
			}
		}

		if (!bladeProjs.isEmpty()) {
			Iterator<Map.Entry<DamagingProjectileAPI, BladeData>> iter = bladeProjs.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<DamagingProjectileAPI, BladeData> entry = iter.next();
				DamagingProjectileAPI projectile = entry.getKey();
				BladeData data = entry.getValue();
				if (projectile == null || projectile.didDamage() || !engine.isEntityInPlay(projectile)) {
					iter.remove();
					continue;
				}

				data.advance(amount);
			}
		}

		if (!toRemove.isEmpty()) {
			for (DamagingProjectileAPI remove : toRemove) {
				engine.removeEntity(remove);
			}
		}
	}

	@Override
	public void init(CombatEngineAPI engine) {
		this.engine = engine;
		Global.getCombatEngine().getCustomData().put(DATA_KEY, new LocalData());
	}

	public static final class LocalData {
		final Map<DamagingProjectileAPI, SineData> sineProjs = new HashMap<>(100);
		final Map<DamagingProjectileAPI, BladeData> bladeProjs = new HashMap<>(100);
	}

	private final static class BladeData {
		final DamagingProjectileAPI proj;
		final Vector2f lastLocation;
		final List<ShipAPI> passing;

		private BladeData(DamagingProjectileAPI proj) {
			this.proj = proj;
			lastLocation = new Vector2f(proj.getLocation());
			passing = new ArrayList<>();
		}

		private void tryToApplyDamage(CombatEntityAPI target, float amount) {
			amount *= 10f;
			if (CollisionUtils.isPointWithinBounds(proj.getLocation(), target)) {
				Global.getCombatEngine().applyDamage(target, proj.getLocation(), proj.getDamageAmount() * amount, proj.getDamageType(), proj.getEmpAmount() * amount, true, false, proj.getSource(), true);
			}
		}

		private void doFinalDamage() {
			proj.setCollisionClass(CollisionClass.PROJECTILE_FIGHTER);
		}

		private void advance(float amount) {
			for (MissileAPI enemy : AIUtils.getNearbyEnemyMissiles(proj, 1f)) {
				tryToApplyDamage(enemy, 1f);
			}

			for (CombatEntityAPI enemy : CombatUtils.getAsteroidsWithinRange(proj.getLocation(), 1f)) {
				tryToApplyDamage(enemy, 1f);
			}

			for (ShipAPI enemy : AIUtils.getNearbyEnemies(proj, 10f)) {
				if (enemy.isPhased()) {
					continue;
				}

				if (enemy.isDrone() || enemy.isFighter()) {
					tryToApplyDamage(enemy, amount * 5f);
					continue;
				}

				if (passing.contains(enemy)) {
					tryToApplyDamage(enemy, amount);
					if (MathUtils.getDistance(proj.getLocation(), enemy.getShieldCenterEvenIfNoShield()) > enemy.getShieldRadiusEvenIfNoShield() + 10f) {
						passing.remove(enemy);
					}
					return;
				}

				if (enemy.getShield() == null || enemy.getShield().isOff() || !enemy.getShield().isWithinArc(proj.getLocation())) {
					tryToApplyDamage(enemy, amount);
				} else if (MathUtils.getDistance(proj.getLocation(), enemy.getShieldCenterEvenIfNoShield()) <= enemy.getShieldRadiusEvenIfNoShield()) {//Now facing with shielded target
					if (MathUtils.getDistance(lastLocation, enemy.getShieldCenterEvenIfNoShield()) > enemy.getShieldRadiusEvenIfNoShield()) {
						doFinalDamage();
					} else {
						passing.add(enemy);
						tryToApplyDamage(enemy, amount);
					}
				}
			}

			lastLocation.set(proj.getLocation());
		}
	}

	private final static class SineData {
		final DamagingProjectileAPI proj;
		final DamagingProjectileAPI projHack;
		float timerOfGoingOut;
		float timerOfGoingBack;
		int positiveOrNegative;
		float k;
		float sideSpeedLastFrame;

		private SineData(DamagingProjectileAPI proj) {
			this.proj = proj;
			projHack = (DamagingProjectileAPI)Global.getCombatEngine().spawnProjectile(proj.getSource(), proj.getWeapon(), "sun_ice_ricochethack", proj.getLocation(), proj.getFacing(), null);

			positiveOrNegative = Math.random() < 0.5d ? 1 : -1;
			refresh();
		}

		private void refresh() {
			positiveOrNegative = -positiveOrNegative;

			Vector2f directionOfFacing = MathUtils.getPointOnCircumference(null, 1f, proj.getFacing());
			float speedInFacing = Math.abs(directionOfFacing.x * proj.getVelocity().x + directionOfFacing.y * proj.getVelocity().y);
			proj.getVelocity().set(MathUtils.getPointOnCircumference(null, speedInFacing, proj.getFacing()));

			float initForce = MathUtils.getRandomNumberInRange(3000f, 7000f);
			CombatUtils.applyForce(proj, proj.getFacing() - positiveOrNegative * 90f, initForce);
			Vector2f directionOfForce = MathUtils.getPointOnCircumference(null, 1f, proj.getFacing() + 90f);
			sideSpeedLastFrame = directionOfForce.x * proj.getVelocity().x + directionOfForce.y * proj.getVelocity().y;

			k = initForce * MathUtils.getRandomNumberInRange(1f, 5f);
			timerOfGoingOut = 0f;
			timerOfGoingBack = 0f;
		}

		private void advance(float amount) {
			Vector2f directionOfForce = MathUtils.getPointOnCircumference(null, 1f, proj.getFacing() + 90f);
			float speedInForceSide = directionOfForce.x * proj.getVelocity().x + directionOfForce.y * proj.getVelocity().y;
			if (speedInForceSide * sideSpeedLastFrame <= 0f || timerOfGoingBack > 0f) {
				if (timerOfGoingBack >= timerOfGoingOut) {
					refresh();
					return;
				} else {
					timerOfGoingBack += amount;
				}
			} else {
				timerOfGoingOut += amount;
			}

			sideSpeedLastFrame = speedInForceSide;
			CombatUtils.applyForce(proj, proj.getFacing() + positiveOrNegative * 90f, k * amount);
			projHack.getLocation().set(proj.getLocation());
			projHack.setFacing(VectorUtils.getFacing(proj.getVelocity()));
		}
	}
}