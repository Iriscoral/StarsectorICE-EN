package data.weapons.beam;

import com.fs.starfarer.api.combat.*;
import data.scripts.plugins.SUN_ICE_EveryFramePlugin;

public class NovaBeamEffect implements BeamEffectPlugin {
	private static final float MAX_ARC_REDUCTION_PER_SECOND = 90f;

	@Override
	public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
		if (beam.getSource().getShield() != null && beam.getSource().getShield().isOn()) {
			beam.getSource().getShield().setActiveArc(beam.getSource().getShield().getActiveArc() * (1f - beam.getBrightness()));
			SUN_ICE_EveryFramePlugin.tagForShieldEffectRefund(beam.getSource());
		}

		if (!(beam.getDamageTarget() instanceof ShipAPI)) {
			return;
		}

		ShipAPI target = ((ShipAPI) beam.getDamageTarget());
		ShieldAPI shield = target.getShield();

		if (shield == null) {
			return;
		}

		float arc = Math.max(-30f, shield.getActiveArc() - amount * beam.getBrightness() * MAX_ARC_REDUCTION_PER_SECOND);

		shield.setActiveArc(arc);
		SUN_ICE_EveryFramePlugin.tagForShieldEffectRefund(target);
	}
}