package data.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.scripts.tools.SUN_ICE_IceUtils.I18nSection;

public class SUN_ICE_SensorBoostStats extends BaseShipSystemScript {
	private static final float RANGE_BOOST = 60f;

	public static final I18nSection strings = I18nSection.getInstance("ShipSystem", "SUN_ICE_");

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		stats.getSightRadiusMod().modifyPercent(id, RANGE_BOOST * effectLevel);
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getSightRadiusMod().unmodifyPercent(id);
	}

	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) return new StatusData(strings.get("SensorBoostStats1"), false);
		return null;
	}
}