package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class SUN_ICE_FissionDrillStats extends BaseShipSystemScript {
	//boolean within = false;

	private static String getString(String key) {
		return Global.getSettings().getString("ShipSystem", "SUN_ICE_" + key);
	}

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (state == ShipSystemStatsScript.State.OUT) {
			stats.getMaxSpeed().unmodify(id);
			stats.getMaxTurnRate().unmodify(id);
		} else {
			stats.getMaxSpeed().modifyFlat(id, 300f * effectLevel);
			stats.getAcceleration().modifyFlat(id, 400f * effectLevel);
			stats.getMaxTurnRate().modifyMult(id, 1f - 0.5f * effectLevel);
			stats.getTurnAcceleration().modifyMult(id, 1f - 0.5f * effectLevel);

			ShipAPI ship = (ShipAPI) stats.getEntity();
			WeaponAPI drill = ship.getAllWeapons().get(2);
			ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.DO_NOT_VENT);

			if (drill.isDisabled()) {
				ship.getFluxTracker().forceOverload(1f);
				return;
			} else {
				ship.setCollisionClass(CollisionClass.FIGHTER);
			}

			Vector2f at = drill.getLocation();
			at.x += (float) Math.random() * 40f - 20f;
			at.y += (float) Math.random() * 40f - 20f;
			for (CombatEntityAPI ast : Global.getCombatEngine().getAsteroids()) {
				if (MathUtils.getDistance(ast.getLocation(), at) < ast.getCollisionRadius() * 0.5f) {
					Global.getCombatEngine().applyDamage(ast, at, ast.getHitpoints(), drill.getSpec().getDamageType(), 0, true, true, ship);
				}
			}

			for (ShipAPI target : AIUtils.getNearbyEnemies(ship, 100)) {
				if (target.isPhased()) {
					continue;
				}
				if (CollisionUtils.isPointWithinBounds(at, target)) {
					float damage = drill.getSpec().getDerivedStats().getDps() * Global.getCombatEngine().getElapsedInLastFrame();

					CombatUtils.applyForce(target, (Vector2f) ship.getVelocity().scale(0.98f), 40f);
					Global.getCombatEngine().applyDamage(target, at, damage, drill.getSpec().getDamageType(), 0f, true, true, ship);

					stats.getWeaponDamageTakenMult().modifyMult(id, 0f);
					stats.getEngineDamageTakenMult().modifyMult(id, 0f);
					Global.getCombatEngine().applyDamage(ship, at, damage * 0.1f, drill.getSpec().getDamageType(), 0f, true, true, ship);

					Global.getCombatEngine().spawnExplosion(at, // Location
							target.getVelocity(), // Velocity
							Color.white, // Color
							50f + (float) Math.random() * 100f, // Size
							1f + (float) Math.random() * 2f); // Duration
					Global.getSoundPlayer().playSound("collision_ships", 1f, 1f, ship.getLocation(), target.getVelocity());
				} else {
					stats.getWeaponDamageTakenMult().unmodify();
					stats.getEngineDamageTakenMult().unmodify();
				}
			}
		}
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		ShipAPI ship = (ShipAPI) stats.getEntity();
		if (ship != null) {
			ship.setCollisionClass(CollisionClass.SHIP);
		}

		stats.getMaxSpeed().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
		stats.getWeaponDamageTakenMult().unmodify(id);
		stats.getEngineDamageTakenMult().unmodify(id);
	}

	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) return new StatusData(getString("FissionDrillStats1"), false);
		if (index == 1) return new StatusData(getString("FissionDrillStats2"), false);
		return null;
	}
}