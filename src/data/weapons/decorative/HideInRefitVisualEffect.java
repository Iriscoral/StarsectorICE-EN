package data.weapons.decorative;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.scripts.tools.SUN_ICE_IceUtils;

public class HideInRefitVisualEffect implements EveryFrameWeaponEffectPlugin {

	@Override
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		if (engine.isPaused() || weapon == null || weapon.getShip() == null) {
			return;
		}

		weapon.getSprite().setAlphaMult(SUN_ICE_IceUtils.isInRefit() ? 0f : 1f);
	}
}