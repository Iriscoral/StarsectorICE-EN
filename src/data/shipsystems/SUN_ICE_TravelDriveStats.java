package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import data.hullmods.SUN_ICE_GravimetricSensors;

import java.awt.*;

public class SUN_ICE_TravelDriveStats extends BaseShipSystemScript {

	private final FaderUtil diveFader = new FaderUtil(1f, 1f);

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

		if (state == State.IDLE) {
			return;
		}

		if (state == State.OUT) {
			stats.getMaxSpeed().unmodifyFlat(id); // to slow down ship to its regular top speed while powering drive down
		} else {
			stats.getMaxSpeed().modifyFlat(id, 600f * effectLevel);
			stats.getAcceleration().modifyFlat(id, 600f * effectLevel);
			stats.getHardFluxDissipationFraction().modifyFlat(id, 1f);
			stats.getFluxDissipation().modifyPercent(id, 100f);
			stats.getOverloadTimeMod().modifyMult(id, 0f);

			ShipAPI ship = (ShipAPI)stats.getEntity();
			if (ship == null || !ship.isAlive()) return;

			if (ship.getPhaseCloak() != null) {
				ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
				ship.getPhaseCloak().forceState(ShipSystemAPI.SystemState.IN, effectLevel);
			} else {
				ship.setPhased(true);
			}

			if (ship.isRetreating() && ship.getFullTimeDeployed() > 5f) { // true retreating
				ship.getVelocity().scale(1f - effectLevel);
				ship.getEngineController().extendFlame(this, 1f - effectLevel, 1f - effectLevel, 1f - effectLevel);
				if (effectLevel == 1f) {
					if (diveFader.isIdle()) {
						Global.getSoundPlayer().playSound("phase_anchor_vanish", 1f, 1f, ship.getLocation(), ship.getVelocity());
					}

					float amount = Global.getCombatEngine().getElapsedInLastFrame();
					diveFader.fadeOut();
					diveFader.advance(amount);

					float b = diveFader.getBrightness();
					ship.setExtraAlphaMult2(b);

					float r = ship.getCollisionRadius() * 5f;
					Color c = SUN_ICE_GravimetricSensors.SUN_ICE_PhaseAnchorScript.DEFAULT_DIVE_COLOR;
					if (ship.getPhaseCloak() != null) {
						c = ship.getPhaseCloak().getSpecAPI().getEffectColor2();
						c = Misc.setAlpha(c, 255);
						c = Misc.interpolateColor(c, Color.white, 0.5f);
					}
					ship.setJitter(this, c, b, 20, r * (1f - b));

					if (diveFader.isFadedOut()) {
						ship.getLocation().set(0, -1000000f);
					}
				}
			}
		}
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getMaxSpeed().unmodifyFlat(id);
		stats.getAcceleration().unmodifyFlat(id);
		stats.getHardFluxDissipationFraction().unmodifyFlat(id);
		stats.getFluxDissipation().unmodifyPercent(id);
		stats.getOverloadTimeMod().unmodifyMult(id);

		ShipAPI ship = (ShipAPI)stats.getEntity();
		if (ship == null || !ship.isAlive()) return;
		ship.setPhased(false);
		diveFader.forceOut();
	}
	
	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		return null;
	}
}