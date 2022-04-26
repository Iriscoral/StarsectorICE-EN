package data.weapons.decorative;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.shipsystems.SUN_ICE_LimiterControlStats;

import java.awt.*;

public class SystemVisualEffect implements EveryFrameWeaponEffectPlugin {
	private static final Color SPECIAL_LIGHT = new Color(255, 221, 127);
	private static final Color SPECIAL_GLOW = new Color(255, 255, 255, 5);
	private final static float ACTIVATION_SPEED = 1f;
	private final static float DEACTIVATION_SPEED = 1f;

	private float alpha = 0f;

	@Override
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		if (engine.isPaused()) {
			return;
		}

		ShipAPI ship = weapon.getShip();

		boolean downshifted = SUN_ICE_LimiterControlStats.isDownshifted(ship);
		if (downshifted) {
			ship.getEngineController().fadeToOtherColor(ship, SPECIAL_LIGHT, SPECIAL_GLOW, alpha, 1f);
		}

		boolean usingSystem = ship.getSystem() != null && ship.getSystem().isActive();
		boolean usingPhase = ship.getPhaseCloak() != null && (ship.getPhaseCloak().isActive() || ship.getPhaseCloak().isCoolingDown());
		boolean on = ship.isAlive() && !ship.getFluxTracker().isOverloadedOrVenting() && !usingPhase && (usingSystem || downshifted);

		if (alpha == 0f && !on) {
			weapon.getAnimation().setFrame(0);
			return;
		}

		weapon.getSprite().setAdditiveBlend();
		weapon.getAnimation().setFrame(1);

		alpha += Global.getCombatEngine().getElapsedInLastFrame() * (on ? ACTIVATION_SPEED : -DEACTIVATION_SPEED);
		alpha = Math.max(Math.min(alpha, 1f), 0f);

		weapon.getAnimation().setAlphaMult(alpha);
	}
}