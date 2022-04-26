package data.weapons.beam;

import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;

public class FissionBladePushEffect implements BeamEffectPlugin {
	private static final float FORCE_MULTIPLIER = 60f;

	@Override
	public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
		CombatEntityAPI target = beam.getDamageTarget();
		float range = beam.getWeapon().getRange();

		if (target == null) {
			return;
		}

		float force = FORCE_MULTIPLIER * beam.getBrightness() * amount * (range / (beam.getLengthPrevFrame() + (range * 0.2f)));
		float direction = VectorUtils.getFacing(target.getVelocity());
		CombatUtils.applyForce(target, direction + 180f, force);

		target.setAngularVelocity(target.getAngularVelocity() * 0.95f);
	}
}