package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.plugins.SUN_ICE_EveryFramePlugin;
import data.scripts.tools.SUN_ICE_IceUtils;
import data.scripts.tools.SUN_ICE_IntervalTracker;
import data.scripts.tools.SUN_ICE_RecallTracker;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

public class SUN_ICE_RecallTeleporter extends BaseHullMod {
	private static final String id = "SUN_ICE_RecallTeleporter";

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {

		CombatEngineAPI engine = Global.getCombatEngine();
		if (!engine.getCustomData().containsKey(id)) {
			engine.getCustomData().put(id, new HashMap<>());
		}

		Map<ShipAPI, SUN_ICE_RecallStarter> shipsMap = (Map) engine.getCustomData().get(id);
		if (engine.isPaused() || !engine.isEntityInPlay(ship) || !ship.isAlive()) {
			if (!ship.isAlive()) {
				shipsMap.remove(ship);
			}
			return;
		}

		if (!shipsMap.containsKey(ship)) {
			shipsMap.put(ship, new SUN_ICE_RecallStarter(ship));
		} else {
			SUN_ICE_RecallStarter data = shipsMap.get(ship);
			data.advance(engine);
		}
	}

	public static class SUN_ICE_RecallStarter {
		private final TreeMap<Integer, List<SUN_ICE_RecallTracker>> recallQueue = new TreeMap<>();

		private final ShipAPI ship;
		private final SUN_ICE_IntervalTracker requestCheckTimer = new SUN_ICE_IntervalTracker(0.3f);
		private final SUN_ICE_IntervalTracker doRecallTimer = new SUN_ICE_IntervalTracker(0.1f, 0.5f);

		public SUN_ICE_RecallStarter(ShipAPI ship) {
			this.ship = ship;
		}

		private void collectRecallRequests() {
			recallQueue.clear();

			for (ShipAPI ally : AIUtils.getAlliesOnMap(ship)) {
				if (ally.isFighter() || ally.isDrone()) continue;
				if (ally.isStation() || ally.isStationModule()) continue;
				if (ally.getFleetMember() != null && !SUN_ICE_IceUtils.isConsideredPhaseShip(ally.getFleetMember())) continue;

				if (ally.isPhased()) continue;
				if (ally.getAI() == null) continue;
				if (ally.getTravelDrive() != null && ally.getTravelDrive().isActive()) continue;

				if (SUN_ICE_RecallTracker.isBeingTeleport(ship)) continue;

				CombatFleetManagerAPI.AssignmentInfo asgnmt = Global.getCombatEngine().getFleetManager(ally.getOwner()).getTaskManager(ally.isAlly()).getAssignmentFor(ally);
				if (asgnmt == null) continue;

				if (!ally.isRetreating() && asgnmt.getTarget() != null) {
					Vector2f targetLocation = asgnmt.getTarget().getLocation();
					if (targetLocation != null && MathUtils.getDistance(ship, targetLocation) < 400f) continue;
				}

				SUN_ICE_RecallTracker t = new SUN_ICE_RecallTracker(ally, ship);
				if (t.getPriority() > 0) {
					if (!recallQueue.containsKey(t.getPriority())) {
						recallQueue.put(t.getPriority(), new LinkedList<SUN_ICE_RecallTracker>());
					}

					recallQueue.get(t.getPriority()).add(t);
				}
			}
		}

		private void selectAnAllyToRecall() {
			if (recallQueue.isEmpty()) {
				return;
			}

			List<SUN_ICE_RecallTracker> candidates = recallQueue.lastEntry().getValue();
			int index = (new Random()).nextInt(candidates.size());
			SUN_ICE_RecallTracker winner = candidates.get(index);

			SUN_ICE_EveryFramePlugin.beginRecall(winner);
			Global.getSoundPlayer().playSound("system_phase_cloak_activate", 2, 1, winner.getAlly().getLocation(), winner.getAlly().getVelocity());

			candidates.remove(winner);
			if (candidates.isEmpty()) {
				recallQueue.remove(recallQueue.lastKey());
			}
		}

		public void advance(CombatEngineAPI engine) {
			if (!ship.isAlive() || ship.isPhased()) {
				return;
			}

			if (requestCheckTimer.intervalElapsed(engine)) {
				collectRecallRequests();
			}
			if (doRecallTimer.intervalElapsed(engine)) {
				selectAnAllyToRecall();
			}
		}
	}
}