package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import data.scripts.tools.SUN_ICE_IntervalTracker;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;

public class SUN_ICE_AdvPhaseWarpStats extends SUN_ICE_PhaseWarpStats {

	private static final float ANGLE_FORCE_MULTIPLIER = 3.5f;
	private static final float VELOCITY_FORCE_MULTIPLIER = 350f;
	private static final float MAX_RANGE_EXTRA = 700f;

	private static final Color ABSORB_COLOR = new Color(125, 255, 225);

	private Vector2f wormhole;
	private final List<DamagingProjectileAPI> ordnance = new LinkedList<>();
	private final SUN_ICE_IntervalTracker wormholeParticleTimer = new SUN_ICE_IntervalTracker(0.02f, 0.05f);

	private void absorbProjectile(ShipAPI ship, DamagingProjectileAPI proj) {
		if (!Global.getCombatEngine().isEntityInPlay(proj)) {
			return;
		}

		float mult = proj.getDamageType() == DamageType.FRAGMENTATION ? 0.25f : 1f;
		float flux = (proj.getDamageAmount() * mult + proj.getEmpAmount() * 0.25f) * 0.5f;
		ship.getFluxTracker().decreaseFlux(flux);
		ship.setJitterUnder(ship, ABSORB_COLOR, 1f, 10, 2f, 20f);
		if (ship == Global.getCombatEngine().getPlayerShip()) {
			Global.getCombatEngine().addFloatingDamageText(ship.getLocation(), flux, ABSORB_COLOR, ship, ship);
		}

		Global.getCombatEngine().removeEntity(proj);
	}

	private void suckInProjectile(ShipAPI ship, DamagingProjectileAPI proj) {
		float fromToAngle = VectorUtils.getAngle(wormhole, proj.getLocation());
		float angleDif = MathUtils.getShortestRotation(fromToAngle, MathUtils.clampAngle(proj.getFacing() + 180));
		float amount = Global.getCombatEngine().getElapsedInLastFrame();
		float distance = MathUtils.getDistance(wormhole, proj.getLocation());
		float force = (ship.getCollisionRadius() + MAX_RANGE_EXTRA) / distance;
		float dAngle = -angleDif * amount * force * ANGLE_FORCE_MULTIPLIER;
		fromToAngle = (float) Math.toRadians(fromToAngle);
		Vector2f speedUp = new Vector2f((float) Math.cos(fromToAngle) * amount, (float) Math.sin(fromToAngle) * amount);
		speedUp.scale(-force * VELOCITY_FORCE_MULTIPLIER);

		Vector2f.add(proj.getVelocity(), speedUp, proj.getVelocity());
		VectorUtils.rotate(proj.getVelocity(), dAngle, proj.getVelocity());
		proj.setFacing(MathUtils.clampAngle(proj.getFacing() + dAngle));
	}

	private void updateNearbyOrdnance(ShipAPI ship) {
		ordnance.clear();
		ordnance.addAll(CombatUtils.getProjectilesWithinRange(wormhole, ship.getCollisionRadius() + MAX_RANGE_EXTRA));
		ordnance.addAll(CombatUtils.getMissilesWithinRange(wormhole, ship.getCollisionRadius() + MAX_RANGE_EXTRA));
	}

	@Override
	public void advanceImpl(MutableShipStatsAPI stats, String id, ShipAPI ship, State state, float effectLevel) {
		super.advanceImpl(stats, id, ship, state, effectLevel);

		if (wormhole == null) {
			wormhole = new Vector2f(ship.getLocation());
			updateNearbyOrdnance(ship);
		}

		for (DamagingProjectileAPI proj : ordnance) {
			if (proj == null || proj.getProjectileSpecId() == null) continue;
			if (proj.getProjectileSpecId().endsWith("_doppelganger")) continue;
			if (proj.getProjectileSpecId().contentEquals("sun_ice_bladeofalpha_hack") && proj.getSource() == ship) {
				continue;
			}

			if (MathUtils.getDistance(proj, wormhole) <= 30f) {
				absorbProjectile(ship, proj);
			} else {
				suckInProjectile(ship, proj);
			}
		}

		CombatEngineAPI engine = Global.getCombatEngine();
		if (wormholeParticleTimer.intervalElapsed(engine)) {
			updateNearbyOrdnance(ship);
			Vector2f at = MathUtils.getRandomPointInCircle(wormhole, 60f);
			Vector2f vel = MathUtils.getRandomPointInCircle(new Vector2f(), 50f);
			float radius = (float) (100 + 150 * Math.random());
			float coreRadius = (float) (10 + 80 * Math.random());
			engine.addSmokeParticle(at, vel, radius, 1f, 0.8f, new Color(12, 24, 24, 124));
			engine.addSmoothParticle(wormhole, new Vector2f(), coreRadius, 1f, 0.9f, new Color(60, 120, 120, 255));
		}
	}

	@Override
	public void unapplyImpl(MutableShipStatsAPI stats, String id, ShipAPI ship) {
		wormhole = null;
		ordnance.clear();
	}
}