package data.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import data.shipsystems.SUN_ICE_LimiterControlStats;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

public class SUN_ICE_LimiterControlAI implements ShipSystemAIScript {
	private ShipAPI ship;

	@Override
	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
		this.ship = ship;
	}

	@Override
	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
		if (target == null || !target.isAlive() || !ship.isAlive()) return;
		if (ship.isPhased() || target.isPhased()) return;
		if (target.isStation() || target.isStationModule()) return;

		if (AIUtils.canUseSystemThisFrame(ship)) {
			boolean downshifted = SUN_ICE_LimiterControlStats.isDownshifted(ship);
			if (downshifted) {
				if (ship.getFluxLevel() < 0.7f && MathUtils.getDistance(ship.getLocation(), target.getLocation()) < 1100f && Math.abs(MathUtils.getShortestRotation(ship.getFacing(), VectorUtils.getAngle(ship.getLocation(), target.getLocation()))) < 10f) {
					ship.setShipTarget(target);
					ship.useSystem();
				}
			} else {
				if (isUsable(ship, target) && ship.getFluxLevel() < 0.6f) {
					ship.setShipTarget(target);
					ship.useSystem();
				}
			}
		}
	}

	private boolean isUsable(ShipAPI ship, ShipAPI target) {
		if (MathUtils.getDistance(ship, target) > 1400f) {
			return false;
		}
		if (!target.isDrone() && !target.isFighter() && target.getOwner() != ship.getOwner() && target.isAlive() && !target.isStation() && !target.isStationModule()) {
			Vector2f point = MathUtils.getPointOnCircumference(target.getLocation(), target.getCollisionRadius() + ship.getCollisionRadius() + 100f, target.getFacing() + 180f);
			return !isShipObstructingArea(point, ship.getCollisionRadius());
		}
		return false;
	}

	private boolean isShipObstructingArea(Vector2f at, float range) {
		for (ShipAPI s : CombatUtils.getShipsWithinRange(at, range)) {
			if (!s.isFighter()) {
				return true;
			}
		}
		return false;
	}
}