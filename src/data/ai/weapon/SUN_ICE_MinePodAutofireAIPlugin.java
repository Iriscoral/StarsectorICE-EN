package data.ai.weapon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.AutofireAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lwjgl.util.vector.Vector2f;

public class SUN_ICE_MinePodAutofireAIPlugin implements AutofireAIPlugin {
	private static final float SHOULD_FIRE_THRESHOLD = 0f;

	private WeaponAPI weapon;
	private ShipAPI ship;
	private final Vector2f target = new Vector2f();

	public SUN_ICE_MinePodAutofireAIPlugin() {
	}

	public SUN_ICE_MinePodAutofireAIPlugin(WeaponAPI weapon) {
		this.weapon = weapon;
		this.ship = weapon.getShip();
	}

	private ShipAPI findTarget() {
		return null;
	}

	@Override
	public void advance(float amount) {
		if (Global.getCombatEngine() == null) {
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
		return target;
	}

	@Override
	public ShipAPI getTargetShip() {
		return null;
	}

	@Override
	public WeaponAPI getWeapon() {
		return weapon;
	}

	@Override
	public boolean shouldFire() {
		float shouldFire = 1;

		// - distance from ship
		// - nearby enemies with PD
		// - distance from center
		// + near objectives

		return shouldFire > SHOULD_FIRE_THRESHOLD;
	}

	@Override
	public MissileAPI getTargetMissile() {
		return null;
	}
}