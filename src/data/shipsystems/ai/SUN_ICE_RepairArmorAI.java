package data.shipsystems.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import data.scripts.tools.SUN_ICE_IceUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Random;

public class SUN_ICE_RepairArmorAI implements ShipSystemAIScript {
	private static final float ACTIVATION_THRESHOLD = 0.1f;
	private static final float DEACTIVATION_THRESHOLD = 0.05f;
	private static final float REFRESH_FREQUENCY = 1f;
	private float timeOfNextRefresh = 0f;
	private ShipSystemAPI system;
	private ShipAPI ship;

	private float getEnginePerformance() {
		List<ShipEngineAPI> engines = ship.getEngineController().getShipEngines();

		float acc = 0f;
		int count = 0;

		for (ShipEngineAPI engine : engines) {

			if (engine.isSystemActivated()) {
				continue;
			}

			acc += engine.isDisabled() ? 0f : 1f;

			++count;
		}

		return acc / count;
	}

	private float getArmorState() {
		ArmorGridAPI armorGrid = ship.getArmorGrid();
		Random rng = new Random();
		float armorState = 0f;
		int gridWidth = armorGrid.getGrid().length;
		int gridHeight = armorGrid.getGrid()[0].length;
		int candidates = 1 + (gridWidth * gridHeight) / 10;

		for (int i = 0; i < candidates; ++i) {
			int x = rng.nextInt(gridWidth);
			int y = rng.nextInt(gridHeight);

			armorState += armorGrid.getArmorFraction(x, y);
		}

		armorState /= candidates;

		return armorState;
	}

	@Override
	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
		this.ship = ship;
		this.system = system;
	}

	@Override
	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
		//        if(!system.isActive() && AIUtils.canUseSystemThisFrame(ship)) {
		//            ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
		//        }

		//if(!AIUtils.canUseSystemThisFrame(ship)) return;

		if (timeOfNextRefresh < Global.getCombatEngine().getTotalElapsedTime(false)) {
			timeOfNextRefresh += REFRESH_FREQUENCY;
		} else {
			return;
		}

		// low  [0] - Tested armor was at least half depleted.
		// high [1] - No armor damage found
		float armorState = Math.max(0f, (getArmorState() - 0.5f) * 2f);

		// low  [0] - No flux
		// high [1] - Full flux
		float fluxLevel = ship.getFluxTracker().getFluxLevel();

		// low  [0] - flameout
		// high [1] - no offline engines
		float enginePerformance = getEnginePerformance();

		// low  [0] - No danger nearby
		// high [1] - Really need to move
		float danger = SUN_ICE_IceUtils.estimateIncomingDamage(ship) / 500f;

		// low  [0] - don't need to activate
		// high [1] - need to activate
		float wantActive = (1f - armorState);
		wantActive *= (1f - danger * enginePerformance);
		wantActive *= (1f - (float) Math.pow(fluxLevel, 2));

		if (!system.isActive() && (wantActive > ACTIVATION_THRESHOLD)) {
			ship.useSystem();
		} else if (system.isActive() && (wantActive < DEACTIVATION_THRESHOLD)) {
			ship.useSystem();
		}
	}
}