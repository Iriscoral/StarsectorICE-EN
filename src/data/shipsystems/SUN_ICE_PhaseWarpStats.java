package data.shipsystems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public class SUN_ICE_PhaseWarpStats extends SUN_ICE_PhaseStats {

	@Override
	public void advanceImpl(MutableShipStatsAPI stats, String id, ShipAPI ship, State state, float effectLevel) {
		unsetPhaseBehavior(ship);
	}

	@Override
	public void applySpeedBonus(MutableShipStatsAPI stats, String id, ShipAPI ship, State state, float effectLevel) {
		spawnAfterImage(ship);

		stats.getMaxSpeed().modifyMult(id, 1f - effectLevel * 0.4f); // slow down initially

		float bonusMult = getSpeedMult(ship, effectLevel) * effectLevel;
		stats.getMaxSpeed().modifyFlat(id, 250f * bonusMult);
		stats.getAcceleration().modifyFlat(id, 500f * bonusMult);
		stats.getDeceleration().modifyFlat(id, 300f * bonusMult);
	}
}