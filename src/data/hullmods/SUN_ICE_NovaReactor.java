package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.Random;

public class SUN_ICE_NovaReactor extends BaseHullMod {

	private static final Color SPARK_COLOR = new Color(255, 223, 128);
	private static final float SPARK_DURATION = 0.3f;
	private static final float SPARK_BRIGHTNESS = 0.95f;
	private static final float SPARK_MAX_RADIUS = 6f;
	private static final float SPARK_CHANCE = 0.5f;
	private static final float SPARK_SPEED_MULTIPLIER = 300f;

	private static final Random rand = new Random();
	private static final int ARMOR_REPAIR_CHECK = 5;
	private static final float ARMOR_REPAIR_MULTIPLIER = 1000f;

	private void repairArmor(ShipAPI ship, float amount) {

		ArmorGridAPI armorGrid = ship.getArmorGrid();
		int width = armorGrid.getGrid().length;
		int height = armorGrid.getGrid()[0].length;
		int x = rand.nextInt(width);
		int y = rand.nextInt(height);
		float newArmor = armorGrid.getArmorValue(x, y);
		float cellSize = armorGrid.getCellSize();

		float reduction = 1f;
		reduction *= 1f - Math.floor(Math.min(2, Math.abs(x - (width - 1) / 2f))) / 2f;
		reduction *= 1f - Math.floor(Math.min(2, Math.abs(y - (height + 3) / 2f) - 1)) / 2f;

		float limit = armorGrid.getMaxArmorInCell() * (1f - reduction);

		if (newArmor >= limit) {
			return;
		}

		newArmor += ARMOR_REPAIR_MULTIPLIER * amount * (0.5f + ship.getCurrentCR() * 0.5f);
		armorGrid.setArmorValue(x, y, Math.min(limit, newArmor));

		if (Math.random() < SPARK_CHANCE) {
			Vector2f cellLoc = armorGrid.getLocation(x, y);
			cellLoc.x += cellSize * 0.5f - cellSize * (float) Math.random();
			cellLoc.y += cellSize * 0.5f - cellSize * (float) Math.random();

			Vector2f vel = new Vector2f(ship.getVelocity());
			vel.x += (Math.random() - 0.5f) * SPARK_SPEED_MULTIPLIER;
			vel.y += (Math.random() - 0.5f) * SPARK_SPEED_MULTIPLIER;

			Global.getCombatEngine().addHitParticle(cellLoc, vel, SPARK_MAX_RADIUS * (float) Math.random() + SPARK_MAX_RADIUS, SPARK_BRIGHTNESS, SPARK_DURATION * (float) Math.random() + SPARK_DURATION, SPARK_COLOR);

		}
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		if (ship == null || !ship.isAlive()) return;
		if (Global.getCombatEngine().isPaused()) return;

		if (!ship.getFluxTracker().isOverloadedOrVenting()) {
			for (int i = 0; i < ARMOR_REPAIR_CHECK; i++) {
				repairArmor(ship, amount);
			}
		}

		// some hack
		float turnRate = ship.getAngularVelocity();
		float turnRateLimit = ship.getMutableStats().getMaxTurnRate().getModifiedValue();

		if (ship.getSystem().isCoolingDown() && Math.abs(turnRate) > turnRateLimit) {
			ship.setAngularVelocity(turnRate * (1f - amount * 2f));
		}
	}

	@Override
	public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
		stats.getCombatEngineRepairTimeMult().modifyMult(id, 0.5f);
		stats.getCombatWeaponRepairTimeMult().modifyMult(id, 0.5f);
	}

	@Override
	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
		if (index == 0) return "" + ARMOR_REPAIR_CHECK * (int) ARMOR_REPAIR_MULTIPLIER;
		if (index == 1) return "50%";
		return null;
	}
}