package data.ai.weapon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import data.scripts.tools.SUN_ICE_IceUtils;
import data.scripts.tools.SUN_ICE_IntervalTracker;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.WeaponUtils;
import org.lwjgl.util.vector.Vector2f;

public class SUN_ICE_ChupacabraAutofireAIPlugin implements AutofireAIPlugin {
	private static final float UPDATE_FREQUENCY = 0.25f;

	private WeaponAPI weapon;
	private ShipAPI ship;
	private ShipAPI target;
	private final SUN_ICE_IntervalTracker timer = new SUN_ICE_IntervalTracker(UPDATE_FREQUENCY);
	private boolean shouldFire = false;

	private float getRangeToHit(Vector2f edgePoint) {
		Vector2f t = VectorUtils.getDirectionalVector(weapon.getLocation(), edgePoint);
		t.scale(weapon.getRange());
		Vector2f.add(weapon.getLocation(), t, t);
		return MathUtils.getDistance(weapon.getLocation(), edgePoint);
	}

	public SUN_ICE_ChupacabraAutofireAIPlugin() {
	}

	public SUN_ICE_ChupacabraAutofireAIPlugin(WeaponAPI weapon) {
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
			shouldFire = target != null && !target.isPhased()
					//                    && (target.getShield() == null || target.getShield().isOff()
					//                        || !target.getShield().isWithinArc(CollisionUtils.getCollisionPoint(weapon.getLocation(), targetVect, target)))
					&& SUN_ICE_IceUtils.getShipInLineOfFire(weapon) == target;
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
		Vector2f targetVect;
		if (target == null) {
			targetVect = ship.getMouseTarget();
		} else if (target.getShield() == null || target.getShield().isOff() || !target.getShield().isWithinArc(weapon.getLocation())) {
			targetVect = target.getLocation();
		} else {
			ShieldAPI shield = target.getShield();
			double angle;

			Vector2f se1 = new Vector2f(shield.getLocation());
			Vector2f se2 = new Vector2f(shield.getLocation());

			angle = Math.toRadians(MathUtils.clampAngle(shield.getFacing() - shield.getActiveArc() / 2 - 5));
			se1.x += Math.cos(angle) * shield.getRadius();
			se1.y += Math.sin(angle) * shield.getRadius();

			angle = Math.toRadians(MathUtils.clampAngle(shield.getFacing() + shield.getActiveArc() / 2 + 5));
			se2.x += Math.cos(angle) * shield.getRadius();
			se2.y += Math.sin(angle) * shield.getRadius();

			//            Global.getCombatEngine().addHitParticle(se1, new Vector2f(), 15, 1, 0.1f, Color.RED);
			//            Global.getCombatEngine().addHitParticle(se2, new Vector2f(), 15, 1, 0.1f, Color.RED);

			targetVect = (getRangeToHit(se1) < getRangeToHit(se2)) ? se1 : se2;
		}

		return targetVect;
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