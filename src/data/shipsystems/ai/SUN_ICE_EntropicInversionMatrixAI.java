package data.shipsystems.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import data.scripts.tools.SUN_ICE_IceUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class SUN_ICE_EntropicInversionMatrixAI implements ShipSystemAIScript {
	private static final float REFRESH_FREQUENCY = 0.25f;
	private static final float USE_SYSTEM_THRESHOLD = 0.03f;

	private float timeOfNextRefresh = 0f;
	private ShipAPI ship;

	@Override
	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
		this.ship = ship;
	}

	@Override
	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
		if (timeOfNextRefresh < Global.getCombatEngine().getTotalElapsedTime(false)) {
			timeOfNextRefresh = Global.getCombatEngine().getTotalElapsedTime(false) + REFRESH_FREQUENCY;
		} else {
			return;
		}

		//SunUtils.print("" + IceUtils.estimateIncomingDamage(ship, 1));

		if (AIUtils.canUseSystemThisFrame(ship) && (ship.getPhaseCloak() == null || !ship.getPhaseCloak().isActive()) && (SUN_ICE_IceUtils.estimateIncomingDamage(ship, 1f) / (ship.getMaxHitpoints() + ship.getHitpoints())) > USE_SYSTEM_THRESHOLD) {

			ship.useSystem();
			//ship.setShipAI(new DontPhaseTempAI(ship));
		}
	}
}