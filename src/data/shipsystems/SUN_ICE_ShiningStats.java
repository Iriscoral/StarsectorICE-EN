package data.shipsystems;

import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class SUN_ICE_ShiningStats extends BaseShipSystemScript {
	
	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = (ShipAPI) stats.getEntity();
		if (ship == null || !ship.isAlive()) {
			return;
		}

		ship.setCollisionClass(CollisionClass.NONE);

		for (ShipAPI drone : ship.getDeployedDrones()) {
			if (drone.isAlive() && drone.getFullTimeDeployed() == 0f) {
				drone.setFacing(ship.getFacing());
				drone.setRenderEngines(true);
			}
		}
	}
	
	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		return null;
	}
}