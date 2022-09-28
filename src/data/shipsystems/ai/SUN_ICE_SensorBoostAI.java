package data.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;

public class SUN_ICE_SensorBoostAI implements ShipSystemAIScript {
	private ShipSystemAPI system;
	private ShipAPI ship;

	@Override
	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
		this.ship = ship;
		this.system = system;
	}

	@Override
	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
		if (ship.isAlive() && !system.isCoolingDown()) {
			if (!system.isActive() && ship.getFluxLevel() < 0.4f) {
				ship.useSystem();
			} else if (system.isActive() && ship.getFluxLevel() > 0.8f) {
				ship.useSystem();
			}
		}
	}
}