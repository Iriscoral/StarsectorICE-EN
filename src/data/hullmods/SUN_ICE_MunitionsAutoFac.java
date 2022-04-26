package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;

public class SUN_ICE_MunitionsAutoFac extends BaseHullMod {

	public static final String id = "sun_ice_munitions_autofac_mod";

	@Override
	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "50%";
		return null;
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {

		if (!ship.isAlive() || ship.getFullTimeDeployed() > 0f) return;

		for (WeaponAPI weapon : ship.getAllWeapons()) {
			if (weapon.getType() == WeaponAPI.WeaponType.SYSTEM) continue;
			if (!weapon.usesAmmo()) continue;
			if (weapon.getAmmoPerSecond() == 0f) continue;

			float op = weapon.getSpec().getOrdnancePointCost(null);
			if (op <= 0f) continue;

			float maxAmmo = weapon.getMaxAmmo();
			float reloadRate = weapon.getAmmoPerSecond();

			float nuCharge = reloadRate + 0.25f * maxAmmo / op;
			if (nuCharge > 0f) {
				weapon.getAmmoTracker().setAmmoPerSecond(nuCharge);
			}
		}
	}

	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		return ship != null && ship.getHullSpec().getHullId().startsWith("sun_ice_");
	}
}