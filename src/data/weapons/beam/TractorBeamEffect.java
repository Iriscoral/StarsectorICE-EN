package data.weapons.beam;

import com.fs.starfarer.api.combat.*;
import data.ai.ship.SUN_ICE_MeleeTempAI;
import data.ai.ship.SUN_ICE_MeleeTempAI.MeleeAIFlags;
import data.scripts.tools.SUN_ICE_IntervalTracker;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class TractorBeamEffect implements BeamEffectPlugin {
	private static final float FORCE_MULTIPLIER = 1800f;
	private final SUN_ICE_IntervalTracker tracker = new SUN_ICE_IntervalTracker(0.03f);

	@Override
	public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
		CombatEntityAPI target = beam.getDamageTarget();
		ShipAPI ship = beam.getSource();

		if (target == null || !tracker.intervalElapsed(engine)) {
			return;
		}

		Vector2f from = beam.getFrom();
		float force = FORCE_MULTIPLIER * amount * beam.getBrightness();
		Vector2f direction = VectorUtils.getDirectionalVector(from, beam.getRayEndPrevFrame());

		if (ship.getShipAI() != null && ship.getFluxTracker().getFluxLevel() < 0.8f
				&& !(ship.getAIFlags() instanceof MeleeAIFlags)
				&& target instanceof ShipAPI && target.getOwner() != ship.getOwner()
				&& ((ShipAPI) target).isAlive()) {
			ship.setShipAI(new SUN_ICE_MeleeTempAI(ship, beam.getWeapon()));
			ship.setShipTarget((ShipAPI) target);
		}

		if (ship.isCruiser()) {
			force *= 1.2f;
		}

		CombatUtils.applyForce(ship, direction, force);
		direction.scale(-1f);
		CombatUtils.applyForce(target, direction, force);

		if (ship.getAIFlags() instanceof MeleeAIFlags) {
			MeleeAIFlags flag = (MeleeAIFlags) ship.getAIFlags();
			flag.friendlyFire = target.getOwner() == ship.getOwner();
		}
	}
}