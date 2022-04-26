package data.weapons.decorative;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.scripts.tools.SUN_ICE_RecallTracker;
import org.lwjgl.util.vector.Vector2f;

public class RecallVisualEffect implements EveryFrameWeaponEffectPlugin {
	private final static float ACTIVATION_SPEED = 1f;
	private final static float DEACTIVATION_SPEED = 1f;

	private float alpha = 0f;
	private Vector2f center;

	@Override
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		if (engine.isPaused()) {
			return;
		}

		if (center == null) {
			center = new Vector2f(weapon.getSprite().getCenterX(), weapon.getSprite().getCenterY());
		}

		boolean on = weapon.getShip().isAlive() && SUN_ICE_RecallTracker.isRecalling(weapon.getShip());

		if (alpha == 0 && !on) {
			weapon.getAnimation().setFrame(0);
			return;
		}

		weapon.getSprite().setCenterX(((float) Math.random() - 0.5f) * 7f + weapon.getSprite().getWidth() / 2f);
		weapon.getSprite().setCenterY(((float) Math.random() - 0.5f) * 7f + weapon.getSprite().getHeight() / 2f);

		weapon.getSprite().setAdditiveBlend();
		weapon.getAnimation().setFrame(1);

		alpha += Global.getCombatEngine().getElapsedInLastFrame() * (on ? ACTIVATION_SPEED : -DEACTIVATION_SPEED);
		alpha = Math.max(Math.min(alpha, 1f), 0f);

		weapon.getAnimation().setAlphaMult(alpha);

		Global.getSoundPlayer().playLoop("high_intensity_laser_loop", weapon.getShip(), 1.5f, alpha, weapon.getShip().getLocation(), weapon.getShip().getVelocity());
	}
}