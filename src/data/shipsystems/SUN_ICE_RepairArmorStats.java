package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import data.scripts.tools.SUN_ICE_IceUtils;
import data.scripts.tools.SUN_ICE_IceUtils.I18nSection;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

public class SUN_ICE_RepairArmorStats extends BaseShipSystemScript {

	private static final float MIN_POTENCY = 0.5f;
	private static final float MAX_POTENCY = 3.0f;
	private static final float ARMOR_REPAIRED_PER_FLUX = 0.3f;
	private static final float SECONDS_TO_MAX_POTENCY = 3.0f;
	private static final float SECONDS_TO_MIN_POTENCY = 0.5f;

	private static final Color SPARK_COLOR = new Color(255, 223, 128);
	private static final float SPARK_DURATION = 0.3f;
	private static final float SPARK_BRIGHTNESS = 0.8f;
	private static final float SPARK_MAX_RADIUS = 10f;
	private static final float SPARK_CHANCE = 0.5f;
	private static final float SPARK_SPEED_MULTIPLIER = 100.0f;

	private static final Map<ShipAPI, Float> potencies = new WeakHashMap<>();

	public static final I18nSection strings = I18nSection.getInstance("ShipSystem", "SUN_ICE_");

	public static float getPotency(ShipAPI ship) {
		return potencies.containsKey(ship) ? potencies.get(ship) : 0.0f;
	}

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = (ShipAPI) stats.getEntity();
		CombatEngineAPI engine = Global.getCombatEngine();
		float amount = Global.getCombatEngine().getElapsedInLastFrame();

		ArmorGridAPI armorGrid = ship.getArmorGrid();
		Random rng = new Random();
		float max = armorGrid.getMaxArmorInCell();
		float cellSize = armorGrid.getCellSize();
		int gridWidth = armorGrid.getGrid().length;
		int gridHeight = armorGrid.getGrid()[0].length;
		int cellCount = gridWidth * gridHeight;
		int candidates = 1 + cellCount / 10;

		// Apply mobility debuffs
		if (state == ShipSystemStatsScript.State.OUT) {
			stats.getMaxSpeed().unmodify(id);
			stats.getMaxTurnRate().unmodify(id);
		} else {
			stats.getMaxSpeed().modifyMult(id, 1f - 0.2f * effectLevel);
			stats.getAcceleration().modifyMult(id, 1f - 0.5f * effectLevel);
			stats.getDeceleration().modifyMult(id, 1f - 0.5f * effectLevel);
			stats.getMaxTurnRate().modifyMult(id, 1f - 0.2f * effectLevel);
			stats.getTurnAcceleration().modifyMult(id, 1f - 0.5f * effectLevel);
		}

		// Increment Potency
		float potency = getPotency(ship);
		potency += (ship.getFluxTracker().getCurrFlux() == ship.getFluxTracker().getHardFlux()) ? amount / SECONDS_TO_MAX_POTENCY : -amount / SECONDS_TO_MIN_POTENCY;
		potency = Math.min(1f, Math.max(0f, potency * effectLevel));
		potencies.put(ship, potency);
		potency = MIN_POTENCY + (MAX_POTENCY - MIN_POTENCY) * potency;

		// Determine which armor cell to try to repair
		int leaderX = 0, leaderY = 0;
		Vector2f leaderLoc = null;
		float bestRecord = Float.MAX_VALUE;

		for (int i = 0; i < candidates; ++i) {
			int x = rng.nextInt(gridWidth);
			int y = rng.nextInt(gridHeight);

			if (armorGrid.getArmorFraction(x, y) >= 1) {
				continue;
			}

			Vector2f cellLoc = armorGrid.getLocation(x, y);
			float current = armorGrid.getArmorValue(x, y);
			float dist = MathUtils.getDistance(cellLoc, ship.getMouseTarget());

			if ((dist < bestRecord) && (current < max)) {
				leaderLoc = cellLoc;
				bestRecord = dist;
				leaderX = x;
				leaderY = y;
			}
		}

		// Repair the chosen armor cell
		if (bestRecord != Float.MAX_VALUE) {
			float current = armorGrid.getArmorValue(leaderX, leaderY);
			float fluxCost = potency * amount * ship.getMutableStats().getFluxDissipation().getBaseValue();
			float increase = ARMOR_REPAIRED_PER_FLUX * fluxCost;
			fluxCost *= 1f - Math.max(0f, ((current + increase) - max) / increase);

			armorGrid.setArmorValue(leaderX, leaderY, Math.min(max, current + increase));
			SUN_ICE_IceUtils.showHealText(ship, leaderLoc, armorGrid.getArmorValue(leaderX, leaderY) - current);

			if (Math.random() < SPARK_CHANCE) {
				leaderLoc.x += cellSize * 0.5f - cellSize * (float) Math.random();
				leaderLoc.y += cellSize * 0.5f - cellSize * (float) Math.random();

				Vector2f vel = new Vector2f(ship.getVelocity());
				vel.x += (Math.random() - 0.5f) * SPARK_SPEED_MULTIPLIER;
				vel.y += (Math.random() - 0.5f) * SPARK_SPEED_MULTIPLIER;

				engine.addHitParticle(leaderLoc, vel, (SPARK_MAX_RADIUS * (float) Math.random() + SPARK_MAX_RADIUS) * (float) Math.sqrt(fluxCost / 30), SPARK_BRIGHTNESS, SPARK_DURATION * (float) Math.random() + SPARK_DURATION, SPARK_COLOR);
			}
			//((Ship) ship).syncWithArmorGridState();
			// Generate flux
			if (ship.getFluxTracker().getCurrFlux() + fluxCost > ship.getFluxTracker().getMaxFlux()) {
				ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
			} else {
				ship.getFluxTracker().increaseFlux(fluxCost, true);
			}
		}
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getMaxSpeed().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);

		if (stats.getEntity() instanceof ShipAPI)
		potencies.remove(stats.getEntity());
	}

	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) return new StatusData(strings.get("RepairArmorStats1"), false);
		if (index == 1) return new StatusData(strings.get("RepairArmorStats2"), false);
		return null;
	}
}