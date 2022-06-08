package data.weapons.beam;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.shipsystems.SUN_ICE_LimiterControlStats;

import java.awt.Color;

public class BladeOfAlphaEffect implements BeamEffectPlugin {
	private static final Color SHIFT_COLOR = new Color(0, 0, 0, 0);

	@Override
	public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
		ShipAPI ship = beam.getSource();
		if (engine.isPaused() || ship == null || !ship.isAlive()) {
			return;
		}

		boolean downshifted = SUN_ICE_LimiterControlStats.isDownshifted(ship);
		if (downshifted) {
			beam.setFringeColor(SHIFT_COLOR);
			beam.setCoreColor(SHIFT_COLOR);
			beam.getDamage().setDamage(0f);
		}
	}
}