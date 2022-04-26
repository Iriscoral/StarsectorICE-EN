package data.weapons.beam;

import com.fs.starfarer.api.combat.*;
import data.scripts.tools.SUN_ICE_IceUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.Point;
import org.lwjgl.util.vector.Vector2f;

import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;

public class NosBeamEffect implements BeamEffectPlugin {

	private float maxRadius = 2f;
	private float innerRadius = 1f;
	private float repairRate = 1f;

	private Map<Point, Float> cellMap;

	private void setHealStats(String id) {
		if (id.contentEquals("sun_ice_chupacabra")) {
			maxRadius = 80f;
			innerRadius = 25f;
			repairRate = 80f;
		} else if (id.contentEquals("sun_ice_nos")) {
			maxRadius = 140f;
			innerRadius = 50f;
			repairRate = 100f;
		}
	}

	private void buildCellMap(BeamAPI beam) {
		cellMap = new WeakHashMap<>();
		ArmorGridAPI grid = beam.getSource().getArmorGrid();
		int[] center = grid.getCellAtLocation(beam.getWeapon().getLocation());
		if (center == null || center.length <= 0) {
			return;
		}

		Vector2f relLoc = new Vector2f(center[0] * grid.getCellSize(), center[1] * grid.getCellSize());

		for (int x = 0; x < grid.getGrid().length; ++x) {
			for (int y = 0; y < grid.getGrid()[0].length; ++y) {
				Vector2f loc = new Vector2f(x * grid.getCellSize(), y * grid.getCellSize());
				float dist = MathUtils.getDistance(relLoc, loc);

				if (dist > maxRadius) {
					continue;
				}

				float effect = (dist <= innerRadius) ? 1f : 1f - (dist - innerRadius) / (maxRadius - innerRadius);
				cellMap.put(new Point(x, y), effect);
			}
		}
	}

	@Override
	public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
		CombatEntityAPI target = beam.getDamageTarget();

		if (!beam.didDamageThisFrame()) return;
		if (beam.getSource() == null) return;
		if (!(target instanceof ShipAPI)) return;

		ShipAPI enemy = (ShipAPI)target;
		if (!enemy.isAlive()) return;
		if (enemy.getShield() != null && enemy.getShield().isWithinArc(beam.getTo())) return;

		if (cellMap == null) {
			setHealStats(beam.getWeapon().getId());
			buildCellMap(beam);
		}

		ArmorGridAPI grid = beam.getSource().getArmorGrid();

		float totalRepaired = 0f;
		for (Entry<Point, Float> pair : cellMap.entrySet()) {
			float currentVal = grid.getArmorValue(pair.getKey().getX(), pair.getKey().getY());
			float newVal = currentVal + (float) Math.sqrt(pair.getValue()) * amount * repairRate;
			newVal = Math.min(newVal, grid.getMaxArmorInCell() * pair.getValue());
			newVal = Math.max(newVal, currentVal);

			grid.setArmorValue(pair.getKey().getX(), pair.getKey().getY(), newVal);
			totalRepaired += newVal - currentVal;
		}

		//((Ship) beam.getSource()).syncWithArmorGridState();
		//((Ship) beam.getSource()).syncWeaponDecalsWithArmorDamage();
		SUN_ICE_IceUtils.showHealText(beam.getSource(), beam.getFrom(), totalRepaired);
	}
}