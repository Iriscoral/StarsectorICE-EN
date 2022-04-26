package data.ai.ship;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;

public class SUN_ICE_DontPhaseTempAI extends SUN_ICE_BaseShipAI {

	@Override
	protected void evaluateCircumstances() {
		if (!ship.getSystem().isActive()) {
			ship.resetDefaultAI();
			//SunUtils.print("Feel free to phase");
		}
	}

	public SUN_ICE_DontPhaseTempAI(ShipAPI ship) {
		super(ship);
	}

	@Override
	public void advance(float amount) {
		if (ship == null) {
			return;
		}

		CombatEngineAPI engine = Global.getCombatEngine();
		if (circumstanceEvaluationTimer.intervalElapsed(engine)) {
			evaluateCircumstances();
		}
	}
}