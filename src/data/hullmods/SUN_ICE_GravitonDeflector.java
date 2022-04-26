package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.tools.SUN_ICE_IceUtils;
import org.lazywizard.lazylib.FastTrig;

public class SUN_ICE_GravitonDeflector extends BaseHullMod {

	private static final float FORCE_MULTIPLIER = 250f;
	private static final float MAX_ANGLE_DIFFERENCE = 20f;

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {

		if (ship == null || !ship.isAlive()) return;
		if (ship.getFluxTracker().isOverloadedOrVenting()) return;

		if (ship.isPhased()) return;
		if (ship.getSystem() != null && ship.getSystem().isOn()) return;

		float force = (float) (FORCE_MULTIPLIER * FastTrig.cos(ship.getFluxTracker().getFluxLevel() * Math.PI * 0.5));
		SUN_ICE_IceUtils.curveBullets(ship.getLocation(), ship.getFacing(), MAX_ANGLE_DIFFERENCE, force);
	}

	@Override
	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
		if (index == 0) return "40";
		return null;
	}
}