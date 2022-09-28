package data.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import data.scripts.tools.SUN_ICE_IceUtils.I18nSection;

public class SUN_ICE_FluxDriveStats extends BaseShipSystemScript {

	public static final I18nSection strings = I18nSection.getInstance("ShipSystem", "SUN_ICE_");

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (state == ShipSystemStatsScript.State.OUT) {
			stats.getMaxSpeed().unmodify(id);
			stats.getMaxTurnRate().unmodify(id);
		} else {
			stats.getMaxSpeed().modifyFlat(id, 200f * effectLevel);
			stats.getAcceleration().modifyFlat(id, 200f * effectLevel);
			stats.getDeceleration().modifyFlat(id, 150f * effectLevel);
			stats.getMaxTurnRate().modifyMult(id, 1f - 0.8f * effectLevel);
			stats.getTurnAcceleration().modifyMult(id, 1f - 0.8f * effectLevel);
		}
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getMaxSpeed().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
	}

	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) return new StatusData(strings.get("FluxDriveStats1"), false);
		return null;
	}
}