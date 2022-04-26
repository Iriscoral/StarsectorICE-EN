package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import org.lwjgl.util.vector.Vector2f;

public class SUN_ICE_VastBulk extends BaseHullMod {

	private static final Vector2f NORMAL_OFFSET = new Vector2f(0f, 0f);
	private static final Vector2f MAX_OFFSET = new Vector2f(100f, 0f);

	@Override
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getHullDamageTakenMult().modifyMult(id, 0f);
		stats.getArmorDamageTakenMult().modifyMult(id, 0f);
		stats.getEngineDamageTakenMult().modifyMult(id, 0f);
		stats.getWeaponDamageTakenMult().modifyMult(id, 0f);
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		if (ship == null || !ship.isAlive()) return;

		ShipAPI mainShip = ship.getParentStation();
		if (mainShip == null || !mainShip.isAlive()) return;
		if (mainShip.getPhaseCloak() == null) return;

		if (mainShip.getPhaseCloak().isActive()) {
			ship.setPhased(true);
			ship.getModuleOffset().set(MAX_OFFSET); // offset hack
		} else {
			ship.setPhased(false);
			ship.getModuleOffset().set(NORMAL_OFFSET);
		}
	}
}