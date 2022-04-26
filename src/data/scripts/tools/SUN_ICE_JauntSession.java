package data.scripts.tools;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import data.ai.ship.SUN_ICE_JauntTurnTempAI;
import data.scripts.plugins.MagicRenderPlugin;
import data.shipsystems.ai.SUN_ICE_JauntAI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.*;

public class SUN_ICE_JauntSession {

	private static final float REPELL_FORCE = 150f;
	private static final float SUPPORT_RANGE = 2000f;
	private static final float SECONDS_IN_WARP_PER_SU = 0.001f;
	private static final float MIN_WARP_TIME = 0.5f;
	private static final int DESTINATION_CANDIDATE_COUNT = 17;
	private static final Color DOPPELGANGER_COLOR = new Color(0, 255, 220, 120);

	private static final Map<ShipAPI, SUN_ICE_JauntSession> jauntSessions = new WeakHashMap<>();
	private static final List<SUN_ICE_JauntSession> toClear = new LinkedList<>();

	public static void clearStaticData() {
		jauntSessions.clear();
	}

	public static void advanceAll(float amount) {
		for (SUN_ICE_JauntSession jaunt : jauntSessions.values()) {
			jaunt.advance(amount);
		}
		for (SUN_ICE_JauntSession jaunt : toClear) {
			jaunt.endNow();
		}
		toClear.clear();
	}

	public static boolean hasSession(ShipAPI ship) {
		return jauntSessions.containsKey(ship);
	}

	public static SUN_ICE_JauntSession getSession(ShipAPI ship) {
		return getSession(ship, 0f);
	}

	public static SUN_ICE_JauntSession getSession(ShipAPI ship, float range) {
		return jauntSessions.containsKey(ship) ? jauntSessions.get(ship) : new SUN_ICE_JauntSession(ship, range);
	}

	private final ShipAPI ship;
	private final CombatEngineAPI engine;
	private Vector2f destination = null;
	private Vector2f origin = null;
	private Vector2f lastLoc = null;
	private SpriteAPI doppelganger = null;
	private final float originalFacing;
	private float lastFacingBeforeReturn;
	private float progress;
	private float highestProgressReached;
	private final float maxRange;
	private float warpTime;
	private boolean returning = false;

	private void setAlpha(float alpha) {
		if (alpha < 0.2f) alpha = 0.2f;
		if (alpha > 0.5f) alpha = 0.5f;

		ship.setAlphaMult(alpha);
	}

	private void resetAlpha() {
		ship.setAlphaMult(1f);
	}

	private boolean pointIsClear(Vector2f at) {
		for (ShipAPI s : Global.getCombatEngine().getShips()) {
			if (s != ship && s.getCollisionClass() == CollisionClass.SHIP && MathUtils.getDistance(s, at) <= 0f) {
				return false;
			}
		}

		return CombatUtils.getAsteroidsWithinRange(at, ship.getCollisionRadius() * 0.8f).isEmpty();
	}

	private Vector2f getAIDestinationChoice() {
		Vector2f retVal = null, shipLoc = new Vector2f(ship.getLocation());
		boolean aggressing = AIUtils.getEnemiesOnMap(ship).size() > 0 && ship.getFluxTracker().getFluxLevel() < 0.4f + Math.random() * 0.3f;
		float range = maxRange + ship.getCollisionRadius();
		float weaponRange = SUN_ICE_IceUtils.estimateOptimalRange(ship) * 0.8f;
		float bestScore = -999999f; // Float.MIN_VALUE won't work for some reason. Why?
		double theta = Math.random() * Math.PI * 2f;
		double thetaIncrement = (Math.PI * 2f) / DESTINATION_CANDIDATE_COUNT;
		ShipAPI enemy = null, bestScoringEnemy = null;
		boolean canTurn = SUN_ICE_IceUtils.getEngineFractionDisabled(ship) > 0f;

		for (int i = 0; i <= DESTINATION_CANDIDATE_COUNT; ++i) {
			float fudge = (float) Math.random() * 0.5f + 0.5f;
			Vector2f candidate = new Vector2f((float) Math.cos(theta) * range * fudge + shipLoc.x, (float) Math.sin(theta) * range * fudge + shipLoc.y);
			theta += thetaIncrement;

			ship.getLocation().set(candidate);
			float score = 0f;

			if (aggressing) {
				enemy = AIUtils.getNearestEnemy(ship); // Don't move this out of the loop
				if (enemy == null) {
					continue;
				}
				float rangeDist = weaponRange - MathUtils.getDistance(ship, enemy);
				if (rangeDist > 0f) {
					boolean shieldBlocked = enemy.getShield() != null && enemy.getShield().isWithinArc(candidate);
					score += SUN_ICE_IceUtils.getFPCost(enemy) * (1f + enemy.getFluxTracker().getFluxLevel()) * (shieldBlocked ? 0.25f : 1f) * (0.5f + 0.5f * (weaponRange - rangeDist) / weaponRange);

					if (!canTurn) {
						float angleTo = VectorUtils.getAngle(ship.getLocation(), enemy.getLocation());
						float degreesFromFacingTarget = Math.abs(MathUtils.getShortestRotation(angleTo, ship.getFacing()));
						score *= 1f - degreesFromFacingTarget / 180f;
					}
				}
			} else {
				score -= SUN_ICE_IceUtils.getFPWorthOfHostility(ship, SUPPORT_RANGE);
			}

			score -= SUN_ICE_IceUtils.estimateIncomingDamage(ship) * 0.05f;
			if (!pointIsClear(candidate)) {
				score -= ship.getCollisionRadius() * 0.1f;
			}

			if (score > bestScore) {
				bestScore = score;
				retVal = candidate;
				bestScoringEnemy = enemy;
			}
		}

		ship.getLocation().set(shipLoc);

		if (retVal == null) {
			retVal = MathUtils.getRandomPointInCircle(ship.getLocation(), range);
		}

		if (bestScoringEnemy != null) {
			ship.setShipAI(new SUN_ICE_JauntTurnTempAI(ship, bestScoringEnemy, this));
		}

		return retVal;
	}

	private void determineDestination() {
		destination = (ship.getShipAI() == null) ? new Vector2f(ship.getMouseTarget()) // For the player
				: getAIDestinationChoice(); // For AI
		origin = new Vector2f(ship.getLocation());

		// Bring destination closer if it exceeds max range
		float range = maxRange + ship.getCollisionRadius();
		float distance = MathUtils.getDistance(ship, destination);
		Vector2f dir = VectorUtils.getDirectionalVector(ship.getLocation(), destination);
		if (distance > range) {
			distance = range - 100f - ship.getCollisionRadius();
			destination = (Vector2f) new Vector2f(dir).scale(range);
			Vector2f.add(destination, origin, destination);
		}

		// Bring destination progressively closer until it is clear of obsticles
		for (float length = distance + 100f + ship.getCollisionRadius(); !pointIsClear(destination); length -= 100f) {
			if (length <= 0f) {
				destination = ship.getLocation();
				break;
			}

			destination = (Vector2f) new Vector2f(dir).scale(length);
			Vector2f.add(destination, origin, destination);
		}
	}

	private void createDoppelganger() {
		// Create the doppelganger (placeholder double) at the origin
		String id = null;
		if (ship.getHullSpec().getBaseHullId().contentEquals("sun_ice_abraxas")) {
			id = "sun_ice_doppelganger_abraxas";
		} else if (ship.getHullSpec().getBaseHullId().contentEquals("sun_ice_nightseer")) {
			id = "sun_ice_doppelganger_nightseer";
		}

		if (id == null) return;
		doppelganger = Global.getSettings().getSprite("fx", id);
		doppelganger.setColor(DOPPELGANGER_COLOR);
		doppelganger.setAdditiveBlend();
	}

	private void manageDoppelganger(float amount) {

		doppelganger.setAngle(originalFacing - 90f + (float) (Math.random() - 0.5d));
		Vector2f at = MathUtils.getRandomPointInCircle(origin, 10f);
		MagicRenderPlugin.addSingleframe(doppelganger, at, CombatEngineLayers.BELOW_INDICATORS_LAYER);

		// Push entities away from the doppelganger to keep that space available
		List<CombatEntityAPI> entities = new ArrayList<>();
		entities.addAll(engine.getShips());
		entities.addAll(engine.getAsteroids());
		entities.remove(ship);
		for (CombatEntityAPI entity : entities) {
			if (entity instanceof ShipAPI && ((ShipAPI) entity).isFighter()) {
				continue;
			}

			float distance = MathUtils.getDistance(entity, origin);
			float force = Math.min(1f, 2f - distance / ship.getCollisionRadius());

			if (force > 0f) {
				force *= amount * REPELL_FORCE;
				Vector2f direction = (Vector2f) VectorUtils.getDirectionalVector(origin, entity.getLocation()).scale(force);
				Vector2f.add(entity.getLocation(), direction, entity.getLocation());
			}
		}
	}

	private SUN_ICE_JauntSession(ShipAPI ship, float range) {
		this.ship = ship;
		maxRange = range;
		engine = Global.getCombatEngine();
		originalFacing = ship.getFacing();
		lastFacingBeforeReturn = ship.getFacing();
		highestProgressReached = 0f;

		if (maxRange == 0f) {
			origin = destination = new Vector2f(ship.getLocation());
			warpTime = 0f;
		} else {
			determineDestination();
			warpTime = MathUtils.getDistance(origin, destination) * SECONDS_IN_WARP_PER_SU + MIN_WARP_TIME;
		}

		createDoppelganger();
		SUN_ICE_JauntAI.setOrigin(ship, origin);
		jauntSessions.put(ship, this);
	}

	public Vector2f getOrigin() {
		return origin;
	}

	public Vector2f getDestination() {
		return destination;
	}

	public boolean isReturning() {
		return returning;
	}

	public boolean isWarping() {
		return progress != 1f;
	}

	private void advance(float amount) {
		if (ship.getFluxTracker().isOverloadedOrVenting()) {
			goHome();
		}

		if (warpTime == 0f) {
			progress = returning ? 0f : 1f;
		} else {
			progress += (amount / warpTime) * (returning ? -1f : 1f);
			progress = Math.max(0f, Math.min(1f, progress));
		}

		if (highestProgressReached < progress) {
			highestProgressReached = progress;
		}
		setAlpha((float) Math.pow(Math.abs((0.5f - progress) * 2f), 3f));

		boolean isUsingPhaseCloak = ship.getPhaseCloak() != null && ship.getPhaseCloak().isActive();

		if (progress == 0f || !engine.isEntityInPlay(ship)) {
			ship.getLocation().set(origin);
			toClear.add(this);
		} else if (!isWarping()) {
			destination = new Vector2f(ship.getLocation());
			lastFacingBeforeReturn = ship.getFacing();
			ship.setPhased(isUsingPhaseCloak);
		} else {
			float sign = Math.signum(progress - 0.5f);
			float scale = (float) Math.pow(Math.abs(Math.cos((1f - progress) * Math.PI)), 0.7f);
			ship.getLocation().set(SUN_ICE_IceUtils.getMidpoint(origin, destination, (sign * scale + 1f) / 2f));
			ship.setPhased(true);

			ship.blockCommandForOneFrame(ShipCommand.FIRE);
			ship.blockCommandForOneFrame(ShipCommand.VENT_FLUX);
			ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);

			if (returning) {
				ship.setAngularVelocity(0f);
				ship.setFacing(lastFacingBeforeReturn + MathUtils.getShortestRotation(lastFacingBeforeReturn, originalFacing) * (highestProgressReached - progress) / highestProgressReached);
			} else {
				lastFacingBeforeReturn = ship.getFacing();
				if (lastLoc != null && amount > 0f) {
					float dist = MathUtils.getDistance(lastLoc, ship.getLocation());
					float speed = Math.min(dist / amount, ship.getMutableStats().getMaxSpeed().getModifiedValue() * 2f);
					ship.getVelocity().set(VectorUtils.getDirectionalVector(lastLoc, ship.getLocation()));
					ship.getVelocity().scale(speed);
				}
			}

			lastLoc = new Vector2f(ship.getLocation());
		}

		if (doppelganger == null) {
			createDoppelganger();
		}

		if (doppelganger != null) {
			manageDoppelganger(amount);
		}
	}

	public void stopGoingHome() {
		returning = false;
		destination = new Vector2f(ship.getLocation());
		progress = 1f;
		warpTime = 0f;
		if (ship.getShipAI() != null) {
			ship.resetDefaultAI();
		}
	}

	public void goHome() {
		if (!jauntSessions.containsValue(this)) {
			return;
		}

		if (!returning) {
			returning = true;
			warpTime = MathUtils.getDistance(origin, destination) * SECONDS_IN_WARP_PER_SU + MIN_WARP_TIME;
			if (ship.getShipAI() != null) {
				ship.setShipAI(new SUN_ICE_JauntTurnTempAI(ship, getNearestEnemy(origin, ship), this));
			}
		}

		ship.setShipSystemDisabled(true);
	}

	public void endNow() {
		boolean isUsingPhaseCloak = ship.getPhaseCloak() != null && ship.getPhaseCloak().isActive();
		ship.setPhased(isUsingPhaseCloak);
		ship.setShipSystemDisabled(false);
		jauntSessions.remove(ship);

		if (ship.getShipAI() != null) {
			ship.resetDefaultAI();
		}

		if (doppelganger != null) {
			doppelganger = null;
		}

		resetAlpha();
	}

	private static ShipAPI getNearestEnemy(Vector2f location, ShipAPI source) {
		ShipAPI closest = null;
		float distance, closestDistance = Float.MAX_VALUE;

		for (ShipAPI tmp : AIUtils.getEnemiesOnMap(source)) {
			distance = MathUtils.getDistance(tmp, location);
			if (distance < closestDistance) {
				closest = tmp;
				closestDistance = distance;
			}
		}

		return closest;
	}
}