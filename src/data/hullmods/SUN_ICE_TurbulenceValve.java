package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import data.scripts.tools.SUN_ICE_IceUtils;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SUN_ICE_TurbulenceValve extends BaseHullMod {
	private static final String id = "SUN_ICE_TurbulenceValve";
	private static final String PROGRESSIVE_KEY = "SUN_ICE_PhaseCloakStats";

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (!engine.getCustomData().containsKey(id)) {
			engine.getCustomData().put(id, new HashMap<>());
		}

		Map<ShipAPI, SUN_ICE_TurbulenceStarter> shipsMap = (Map) engine.getCustomData().get(id);
		if (engine.isPaused() || !engine.isEntityInPlay(ship) || !ship.isAlive()) {
			if (!ship.isAlive()) {
				if (shipsMap.containsKey(ship)) {
					shipsMap.get(ship).remove();
					shipsMap.remove(ship);
				}
			}
			return;
		}

		if (!shipsMap.containsKey(ship)) {
			shipsMap.put(ship, new SUN_ICE_TurbulenceStarter(ship));
		} else {
			SUN_ICE_TurbulenceStarter data = shipsMap.get(ship);
			data.advance(engine);
		}
	}

	@Override
	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "150";
		if (index == 1) return "200";
		if (index == 2) return "250";
		if (index == 3) return "300";
		if (index == 4) return "2";
		return null;
	}

	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		return ship != null && ship.getHullSpec().getHullId().startsWith("sun_ice_") && SUN_ICE_IceUtils.isPhysicallyPhaseShip(ship);
	}

	public static class SUN_ICE_TurbulenceStarter {

		private static final float MAX_TIMER = 0.1f;
		private static final Map<HullSize, Float> mag = new HashMap<>();
		static {
			mag.put(HullSize.FIGHTER, 100f);
			mag.put(HullSize.FRIGATE, 150f);
			mag.put(HullSize.DESTROYER, 200f);
			mag.put(HullSize.CRUISER, 250f);
			mag.put(HullSize.CAPITAL_SHIP, 300f);
		}

		private final ShipAPI ship;
		private float timer;
		private float effectLevel;

		private final SUN_ICE_TurbulenceValveVisual visual;
		private final CombatEntityAPI visualEntity;

		public SUN_ICE_TurbulenceStarter(ShipAPI ship) {
			this.ship = ship;
			this.timer = 0f;
			this.effectLevel = 0f;

			this.visual = new SUN_ICE_TurbulenceValveVisual(ship);
			this.visualEntity = Global.getCombatEngine().addLayeredRenderingPlugin(visual);
		}

		public void advance(CombatEngineAPI engine) {
			float amount = engine.getElapsedInLastFrame();

			ShipSystemAPI cloak = ship.getPhaseCloak();
			if (cloak.getCooldownRemaining() > 0f && !cloak.isOn()) {
				boolean progressiveCheck = true;
				if (ship.getCustomData().get(PROGRESSIVE_KEY) != null) {
					progressiveCheck = (float)ship.getCustomData().get(PROGRESSIVE_KEY) >= 2f;
				}

				if (progressiveCheck) {
					effectLevel += amount * 3f;
				}
			}

			float cooldownLevel = cloak.getCooldownRemaining() / cloak.getCooldown();
			effectLevel = Math.max(effectLevel, 0f);
			effectLevel = Math.min(effectLevel, cooldownLevel);

			visual.updateEffectLevel(effectLevel);
			visualEntity.getLocation().set(ship.getLocation());

			if (effectLevel > 0f) {
				timer -= amount;
				if (timer <= 0f) {
					float damage = mag.get(ship.getHullSize()) * effectLevel;
					float range = (ship.getCollisionRadius() + 200f) * effectLevel;

					float cooldown = ship.getMutableStats().getPhaseCloakCooldownBonus().computeEffective(1f);
					if (cooldown > 0f && cooldown < 1f) {
						damage /= cooldown;
					}

					for (MissileAPI missile : AIUtils.getNearbyEnemyMissiles(ship, range)) {
						engine.applyDamage(missile, missile.getLocation(), damage * MAX_TIMER, DamageType.ENERGY, 0f, false, false, ship);
					}

					AOE(ship, ship.getLocation(), damage * MAX_TIMER, 0f, DamageType.ENERGY, AIUtils.getNearbyEnemies(ship, range));
					timer = MAX_TIMER;
				}
			} else {
				timer = 0f;
			}
		}

		public void remove() {
			visual.setValid(false);
		}

		public static void AOE(ShipAPI source, Vector2f centerPoint, float damage, float empDamage, DamageType damageType, List<ShipAPI> targets) {
			for (ShipAPI target : targets) {

				float checkRange = target.getCollisionRadius() * 1.5f;
				if (target.getShield() != null) {
					checkRange = Math.max(checkRange, target.getShield().getRadius());
				}

				List<Vector2f> toHitLocations = new ArrayList<>();
				float horiAngle = MathUtils.clampAngle(VectorUtils.getAngle(centerPoint, target.getLocation()));
				float verticalAngle = MathUtils.clampAngle(horiAngle - 90f);
				for (float i = -checkRange - 10f; i <= checkRange + 10f; i += 20f) {
					Vector2f targetMiddleV2f = MathUtils.getPointOnCircumference(target.getLocation(), i, verticalAngle);
					Vector2f a_V2f = MathUtils.getPointOnCircumference(targetMiddleV2f, -checkRange, horiAngle);
					Vector2f b_V2f = MathUtils.getPointOnCircumference(targetMiddleV2f, checkRange, horiAngle);
					Vector2f collisionPoint = CollisionUtils.getCollisionPoint(a_V2f, b_V2f, target);
					if (collisionPoint != null) {
						toHitLocations.add(collisionPoint);
					}
				}

				if (!toHitLocations.isEmpty()) {
					float actualDamage = target.isFighter() || target.isDrone() ? damage : damage * 0.5f;

					for (Vector2f damagePoint : toHitLocations) {
						float dis = MathUtils.getDistance(damagePoint, centerPoint);
						Global.getCombatEngine().applyDamage(target, damagePoint, actualDamage * (1f - Math.min(1f, dis / 2000f)), damageType, empDamage, false, true, source);
					}
				}
			}
		}
	}
}