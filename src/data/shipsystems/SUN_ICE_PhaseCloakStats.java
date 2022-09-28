package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import org.lwjgl.util.vector.Vector2f;

import java.text.DecimalFormat;

public class SUN_ICE_PhaseCloakStats extends SUN_ICE_PhaseStats {
	private static final String PROGRESSIVE_KEY = "SUN_ICE_PhaseCloakStats";
	private static final Vector2f ZERO = new Vector2f();
	private static final DecimalFormat DF = new DecimalFormat("#.00");

	private float init = -1f;

	private static String processDecimal(float num) {
		return DF.format(num);
	}

	@Override
	public void advanceImpl(MutableShipStatsAPI stats, String id, ShipAPI ship, State state, float effectLevel) {
		unsetPhaseBehavior(ship);
		if (ship.isDrone() || ship.isFighter()) return;

		if (init < 0f) {
			init = Global.getCombatEngine().getTotalElapsedTime(false);
		}

		float pitch = getTimeMult(ship) * effectLevel * 0.25f + 1f;
		float volume = ship == Global.getCombatEngine().getPlayerShip() ? 1f : 0.5f;
		volume *= effectLevel;

		switch (ship.getHullSize()) {
			case CRUISER:
				volume *= 0.8f;
			case CAPITAL_SHIP:
				Global.getSoundPlayer().playLoop("sun_ice_system_phasecloak_loop_large", ship, pitch, volume, ship.getLocation(), ZERO);
				break;
			case FRIGATE:
				volume *= 0.8f;
			case DESTROYER:
				Global.getSoundPlayer().playLoop("sun_ice_system_phasecloak_loop_small", ship, pitch, volume, ship.getLocation(), ZERO);
				break;
		}
	}

	@Override
	public void unapplyImpl(MutableShipStatsAPI stats, String id, ShipAPI ship) {
		init = -1f;
	}

	@Override
	public void applySpeedBonus(MutableShipStatsAPI stats, String id, ShipAPI ship, State state, float effectLevel) {
		spawnAfterImage(ship);

		ship.setCustomData(PROGRESSIVE_KEY, getTimeMult(ship));

		float bonusMult = getSpeedMult(ship, effectLevel) * effectLevel;
		stats.getMaxSpeed().modifyFlat(id, 50f * bonusMult);
		stats.getAcceleration().modifyFlat(id, 100f * bonusMult);
		stats.getDeceleration().modifyFlat(id, 50f * bonusMult);
	}

	@Override
	public float getTimeMult(ShipAPI ship) {
		if (ship.isDrone() || ship.isFighter()) return 1f;

		float clock = Global.getCombatEngine().getTotalElapsedTime(false) - init;
		return 1f + clock * 0.5f;
	}

	@Override
	public void maintainStatus(ShipAPI playerShip, State state, float effectLevel) {
		super.maintainStatus(playerShip, state, effectLevel);

		float displayTimeMult = getTimeMult(playerShip);
		ShipSystemAPI cloak = playerShip.getPhaseCloak();
		Global.getCombatEngine().maintainStatusForPlayerShip(cloak, cloak.getSpecAPI().getIconSpriteName(), cloak.getDisplayName(), strings.get("PhaseCloakStats1") + processDecimal(displayTimeMult), false);
	}

	@Override
	public void applySpeedPenalty(MutableShipStatsAPI stats, String id, ShipAPI ship, State state, float effectLevel) {
		// empty
	}
}