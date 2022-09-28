package data.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.shipsystems.SUN_ICE_LimiterControlStats;
import org.lwjgl.util.vector.Vector2f;

public class BladeOfAlphaEffect implements EveryFrameWeaponEffectPlugin {
	private static final Vector2f ZERO = new Vector2f();
	private boolean newStart = true;
	private float lastChargeLevel = 0f;

	@Override
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		ShipAPI ship = weapon.getShip();
		if (engine.isPaused() || ship == null || !ship.isAlive()) {
			return;
		}

		boolean downshifted = SUN_ICE_LimiterControlStats.isDownshifted(ship);
		if (downshifted && newStart && weapon.getChargeLevel() > lastChargeLevel) {
			newStart = false;
			Global.getSoundPlayer().playSound("sun_ice_bladeofalpha_proj", 1f, 1f, weapon.getLocation(), ZERO);
			for (int i = -2; i < 3; i++) {
				engine.spawnProjectile(ship, weapon, "sun_ice_bladeofalphahack", weapon.getLocation(), weapon.getCurrAngle() + i * 22.5f, null);
			}
		}

		if (weapon.getChargeLevel() < lastChargeLevel && weapon.getChargeLevel() == 0f) {
			newStart = true;
		}

		lastChargeLevel = weapon.getChargeLevel();
	}
}