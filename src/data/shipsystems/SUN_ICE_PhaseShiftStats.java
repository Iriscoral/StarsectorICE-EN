package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.tools.SUN_ICE_IntervalTracker;
import data.scripts.util.MagicLensFlare;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class SUN_ICE_PhaseShiftStats extends SUN_ICE_PhaseWarpStats {

	private static final float EMP_ENERGY_DAMAGE = 50f;
	private static final float EMP_EMP_DAMAGE = 50f;
	private static final float EMP_THICKNESS = 1f;
	private static final Color EMP_COLOR = new Color(0, 255, 220);
	private static final Color EMP_POP_COLOR = new Color(124, 255, 233, 64);

	private final SUN_ICE_IntervalTracker zapTimer = new SUN_ICE_IntervalTracker(0.05f, 0.15f);

	@Override
	public void advanceImpl(MutableShipStatsAPI stats, String id, ShipAPI ship, State state, float effectLevel) {
		unsetPhaseBehavior(ship);

		CombatEngineAPI engine = Global.getCombatEngine();
		if (zapTimer.intervalElapsed(engine)) {
			float range = ship.getCollisionRadius() * 1.5f;
			Vector2f point = MathUtils.getRandomPointInCircle(ship.getLocation(), ship.getCollisionRadius() * 0.7f);

			WeightedRandomPicker<CombatEntityAPI> targets = new WeightedRandomPicker<>();
			for (MissileAPI enemy : AIUtils.getNearbyEnemyMissiles(ship, range)) {
				if (enemy.isFizzling() || enemy.isFading() || enemy.didDamage()) {
					continue;
				}
				if (enemy.isFlare()) {
					targets.add(enemy, 3f);
				} else {
					targets.add(enemy, 4f);
				}
			}

			for (ShipAPI enemy : AIUtils.getNearbyEnemies(ship, range)) {
				if (enemy.isPhased()) {
					continue;
				}
				if (enemy.isFighter() || enemy.isDrone()) {
					targets.add(enemy, 2f);
				} else {
					targets.add(enemy, 1f);
				}
			}

			if (!targets.isEmpty()) {
				CombatEntityAPI target = targets.pick();

				engine.spawnEmpArc(ship, point, ship, target, DamageType.ENERGY, EMP_ENERGY_DAMAGE, EMP_EMP_DAMAGE, range * 2f, null, EMP_THICKNESS, EMP_POP_COLOR, EMP_COLOR);
			} else {
				MagicLensFlare.createSharpFlare(engine, ship, point, 1f, 50f, 0f, EMP_POP_COLOR, EMP_COLOR);
			}
		}
	}

	@Override
	public void applySpeedBonus(MutableShipStatsAPI stats, String id, ShipAPI ship, State state, float effectLevel) {
		super.applySpeedBonus(stats, id, ship, state, effectLevel);

		float bonusMult = getSpeedMult(ship, effectLevel) * effectLevel;
		stats.getTurnAcceleration().modifyFlat(id, 150f * bonusMult);
		stats.getMaxTurnRate().modifyFlat(id, 75f * bonusMult);
	}

	@Override
	public void unapplyImpl(MutableShipStatsAPI stats, String id, ShipAPI ship) {

		stats.getMaxTurnRate().unmodify(id);
		float sign = Math.signum(ship.getAngularVelocity());
		float from = Math.abs(ship.getAngularVelocity());
		float to = ship.getMutableStats().getMaxTurnRate().getModifiedValue();
		ship.setAngularVelocity((from * 1.9f + to * 0.1f) / 2f * sign);
	}
}