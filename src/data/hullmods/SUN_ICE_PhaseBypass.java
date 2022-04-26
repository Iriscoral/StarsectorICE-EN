package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import data.scripts.tools.SUN_ICE_IceUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SUN_ICE_PhaseBypass extends BaseHullMod {
	private static final Random rand = new Random();
	private static final float ARMOR_REPAIR_MULTIPLIER = 3f;
	private static final Color SPARK_COLOR = new Color(255, 223, 128);
	private static final float SPARK_DURATION = 0.3f;
	private static final float SPARK_BRIGHTNESS = 0.95f;
	private static final float SPARK_MAX_RADIUS = 6f;
	private static final float SPARK_CHANCE = 0.2f;
	private static final float SPARK_SPEED_MULTIPLIER = 300.0f;

	private static final List<String> BLACK_LIST = new ArrayList<>();

	static {
		BLACK_LIST.add(HullMods.MAKESHIFT_GENERATOR);
		BLACK_LIST.add(HullMods.ADAPTIVE_COILS);
		BLACK_LIST.add(HullMods.PHASE_ANCHOR);
		BLACK_LIST.add("sun_ice_turbulence_valve_mod");
	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		for (String hullmod : BLACK_LIST) {
			if (ship.getVariant().getHullMods().contains(hullmod)) {
				ship.getVariant().removeMod(hullmod);
			}
		}

		ship.getMutableStats().getPhaseCloakUpkeepCostBonus().modifyMult(id, 0f);
		ship.getMutableStats().getPhaseCloakActivationCostBonus().modifyMult(id, 0f);
	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		if (ship == null || !ship.isAlive()) return;
		if (Global.getCombatEngine().isPaused() || ship.getPhaseCloak() == null) return;

		ship.setDefenseDisabled(true);
		ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);

		if (ship.getFluxTracker().isOverloadedOrVenting() || ship.getCurrentCR() == 0f) {
			return;
		}

		if (!repairArmor(ship, amount)) {
			repairArmor(ship, amount);
		}
	}

	private static boolean repairArmor(ShipAPI ship, float amount) {
		ArmorGridAPI armorGrid = ship.getArmorGrid();
		int width = armorGrid.getGrid().length;
		int height = armorGrid.getGrid()[0].length;
		int x = rand.nextInt(width);
		int y = rand.nextInt(height);
		float limit = armorGrid.getMaxArmorInCell();
		if (armorGrid.getArmorValue(x, y) >= limit) {
			return false;
		}

		float newArmor = armorGrid.getArmorValue(x, y) + limit * ARMOR_REPAIR_MULTIPLIER * amount * (0.5f + ship.getCurrentCR() * 0.5f);
		armorGrid.setArmorValue(x, y, Math.min(limit, newArmor));
		float cellSize = armorGrid.getCellSize();

		if (Math.random() < SPARK_CHANCE) {
			Vector2f cellLoc = armorGrid.getLocation(x, y);
			cellLoc.x += cellSize * 0.5f - cellSize * (float) Math.random();
			cellLoc.y += cellSize * 0.5f - cellSize * (float) Math.random();

			Vector2f vel = new Vector2f(ship.getVelocity());
			vel.x += (Math.random() - 0.5f) * SPARK_SPEED_MULTIPLIER;
			vel.y += (Math.random() - 0.5f) * SPARK_SPEED_MULTIPLIER;

			Global.getCombatEngine().addHitParticle(cellLoc, vel, SPARK_MAX_RADIUS * (float) Math.random() + SPARK_MAX_RADIUS, SPARK_BRIGHTNESS, SPARK_DURATION * (float) Math.random() + SPARK_DURATION, SPARK_COLOR);
		}

		return true;
	}

	@Override
	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + (int) ARMOR_REPAIR_MULTIPLIER;
		return null;
	}

	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		boolean basic = ship != null && ship.getHullSpec().getHullId().startsWith("sun_ice_") && SUN_ICE_IceUtils.isPhysicallyPhaseShip(ship);
		if (!basic) return false;

		for (String hullmod : BLACK_LIST) {
			if (ship.getVariant().getHullMods().contains(hullmod)) {
				return false;
			}
		}

		return true;
	}
}