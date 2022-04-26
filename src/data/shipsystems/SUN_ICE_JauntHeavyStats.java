package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import data.scripts.tools.SUN_ICE_JauntSession;

public class SUN_ICE_JauntHeavyStats extends BaseShipSystemScript {
	public static final float RANGE_FACTOR = 800f;

	private SUN_ICE_JauntSession session;

	private static String getString(String key) {
		return Global.getSettings().getString("ShipSystem", "SUN_ICE_" + key);
	}

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
		float amount = Global.getCombatEngine().getElapsedInLastFrame();

		if (session == null) {
			session = SUN_ICE_JauntSession.getSession((ShipAPI) stats.getEntity(), getRange(ship));
		}

		if (state == ShipSystemStatsScript.State.ACTIVE) {
			stats.getTurnAcceleration().modifyFlat(id, 50f);
			stats.getMaxTurnRate().modifyFlat(id, 25f);

			// Make sure the ship's rotation slows down to a reasonable rate
			float turnRate = ship.getAngularVelocity();
			float turnRateLimit = ship.getMutableStats().getMaxTurnRate().getModifiedValue();
			if (Math.abs(turnRate) > turnRateLimit) {
				ship.setAngularVelocity(turnRate * (1f - amount * 2f));
			}
		} else {
			// Apply during-warp bonuses
			if (ship.getFluxTracker().isOverloadedOrVenting()) {
				stats.getMaxTurnRate().unmodify(id);
				stats.getTurnAcceleration().unmodify(id);
			} else {
				stats.getTurnAcceleration().modifyFlat(id, 300f);
				stats.getMaxTurnRate().modifyFlat(id, 150f);
			}

			if (state == ShipSystemStatsScript.State.OUT) {
				session.goHome();
			}
		}
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getMaxTurnRate().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);

		if (session != null) {
			session.goHome();
			session = null;
		}
	}

	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) return new StatusData(getString("JauntHeavyStats1"), false);
		return null;
	}
}