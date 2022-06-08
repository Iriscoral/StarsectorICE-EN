package data.ai.weapon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import data.scripts.tools.SUN_ICE_IceUtils;
import data.scripts.tools.SUN_ICE_IntervalTracker;
import org.lwjgl.util.vector.Vector2f;

public class SUN_ICE_NovaDischargerAutofireAIPlugin implements AutofireAIPlugin {
	private static final float UPDATE_FREQUENCY = 0.3f;

	private WeaponAPI weapon;
	private ShipAPI ship;
	private ShipAPI target;
	private final SUN_ICE_IntervalTracker timer = new SUN_ICE_IntervalTracker(UPDATE_FREQUENCY);

	public SUN_ICE_NovaDischargerAutofireAIPlugin(WeaponAPI weapon) {
		this.weapon = weapon;
		this.ship = weapon.getShip();
	}

	private ShipAPI findTarget() {
		target = SUN_ICE_IceUtils.getShipInLineOfFire(weapon);

		if (target != null && target.isAlive() && target.getOwner() != ship.getOwner() && target.getShield() != null && target.getShield().isOn() && !target.isPhased()) {
			return target;
		} else {
			return target = null;
		}
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
		return ship.getMouseTarget();
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
		return target != null && (ship.getShipAI() != null || ship.getShield() == null || ship.getShield().isOff()) && !target.getFluxTracker().isOverloadedOrVenting() && target.getShield() != null && target.getShield().isOn() && !target.isPhased();
	}

	@Override
	public MissileAPI getTargetMissile() {
		return null;
	}
}