package data.ai.weapon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import data.scripts.tools.SUN_ICE_IntervalTracker;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashMap;

public class SUN_ICE_MobiusRayAutofireAIPlugin implements AutofireAIPlugin {
	private static final HashMap<WeaponAPI, SUN_ICE_MobiusRayAutofireAIPlugin> autofireMap = new HashMap<>();
	private static final float SHOULD_FIRE_THRESHOLD = 0.3f;
	private static final float UPDATE_FREQUENCY = 0.5f;

	public static SUN_ICE_MobiusRayAutofireAIPlugin get(WeaponAPI weapon) {
		//if(!autofireMap.containsKey(weapon)) return null;

		return autofireMap.get(weapon);
	}

	private WeaponAPI weapon;
	private ShipAPI target;
	private ShipAPI activeTarget;
	private boolean isOn = false;
	private final SUN_ICE_IntervalTracker timer = new SUN_ICE_IntervalTracker(UPDATE_FREQUENCY);

	public SUN_ICE_MobiusRayAutofireAIPlugin(WeaponAPI weapon) {
		this.weapon = weapon;
		autofireMap.put(weapon, this);
	}

	public boolean isOn() {
		return isOn;
	}

	public void setIsOn(boolean on) {
		isOn = on;
	}

	private void findTarget() {
		float theta = (weapon.getCurrAngle() / 180f) * (float) Math.PI;
		float halfRange = weapon.getRange() / 2f;
		Vector2f midPoint = new Vector2f(weapon.getLocation());
		midPoint.x += (float) Math.cos(theta) * halfRange * 0.6f;
		midPoint.y += (float) Math.sin(theta) * halfRange * 0.6f;

		target = null;
		float shortestDistance = Float.MAX_VALUE;

		for (ShipAPI ship : CombatUtils.getShipsWithinRange(midPoint, halfRange * 1.3f)) {
			if (!ship.isAlive() || ship.getOwner() == weapon.getShip().getOwner() || ship.isPhased()) {
				continue;
			}

			float dist = MathUtils.getDistanceSquared(ship, midPoint);

			// Fighter and drone less likely to be chosen
			if (ship.isFighter() || ship.isDrone()) {
				dist *= 2f;
			}

			// Targeted ship more likely to be chosen
			if (ship == weapon.getShip().getShipTarget()) {
				dist *= 0.5f;
			}

			// Ships with high flux more likely to be chosen
			dist *= Math.min(1f, (1f - ship.getFluxTracker().getFluxLevel()) + 0.8f);

			if (dist < shortestDistance) {
				shortestDistance = dist;
				target = ship;
			}
		}

		if (target == null) {
			return;
		}

		midPoint.x = (weapon.getLocation().x + target.getLocation().x) / 2f;
		midPoint.y = (weapon.getLocation().y + target.getLocation().y) / 2f;
	}

	@Override
	public void advance(float amount) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (engine == null) {
			return;
		}

		if (timer.intervalElapsed(engine)) {
			findTarget();
		}
	}

	@Override
	public void forceOff() {
		if (Global.getCombatEngine() == null) {
			return;
		}

		findTarget();
	}

	@Override
	public Vector2f getTarget() {
		return (activeTarget == null) ? weapon.getShip().getMouseTarget() : activeTarget.getLocation();
	}

	@Override
	public ShipAPI getTargetShip() {
		return target;
	}

	@Override
	public WeaponAPI getWeapon() {
		return weapon;
	}

	@Override
	public boolean shouldFire() {
		isOn = true;

		if (weapon.isDisabled() || weapon.isFiring() || weapon.getCooldownRemaining() > 0 || target == null) {
			return false;
		}

		float shouldFire = 0f;

		// 0.3f - Target has high flux?
		shouldFire += Math.max(0f, target.getFluxTracker().getFluxLevel() - 0.7f);

		// 0.3 - Target defenses ready?
		if (target.getShield() != null) {
			shouldFire += 0.3f * (1 - target.getShield().getActiveArc() / 360f);
		}
		if (target.getPhaseCloak() != null) {
			shouldFire *= 2f;
		} else {
			shouldFire += 0.3f;
		}

		// 0.2 - Target damaged enough to kill in one shot?
		shouldFire += 0.2f * (Math.min(1200f, target.getHitpoints()) / 1200f);

		// 1.0f - Can we afford to use the flux?
		shouldFire += 1f - weapon.getShip().getFluxTracker().getFluxLevel();

		activeTarget = target;

		return shouldFire > SHOULD_FIRE_THRESHOLD;
	}

	@Override
	public MissileAPI getTargetMissile() {
		return null;
	}
}