package data.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import data.scripts.tools.SUN_ICE_IntervalTracker;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.WeaponUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class SUN_ICE_FissionDrillAI implements ShipSystemAIScript {

	private final SUN_ICE_IntervalTracker timer = new SUN_ICE_IntervalTracker(0.3f);
	private CombatEngineAPI engine;
	private ShipSystemAPI system;
	private ShipAPI ship;
	private ShipAPI victim;
	private float timeOfTargetAquisition;

	private float getScore(ShipAPI self, ShipAPI victim) {
		if (!victim.isAlive()) {
			return 0f;
		}

		return Math.max(0f, (victim.getCollisionRadius() - 0f) / MathUtils.getDistance(self, victim));
	}

	@Override
	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
		this.ship = ship;
		this.system = system;
		this.engine = engine;
	}

	@Override
	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
		float time = engine.getTotalElapsedTime(false);

		if (system.isActive() || timer.intervalElapsed(engine)) {
			WeaponAPI drill = ship.getAllWeapons().get(2);

			// Can we get a better target?
			List<ShipAPI> candidates = WeaponUtils.getEnemiesInArc(drill);

			if (candidates.isEmpty()) {
				timeOfTargetAquisition = time;
			}

			float score = (victim == null || !candidates.contains(victim)) ? 0f : getScore(ship, victim);

			for (Object candidate : candidates) {
				ShipAPI newVictim = (ShipAPI) candidate;
				float newScore = getScore(ship, newVictim);

				if (newScore > 0f && newScore > score) {
					victim = newVictim;
					score = newScore;
					timeOfTargetAquisition = time;
				}
			}

			// Nothing to kill...
			if (victim == null) {
				return;
			}

			boolean wantActive = (!drill.isDisabled() && score > 0f && time - timeOfTargetAquisition > 1f) || (system.isActive() && MathUtils.getDistance(ship, victim) < (ship.getCollisionRadius() + victim.getCollisionRadius()) * 2f + 500f);

			// Prevent ship from strafing before activation.
			if (!system.isActive() && time != timeOfTargetAquisition) {
				ship.getVelocity().scale(0.9f);
			}

			if (system.isActive() && !wantActive) {
				ship.useSystem(); // Turn off
				victim = null;
			} else if (!system.isActive() && wantActive && ship.getFluxTracker().getFluxLevel() < 0.5f) {
				ship.useSystem(); // Turn on
			} else if (system.isActive()) {
				Vector2f to = victim.getLocation();

				float angleDif = MathUtils.getShortestRotation(ship.getFacing(), VectorUtils.getAngle(ship.getLocation(), to));
				ShipCommand direction = (angleDif > 0f) ? ShipCommand.TURN_LEFT : ShipCommand.TURN_RIGHT;
				ship.giveCommand(direction, to, 0);
			}
		}
	}
}
