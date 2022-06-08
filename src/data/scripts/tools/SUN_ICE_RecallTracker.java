package data.scripts.tools;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.fleet.FleetGoal;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SUN_ICE_RecallTracker {

	private static final float CHARGE_TIME = 2f;
	private static final float MAX_RANGE = 1500f;
	private static final int JITTER_COUNT = 3;
	private static final Color JITTER_COLOR = new Color(209, 255, 248, 255);

	public static boolean isRecalling(ShipAPI ship) {
		return RECALL_COUNT.containsKey(ship) && RECALL_COUNT.get(ship) > 0;
	}

	public static void clearStaticData() {
		PREVIOUS_DESTINATION.clear();
		ALREADY_TELEPORTING.clear();
		RECALL_COUNT.clear();
	}

	private static final Map<ShipAPI, Vector2f> PREVIOUS_DESTINATION = new HashMap<>();
	private static final Map<ShipAPI, Integer> RECALL_COUNT = new HashMap<>();
	private static final Set<ShipAPI> ALREADY_TELEPORTING = new HashSet<>();

	private int priority = 0;
	private final ShipAPI ally;
	private final ShipAPI teleporter;
	private Vector2f idealRecallLoc;
	private Vector2f recallLoc;
	private float progress = -1f;

	public SUN_ICE_RecallTracker(ShipAPI ally, ShipAPI teleporter) {
		this.ally = ally;
		this.teleporter = teleporter;

		if (ALREADY_TELEPORTING.contains(ally)) {
			return;
		}

		CombatFleetManagerAPI.AssignmentInfo asgnmt = Global.getCombatEngine().getFleetManager(ally.getOwner()).getTaskManager(ally.isAlly()).getAssignmentFor(ally);

		Vector2f destination;
		if (ally.isRetreating() && !Global.getCombatEngine().isMission() && !Global.getCombatEngine().isSimulation()) {
			destination = getRetreatLocation();
		} else if (asgnmt == null || asgnmt.getTarget() == null) {
			return;
		} else {
			destination = asgnmt.getTarget().getLocation();
		}

		boolean destinationChanged = !PREVIOUS_DESTINATION.containsKey(ally) || MathUtils.getDistance(destination, PREVIOUS_DESTINATION.get(ally)) > 100f;
		float minimumDistanceGain = destinationChanged ? 500f : 2500f;

		//        if(destinationChanged) {
		//            IceUtils.print(ally, "Recalculating");
		//        }
		PREVIOUS_DESTINATION.put(ally, destination);

		idealRecallLoc = VectorUtils.getDirectionalVector(teleporter.getLocation(), destination);
		idealRecallLoc.scale(Math.min(MAX_RANGE, MathUtils.getDistance(teleporter, destination)));
		Vector2f.add(idealRecallLoc, teleporter.getLocation(), idealRecallLoc);

		float distFromCurrent = MathUtils.getDistance(ally, destination);
		float distFromRecalled = MathUtils.getDistance(idealRecallLoc, destination);

		priority = (int) Math.ceil((distFromCurrent - distFromRecalled - minimumDistanceGain) / 5000f * SUN_ICE_IceUtils.getFPCost(ally));
		priority = Math.max(0, priority);

		//IceUtils.print(ally, ((destinationChanged) ? "Changed - " : "") + priority);
	}

	public void start() {
		ALREADY_TELEPORTING.add(ally);
		RECALL_COUNT.put(teleporter, RECALL_COUNT.containsKey(teleporter) ? RECALL_COUNT.get(teleporter) + 1 : 1);
	}

	public void end() {
		ALREADY_TELEPORTING.remove(ally);
		RECALL_COUNT.put(teleporter, RECALL_COUNT.get(teleporter) - 1);
		ally.getMutableStats().getMaxSpeed().unmodify("sun_ice_recall");
	}

	public void advance(float amount) {
		if (progress > 0f && recallLoc == null) {
			Vector2f midPoint = new Vector2f((idealRecallLoc.x + teleporter.getLocation().x) / 2f, (idealRecallLoc.y + teleporter.getLocation().y) / 2f);

			for (float range = 100; range < MAX_RANGE + 101f; range += MAX_RANGE / (priority + 4f)) {
				Vector2f candidate = MathUtils.getRandomPointInCircle(midPoint, range);

				if (!isShipObstructingArea(candidate, ally.getCollisionRadius() + 75f) || ally.isFighter()) {
					recallLoc = candidate;
					break;
				}
			}

			if (recallLoc == null) {
				recallLoc = ally.getLocation();
			} else {
				playTeleportSound();
				ally.getLocation().set(recallLoc);
				ally.getVelocity().scale(0f);
				ally.getMutableStats().getMaxSpeed().modifyMult("sun_ice_recall", 0f);
				ally.setAngularVelocity(0f);
				ally.setFacing(VectorUtils.getAngle(teleporter.getLocation(), recallLoc));

				if (ally.getPhaseCloak() != null && ally.getPhaseCloak().isActive()) {
					ally.getPhaseCloak().deactivate();
				}

				if (SUN_ICE_JauntSession.hasSession(ally)) {
					SUN_ICE_JauntSession.getSession(ally).endNow();
				}

				for (CombatEntityAPI roid : CombatUtils.getAsteroidsWithinRange(recallLoc, ally.getCollisionRadius())) {
					Vector2f vel = new Vector2f(1f, 0f);

					if (roid.getLocation() != ally.getLocation()) {
						vel = VectorUtils.getDirectionalVector(recallLoc, roid.getLocation());
					}

					vel.scale(ally.getCollisionRadius() / CHARGE_TIME);
					roid.getVelocity().set(vel);
				}
			}
		}

		progress += amount / CHARGE_TIME;

		float progressFactor = Math.min(progress * progress, 1f);
		ally.setAlphaMult(progressFactor);

		float jitterRange = (1f - Math.abs(progress)) * ally.getCollisionRadius() + 100f;
		float jitterAlpha = (1f - Math.abs(progress)) * Math.abs(progress);
		ally.setJitter(this, JITTER_COLOR, jitterAlpha, JITTER_COUNT, jitterRange);
		ally.setPhased(!isComplete());

		ally.blockCommandForOneFrame(ShipCommand.FIRE);
		ally.blockCommandForOneFrame(ShipCommand.VENT_FLUX);
		ally.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
	}

	public boolean isComplete() {
		return progress >= 1f;
	}

	public int getPriority() {
		return priority;
	}

	public ShipAPI getAlly() {
		return ally;
	}

	public static boolean isBeingTeleport(ShipAPI ship) {
		return ALREADY_TELEPORTING.contains(ship);
	}

	private Vector2f getRetreatLocation() {
		Vector2f retVal = new Vector2f((ally.getLocation().x + teleporter.getLocation().x) / 2f, Global.getCombatEngine().getMapHeight() * 2f);

		// The only time the escape direction is down is when the player
		// fleet is retreating after it attacks
		// owner 0 means player
		int owner = ally.getOwner();
		FleetGoal goal = Global.getCombatEngine().getContext().getPlayerGoal();
		if (owner == 0 && (goal == FleetGoal.ATTACK || goal == null)) {
			retVal.y *= -1f;
		}

		return retVal;
	}

	private void playTeleportSound() {
		Global.getSoundPlayer().playSound("system_phase_skimmer", 1f, 1f, ally.getLocation(), ally.getVelocity());
	}

	private boolean isShipObstructingArea(Vector2f at, float range) {
		for (ShipAPI s : CombatUtils.getShipsWithinRange(at, range)) {
			if (!s.isFighter() && !s.isDrone()) {
				return true;
			}
		}

		return false;
	}
}