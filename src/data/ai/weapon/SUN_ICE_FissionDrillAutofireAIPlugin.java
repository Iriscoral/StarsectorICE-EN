package data.ai.weapon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.AutofireAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lwjgl.util.vector.Vector2f;

public class SUN_ICE_FissionDrillAutofireAIPlugin implements AutofireAIPlugin {
	private WeaponAPI weapon;
	private ShipAPI ship;

	public SUN_ICE_FissionDrillAutofireAIPlugin() {
	}

	public SUN_ICE_FissionDrillAutofireAIPlugin(WeaponAPI weapon) {
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
		return ship.getMouseTarget();
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
		return false;
	}

	@Override
	public MissileAPI getTargetMissile() {
		return null;
	}
}