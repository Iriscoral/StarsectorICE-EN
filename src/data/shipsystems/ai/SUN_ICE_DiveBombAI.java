package data.shipsystems.ai;

import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;

public class SUN_ICE_DiveBombAI implements ShipSystemAIScript {
	private ShipSystemAPI system;
	private ShipAPI ship;

	@Override
	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
		this.ship = ship;
		this.system = system;
	}

	@Override
	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
		if (!system.isActive() && ship.isAlive() && ship.getAllWeapons().get(0).isFiring()) {
			ship.useSystem();
		}
	}
}