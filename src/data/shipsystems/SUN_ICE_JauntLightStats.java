package data.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import data.scripts.tools.SUN_ICE_JauntSession;

public class SUN_ICE_JauntLightStats extends BaseShipSystemScript {
	private static final float RANGE_FACTOR = 1500f;

	private SUN_ICE_JauntSession session;

	public static float getRange(ShipAPI ship) {
		if (ship == null) {
			return RANGE_FACTOR;
		}
		return ship.getMutableStats().getSystemRangeBonus().computeEffective(RANGE_FACTOR);
	}

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = (ShipAPI) stats.getEntity();
		if (ship == null) {
			return;
		}

		ship.blockCommandForOneFrame(ShipCommand.VENT_FLUX);
		if (session == null) {
			session = SUN_ICE_JauntSession.getSession(ship, getRange(ship));
		} else if (state == ShipSystemStatsScript.State.OUT) {
			session.goHome();
		}
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		ShipAPI ship = (ShipAPI) stats.getEntity();
		ShipSystemAPI cloak = ship.getPhaseCloak();

		if (session != null && (cloak == null || !cloak.isActive())) {
			session.goHome();
		}

		session = null;
	}

	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		return null;
	}
}