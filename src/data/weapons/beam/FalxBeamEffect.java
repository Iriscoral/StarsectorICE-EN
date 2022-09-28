package data.weapons.beam;

import com.fs.starfarer.api.combat.*;
import data.scripts.plugins.SUN_ICE_EveryFramePlugin;

public class FalxBeamEffect implements BeamEffectPlugin {
	private static final float MAX_ARC_REDUCTION_PER_SECOND = 80f;

	@Override
	public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
		if (!(beam.getDamageTarget() instanceof ShipAPI)) {
			return;
		}

		ShipAPI target = ((ShipAPI) beam.getDamageTarget());
		ShieldAPI shield = target.getShield();

		if (shield == null || !shield.isWithinArc(beam.getTo())) {
			return;
		}

		float arc = shield.getActiveArc() - amount * beam.getBrightness() * (float) Math.pow(shield.getActiveArc() / shield.getArc(), 2) * MAX_ARC_REDUCTION_PER_SECOND;
		shield.setActiveArc(arc);

		SUN_ICE_EveryFramePlugin.tagForShieldEffectRefund(target);
	}
}