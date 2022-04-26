package data.weapons.decorative;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.shipsystems.SUN_ICE_RepairArmorStats;

public class RepairArmorVisualEffect implements EveryFrameWeaponEffectPlugin {
	private static final float ACTIVATION_SPEED = 0.3f;
	private static final float DEACTIVATION_SPEED = 0.5f;

	private float alpha = 0f;

	@Override
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		if (engine.isPaused()) {
			return;
		}

		boolean on = weapon.getShip().getSystem().isOn() && weapon.getShip().isAlive();

		if (alpha == 0f && !on) {
			weapon.getAnimation().setFrame(0);
			return;
		}

		weapon.getSprite().setAdditiveBlend();
		weapon.getAnimation().setFrame(1);

		alpha += Global.getCombatEngine().getElapsedInLastFrame() * (on ? ACTIVATION_SPEED : -DEACTIVATION_SPEED);
		alpha = Math.max(Math.min(alpha, 1f), 0f);

		weapon.getAnimation().setAlphaMult(alpha * (0.4f + SUN_ICE_RepairArmorStats.getPotency(weapon.getShip()) * 0.6f));
	}
}