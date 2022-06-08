package data.ai.weapon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import data.scripts.tools.SUN_ICE_IceUtils;
import data.scripts.tools.SUN_ICE_IntervalTracker;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.WeaponUtils;
import org.lwjgl.util.vector.Vector2f;

public class SUN_ICE_NosAutofireAIPlugin implements AutofireAIPlugin {
	private static final float UPDATE_FREQUENCY = 0.25f;

	private WeaponAPI weapon;
	private ShipAPI ship;
	private ShipAPI target;
	Vector2f targetVect;
	private final SUN_ICE_IntervalTracker timer = new SUN_ICE_IntervalTracker(UPDATE_FREQUENCY);
	private boolean shouldFire = false;

	float getRangeToHit(Vector2f edgePoint) {
		Vector2f t = VectorUtils.getDirectionalVector(weapon.getLocation(), edgePoint);
		t.scale(weapon.getRange());
		Vector2f.add(weapon.getLocation(), t, t);
		return MathUtils.getDistance(weapon.getLocation(), edgePoint);
	}

	public SUN_ICE_NosAutofireAIPlugin(WeaponAPI weapon) {
		this.weapon = weapon;
		this.ship = weapon.getShip();
	}

	private ShipAPI findTarget() {
		ShipAPI enemy = WeaponUtils.getNearestEnemyInArc(weapon);

		return target = enemy != null && enemy.isAlive() ? enemy : null;
	}

	@Override
	public void advance(float amount) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (engine == null) {
			return;
		}

		if (timer.intervalElapsed(engine)) {
			findTarget();
			shouldFire = target != null && !target.isPhased() && (target.getShield() == null || target.getShield().isOff() || !target.getShield().isWithinArc(weapon.getLocation())) && SUN_ICE_IceUtils.getShipInLineOfFire(weapon) == target;
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
		return shouldFire;
	}

	@Override
	public MissileAPI getTargetMissile() {
		return null;
	}
}