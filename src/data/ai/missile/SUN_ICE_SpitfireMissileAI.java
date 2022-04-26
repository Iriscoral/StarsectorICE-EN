package data.ai.missile;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MissileAPI;
import data.scripts.tools.SUN_ICE_IceUtils;

public class SUN_ICE_SpitfireMissileAI extends SUN_ICE_BaseMissileAI {
	private boolean fizzled = false;
	private final double waveOffset;

	public SUN_ICE_SpitfireMissileAI(MissileAPI missile) {
		this.missile = missile;
		findTarget();
		waveOffset = (Math.random() * Math.PI * 2d);
	}

	@Override
	public void evaluateCircumstances() {
		if (target == null) {
			findTarget();
		}
		if (fizzled && Math.random() < 0.1f) {
			SUN_ICE_IceUtils.destroy(missile);
		}
	}

	@Override
	public void advance(float amount) {
		super.advance(amount);

		if (target == null || fizzled) {
			return;
		}

		accelerate();

		if (missile.isFizzling()) {
			fizzled = true;
		}

		double wave = Math.sin(waveOffset + Global.getCombatEngine().getTotalElapsedTime(false) * 5d);
		if (wave < 0.5d) {
			strafeToward(target);
		} else if (wave > 0.8d) {
			//turnAway(target);
			strafeAway(target);
		}
		turnToward(target);
	}
}