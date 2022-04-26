package data.ai.ship;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI.AssignmentInfo;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;

public class SUN_ICE_PhaseCruiseTempAI extends SUN_ICE_BaseShipAI {

	public static float getRiskRange(ShipAPI ship) {
		return 1200f + 400f * ship.getFluxTracker().getTimeToVent();
	}

	private final CombatFleetManagerAPI fleet;
	private AssignmentInfo task;

	@Override
	public void evaluateCircumstances() {
		ShipSystemAPI cloak = ship.getPhaseCloak();
		float riskRange = getRiskRange(ship);

		if (cloak == null || ship.getFluxTracker().getFluxLevel() > 0.75f || ship.getPhaseCloak().isCoolingDown() || !AIUtils.getNearbyEnemies(ship, riskRange).isEmpty() || (task != null && task.getTarget() != null && MathUtils.getDistance(ship, task.getTarget().getLocation()) < 700f)) {

			if (AIUtils.getNearbyEnemies(ship, riskRange / 2f).isEmpty()) {
				ship.giveCommand(ShipCommand.VENT_FLUX, null, 0);
			}

			ship.resetDefaultAI();
		}
	}

	public SUN_ICE_PhaseCruiseTempAI(ShipAPI ship) {
		super(ship);
		this.fleet = Global.getCombatEngine().getFleetManager(ship.getOwner());
		circumstanceEvaluationTimer.setInterval(0.4f, 0.6f);
	}

	@Override
	public void advance(float amount) {
		super.advance(amount);
		if (ship == null) {
			return;
		}

		ship.giveCommand(ShipCommand.ACCELERATE, null, 0);
		task = fleet.getTaskManager(ship.isAlly()).getAssignmentFor(ship);
		if (task != null && task.getTarget() != null) {
			this.turnToward(task.getTarget().getLocation());
			this.strafeToward(task.getTarget().getLocation());
		}
	}
}