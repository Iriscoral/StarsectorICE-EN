package data.shipsystems;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.scripts.tools.SUN_ICE_IceUtils;
import data.scripts.tools.SUN_ICE_IceUtils.I18nSection;

public class SUN_ICE_EntropicInversionMatrixStats extends BaseShipSystemScript {
	private float[][] prev = null;
	private float initialFlux = Float.MIN_VALUE;

	public static final I18nSection strings = I18nSection.getInstance("ShipSystem", "SUN_ICE_");

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = (ShipAPI) stats.getEntity();

		float[][] now = ship.getArmorGrid().getGrid();
		float max = ship.getArmorGrid().getMaxArmorInCell();
		int gridWidth = now.length;
		int gridHeight = now[0].length;

		if (initialFlux == Float.MIN_VALUE) {
			initialFlux = ship.getFluxTracker().getCurrFlux();
		}

		if (prev == null) {
			prev = new float[gridWidth][gridHeight];
			for (int x = 0; x < gridWidth; x++) {
				for (int y = 0; y < gridHeight; y++) {
					prev[x][y] = now[x][y];
				}
			}
		}

		for (int x = 0; x < gridWidth; x++) {
			for (int y = 0; y < gridHeight; y++) {
				float val = Math.max(now[x][y], 10);
				float repairAmount = Math.max(prev[x][y] - val, 0);
				val += repairAmount * 2 * effectLevel;
				val = Math.min(val, max);

				ArmorGridAPI armorGrid = ship.getArmorGrid();
				armorGrid.setArmorValue(x, y, val);
				prev[x][y] = val;

				SUN_ICE_IceUtils.showHealText(ship, armorGrid.getLocation(x, y), repairAmount);
			}
		}

		ship.getFluxTracker().setCurrFlux(ship.getFluxTracker().getMaxFlux() * 0.95f);
		stats.getHullDamageTakenMult().modifyMult(id, 0f);
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getHullDamageTakenMult().unmodify(id);
		prev = null;
		((ShipAPI) stats.getEntity()).getFluxTracker().setCurrFlux(initialFlux);
		initialFlux = Float.MIN_VALUE;
	}

	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) return new StatusData(strings.get("EntropicInversionMatrixStats1"), false);

		return null;
	}
}