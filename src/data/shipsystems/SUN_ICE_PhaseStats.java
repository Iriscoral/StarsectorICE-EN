package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.combat.PhaseCloakStats;

import java.awt.*;

public class SUN_ICE_PhaseStats extends PhaseCloakStats {

	private static final Color DEFAULT_AE_COLOR = new Color(175, 245, 240, 15);
	private static final String PHASE_PENALTY = "SUN_ICE_PhaseStats_PHASE_PENALTY";

	public static void phaseOn(ShipAPI ship, String id, float effectLevel, float MaxTimeMult, float shipAlphaMult, MutableShipStatsAPI stats) {
		id = id + "_" + ship.getId();
		if (Global.getCombatEngine().isPaused()) {
			return;
		}

		float speedPercentMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_SPEED_MOD).computeEffective(0f);
		float accelPercentMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_ACCEL_MOD).computeEffective(0f);
		stats.getMaxSpeed().modifyPercent(id, speedPercentMod * effectLevel);
		stats.getAcceleration().modifyPercent(id, accelPercentMod * effectLevel);
		stats.getDeceleration().modifyPercent(id, accelPercentMod * effectLevel);

		float speedMultMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_SPEED_MOD).getMult();
		float accelMultMod = stats.getDynamic().getMod(Stats.PHASE_CLOAK_ACCEL_MOD).getMult();
		stats.getMaxSpeed().modifyMult(id, speedMultMod * effectLevel);
		stats.getAcceleration().modifyMult(id, accelMultMod * effectLevel);
		stats.getDeceleration().modifyMult(id, accelMultMod * effectLevel);

		ship.setExtraAlphaMult(1f - (1f - shipAlphaMult) * effectLevel);
		ship.setApplyExtraAlphaToEngines(true);
		if (effectLevel >= 0.5f) {
			ship.setPhased(true);
		}

		if (MaxTimeMult != 1f) {
			float shipTimeMult = 1f + (MaxTimeMult - 1f) * effectLevel * stats.getDynamic().getValue(Stats.PHASE_TIME_BONUS_MULT);
			stats.getTimeMult().modifyMult(id, shipTimeMult);
			if (ship == Global.getCombatEngine().getPlayerShip()) {
				Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / shipTimeMult);
			} else {
				Global.getCombatEngine().getTimeMult().unmodify(id);
			}
		}
	}

	public static void phaseOff(ShipAPI ship, String id, MutableShipStatsAPI stats) {
		id = id + "_" + ship.getId();

		Global.getCombatEngine().getTimeMult().unmodify(id);
		stats.getTimeMult().unmodify(id);
		stats.getMaxSpeed().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);

		ship.setPhased(false);
		ship.setExtraAlphaMult(1f);
		ship.setApplyExtraAlphaToEngines(false);
	}

	public static void unsetPhaseBehavior(ShipAPI ship) {
		ship.getAIFlags().unsetFlag(ShipwideAIFlags.AIFlags.PHASE_ATTACK_RUN);
		ship.getAIFlags().unsetFlag(ShipwideAIFlags.AIFlags.PHASE_ATTACK_RUN_IN_GOOD_SPOT);
		ship.getAIFlags().unsetFlag(ShipwideAIFlags.AIFlags.PHASE_ATTACK_RUN_FROM_BEHIND_DIST_CRITICAL);
		ship.getAIFlags().unsetFlag(ShipwideAIFlags.AIFlags.PHASE_ATTACK_RUN_TIMEOUT);
	}

	public static void spawnAfterImage(ShipAPI ship) {
		ship.addAfterimage(DEFAULT_AE_COLOR, 0f, 0f, -ship.getVelocity().x, -ship.getVelocity().y, 0f, 0f, 0.05f, 0.5f, true, false, false);
	}

	public static void spawnCoilJitter(ShipAPI ship, float level) {
		ShipSystemAPI cloak = ship.getPhaseCloak();
		if (cloak == null) return;
		if (!(cloak instanceof PhaseCloakSystemAPI)) return;

		((PhaseCloakSystemAPI)cloak).setMinCoilJitterLevel(level);
	}

	public static void despawnCoilJitter(ShipAPI ship) {
		ShipSystemAPI cloak = ship.getPhaseCloak();
		if (cloak == null) return;
		if (!(cloak instanceof PhaseCloakSystemAPI)) return;

		((PhaseCloakSystemAPI)cloak).setMinCoilJitterLevel(0f);
	}

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = (ShipAPI)stats.getEntity();
		if (ship == null) return;

		ShipSystemAPI cloak = ship.getPhaseCloak();
		if (cloak == null) return;

		advanceImpl(stats, id, ship, state, effectLevel);
		if (ship == Global.getCombatEngine().getPlayerShip()) {
			maintainStatus(ship, state, effectLevel);
		}

		float timeMult = getTimeMult(ship);
		float alphaMult = getAlphaMult(ship);
		phaseOn(ship, id, effectLevel, timeMult, alphaMult, stats);

		if (state == State.OUT) {
			stats.getMaxSpeed().unmodify(id); // to slow down ship to its regular top speed while powering drive down
			stats.getMaxSpeed().unmodify(PHASE_PENALTY);

			if (effectLevel < 0.5f) {
				ship.setPhased(false);
			}
		} else {
			applySpeedBonus(stats, id, ship, state, effectLevel);
			applySpeedPenalty(stats, id, ship, state, effectLevel);
		}
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		ShipAPI ship = (ShipAPI)stats.getEntity();
		if (ship == null) return;

		phaseOff(ship, id, stats);
		unapplyImpl(stats, id, ship);

		stats.getMaxSpeed().unmodify(PHASE_PENALTY);
		stats.getMaxSpeed().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
	}

	public void advanceImpl(MutableShipStatsAPI stats, String id, ShipAPI ship, State state, float effectLevel) {

	}

	public void unapplyImpl(MutableShipStatsAPI stats, String id, ShipAPI ship) {

	}

	public void applySpeedBonus(MutableShipStatsAPI stats, String id, ShipAPI ship, State state, float effectLevel) {

	}

	public void applySpeedPenalty(MutableShipStatsAPI stats, String id, ShipAPI ship, State state, float effectLevel) {
		//stats.getMaxSpeed().modifyMult(PHASE_PENALTY, getSpeedMult(ship, effectLevel));
	}

	@Override
	public void maintainStatus(ShipAPI playerShip, State state, float effectLevel) {
		ShipSystemAPI cloak = playerShip.getPhaseCloak();
		if (cloak == null) return;

		if (getDisruptionLevel(playerShip) <= 0f) {
			Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY3,
					cloak.getSpecAPI().getIconSpriteName(), getString("PhaseStableStats1"), getString("PhaseStableStats2"), false);
		} else {
			String speedPercentStr = Math.round(getSpeedMult(playerShip, effectLevel) * 100f) + "%";
			Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY3,
					cloak.getSpecAPI().getIconSpriteName(),
					getString("PhaseStressStats1"),
					getString("PhaseStressStats2") + speedPercentStr, true);
		}
	}

	public float getTimeMult(ShipAPI ship) {
		return 3f;
	}

	public float getAlphaMult(ShipAPI ship) {
		return 0.25f;
	}

	public static String getString(String key) {
		return Global.getSettings().getString("ShipSystem", "SUN_ICE_" + key);
	}
}