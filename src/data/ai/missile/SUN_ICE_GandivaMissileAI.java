package data.ai.missile;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MissileAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;

public class SUN_ICE_GandivaMissileAI extends SUN_ICE_BaseMissileAI {
	private static final float[] STAGE_DURATION = {2f, 1.5f, 3600f};
	private static final String SOUND_ID = "engine_accelerate";
	private static final float SOUND_PITCH = 0.5f;
	private static final float SOUND_VOLUME = 3.0f;
	private static final float FAKE_TURN_MODIFIER = 15f;

	private int stage = 0; // 0:Drift, 1:Burn, 2:Cruise
	private float duration = STAGE_DURATION[stage];

	public SUN_ICE_GandivaMissileAI(MissileAPI missile) {
		this.missile = missile;
		findTarget();
	}

	@Override
	public void evaluateCircumstances() {
		if (target == null) {
			findTarget();
		}
	}

	@Override
	public void advance(float amount) {
		super.advance(amount);

		if (target == null) {
			return;
		}

		duration -= amount;

		if (duration <= 0f) {
			++stage;
			duration += STAGE_DURATION[stage];

			if (stage == 1) {
				Global.getSoundPlayer().playSound(SOUND_ID, SOUND_PITCH, SOUND_VOLUME, missile.getLocation(), missile.getVelocity());
			}
		}

		if (stage == 1) {
			accelerate();
			strafeToward(target);
		}

		if (stage < 2) {
			turnToward(target);
		} else if (!missile.isFizzling()) {
			float angleDif = MathUtils.getShortestRotation(missile.getFacing(), VectorUtils.getAngle(missile.getLocation(), target.getLocation()));

			float dAngle = Math.signum(angleDif) * missile.getSource().getMutableStats().getMissileMaxTurnRateBonus().computeEffective(FAKE_TURN_MODIFIER) * amount;

			VectorUtils.rotate(missile.getVelocity(), dAngle, missile.getVelocity());
			missile.setFacing(MathUtils.clampAngle(missile.getFacing() + dAngle));
		}
	}
}