package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.scripts.tools.SUN_ICE_IceUtils.I18nSection;
import data.scripts.tools.SUN_ICE_IntervalTracker;
import org.lazywizard.lazylib.combat.AIUtils;

import java.awt.Point;
import java.util.Random;

public class SUN_ICE_RepairAoE extends BaseShipSystemScript {

	private static final float ARMOR_HEAL_PER_SECOND = 300f;
	private static final float HULL_HEAL_PER_SECOND = 300f;
	private static final float RANGE_FACTOR = 800f;
	private static final Random rand = new Random();

	private final SUN_ICE_IntervalTracker tracker = new SUN_ICE_IntervalTracker(0.05f);

	//Still WIP
	public static final I18nSection strings = I18nSection.getInstance("ShipSystem", "SUN_ICE_");

	public static float getRange(ShipAPI ship) {
		if (ship == null) {
			return RANGE_FACTOR;
		}
		return ship.getMutableStats().getSystemRangeBonus().computeEffective(RANGE_FACTOR);
	}

	private void heal(ShipAPI ship) {
		ArmorGridAPI armorGrid = ship.getArmorGrid();
		float max = armorGrid.getMaxArmorInCell();
		int gridWidth = armorGrid.getGrid().length;
		int gridHeight = armorGrid.getGrid()[0].length;
		Point cellToFix = new Point(rand.nextInt(gridWidth), rand.nextInt(gridHeight));

		ship.setHitpoints(Math.min(ship.getMaxHitpoints(), ship.getHitpoints() + HULL_HEAL_PER_SECOND * tracker.getAverageInterval()));

		for (int x = cellToFix.x - 1; x <= cellToFix.x + 1; ++x) {
			if (x < 0 || x >= gridWidth) {
				continue;
			}

			for (int y = cellToFix.y - 1; y <= cellToFix.y + 1; ++y) {
				if (y < 0 || y >= gridHeight) {
					continue;
				}

				float mult = (3 - Math.abs(x - cellToFix.x) - Math.abs(y - cellToFix.y)) / 3f;

				armorGrid.setArmorValue(x, y, Math.min(max, armorGrid.getArmorValue(x, y) + ARMOR_HEAL_PER_SECOND * tracker.getAverageInterval() * mult));
			}
		}
	}

	@Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (!tracker.intervalElapsed(engine)) {
			return;
		}

		ShipAPI ship = (ShipAPI) stats.getEntity();
		if (ship == null) {
			return;
		}

		heal(ship);

		for (ShipAPI ally : AIUtils.getNearbyAllies(ship, getRange(ship))) {
			heal(ally);
		}
	}

	@Override
	public void unapply(MutableShipStatsAPI stats, String id) {
	}

	@Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) return new StatusData("healing nearby allies", false);
		return null;
	}
}