package data.ai.weapon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.AutofireAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.scripts.tools.SUN_ICE_IceUtils;
import org.lwjgl.util.vector.Vector2f;

public class SUN_ICE_HypermassDriverAutofireAIPlugin implements AutofireAIPlugin {
	private static final float SHOULD_FIRE_THRESHOLD = 0.0f;
	private static final float UPDATE_FREQUENCY = 0.3f;

	private WeaponAPI weapon;
	private ShipAPI ship;
	private ShipAPI target;
	private float timeOfNextUpdate = 0;

	private boolean targetWillDieSoonAnyway = false;
	private float hitChance;
	private float danger;
	private float overloadBalance;
	private float fpRatio;

	public SUN_ICE_HypermassDriverAutofireAIPlugin(WeaponAPI weapon) {
		this.weapon = weapon;
		this.ship = weapon.getShip();
	}

	private ShipAPI findTarget() {
		target = SUN_ICE_IceUtils.getShipInLineOfFire(weapon);

		if (target != null && target.isAlive() && target.getOwner() != ship.getOwner()) {
			return target;
		} else {
			return target = null;
		}
	}

	@Override
	public void advance(float amount) {
		if (Global.getCombatEngine() == null) {
			return;
		}

		float t = Global.getCombatEngine().getTotalElapsedTime(false);
		if (t > timeOfNextUpdate) {
			timeOfNextUpdate = t + UPDATE_FREQUENCY;

			if (findTarget() == null) {
				return;
			}

			float selfOverloadTime = SUN_ICE_IceUtils.getBaseOverloadDuration(ship);
			float targetOverloadTime = SUN_ICE_IceUtils.estimateOverloadDurationOnHit(target, weapon.getDerivedStats().getDamagePerShot(), weapon.getDamageType());
			float incomingMissileDamage = SUN_ICE_IceUtils.estimateIncomingMissileDamage(ship);
			float fpOfSupport = SUN_ICE_IceUtils.getFPWorthOfSupport(ship, 2000f);
			float fpOfEnemies = SUN_ICE_IceUtils.getFPWorthOfHostility(ship, 2000f);
			fpOfEnemies = Math.max(0, fpOfEnemies - SUN_ICE_IceUtils.getFPCost(target) / 2f);

			hitChance = SUN_ICE_IceUtils.getHitChance(weapon, target);
			targetWillDieSoonAnyway = (SUN_ICE_IceUtils.getLifeExpectancy(ship) < 3f);
			overloadBalance = targetOverloadTime - selfOverloadTime;
			fpRatio = SUN_ICE_IceUtils.getFPStrength(target) / SUN_ICE_IceUtils.getFPStrength(ship);
			danger = Math.max(0f, fpOfEnemies - fpOfSupport + incomingMissileDamage / 100f);
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
		if (target == null || !target.isAlive() || targetWillDieSoonAnyway || (target.getPhaseCloak() != null && !target.getFluxTracker().isOverloadedOrVenting())) {
			return false;
		}

		float shouldFire = -danger;
		if (target.getShield() != null && target.getShield().isWithinArc(weapon.getLocation())) {
			shouldFire += overloadBalance * 8f * fpRatio;
		} else {
			shouldFire += Math.min(1f, weapon.getDerivedStats().getDamagePerShot() / target.getHitpoints()) * 16f * fpRatio;
		}

		shouldFire *= hitChance;
		return shouldFire > SHOULD_FIRE_THRESHOLD;
	}

	@Override
	public MissileAPI getTargetMissile() {
		return null;
	}
}