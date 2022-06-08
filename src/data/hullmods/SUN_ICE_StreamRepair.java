package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;

import java.awt.Color;

public class SUN_ICE_StreamRepair extends BaseHullMod {

	private static final String id = "sun_ice_stream_repair_mod";
	private static final Color BORDER = new Color(147, 102, 50, 0);
	private static final Color NAME = new Color(12, 150, 131, 255);

	@Override
	public Color getBorderColor() {
		//return BORDER;
		return null;
	}

	@Override
	public Color getNameColor() {
		return NAME;
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		if (ship == null || !ship.isAlive()) return;

		if (ship.isPhased()) {
			ship.getMutableStats().getCombatEngineRepairTimeMult().modifyMult(id, 0.5f);
			ship.getMutableStats().getCombatWeaponRepairTimeMult().modifyMult(id, 0.5f);
		} else {
			ship.getMutableStats().getCombatEngineRepairTimeMult().unmodifyMult(id);
			ship.getMutableStats().getCombatWeaponRepairTimeMult().unmodifyMult(id);
		}
	}

	@Override
	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
		if (index == 0) return "50%";
		return null;
	}
}