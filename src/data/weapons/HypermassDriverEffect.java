package data.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.weapons.onhit.HypermassOnHitEffect;
import org.lazywizard.lazylib.combat.CombatUtils;

public class HypermassDriverEffect implements EveryFrameWeaponEffectPlugin {
	private boolean onFireEffectIsReady = true;

	@Override
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		if (onFireEffectIsReady && weapon.getCooldownRemaining() > 0f) {
			weapon.getShip().getFluxTracker().beginOverloadWithTotalBaseDuration(2f);
			CombatUtils.applyForce(weapon.getShip(), weapon.getCurrAngle() + 180f, HypermassOnHitEffect.FORCE);

			onFireEffectIsReady = false;
		} else if (!onFireEffectIsReady && weapon.getCooldownRemaining() == 0f) {
			onFireEffectIsReady = true;
		}
	}
}